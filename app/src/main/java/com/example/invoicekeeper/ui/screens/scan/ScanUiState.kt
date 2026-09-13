package com.example.invoicekeeper.ui.screens.scan

import com.example.invoicekeeper.data.remote.NetworkResult
import com.example.invoicekeeper.domain.model.ExtractedInvoice
import com.example.invoicekeeper.domain.model.LineItem
import com.example.invoicekeeper.domain.model.ReviewFinding
import com.example.invoicekeeper.util.PickedFile
import com.example.invoicekeeper.util.formatAmount
import com.example.invoicekeeper.util.formatQuantity
import com.example.invoicekeeper.util.parseAmountOrNull

/** Idle / loading / failed / done, for one request. */
sealed interface RequestState {
    data object Idle : RequestState
    data object Loading : RequestState
    data object Done : RequestState
    data class Failed(val reason: NetworkResult.Failure) : RequestState

    val isLoading: Boolean get() = this is Loading
    val failure: NetworkResult.Failure? get() = (this as? Failed)?.reason
}

/**
 * Numbers are held as the text the user actually typed. Parsing to Double on every keystroke would
 * turn "12." into 12 and fight the keyboard; instead the text is kept verbatim and validated when
 * it matters.
 */
data class LineItemForm(
    val key: Long,
    val description: String = "",
    val quantity: String = "",
    val unitPrice: String = "",
    val amount: String = "",
) {
    val amountValue: Double? get() = parseAmountOrNull(amount)
    val quantityValue: Double? get() = parseAmountOrNull(quantity)
    val unitPriceValue: Double? get() = parseAmountOrNull(unitPrice)

    val hasNumberError: Boolean
        get() = amountValue == null || quantityValue == null || unitPriceValue == null

    fun toLineItem(): LineItem = LineItem(
        description = description.trim(),
        quantity = quantityValue ?: 0.0,
        unitPrice = unitPriceValue ?: 0.0,
        amount = amountValue ?: 0.0,
    )
}

data class InvoiceForm(
    val supplier: String = "",
    val supplierTaxId: String = "",
    val invoiceNumber: String = "",
    val issueDate: String = "",
    val dueDate: String = "",
    val currency: String = "",
    val lineItems: List<LineItemForm> = emptyList(),
    val subtotal: String = "",
    val tax: String = "",
    val total: String = "",
) {
    val subtotalValue: Double? get() = parseAmountOrNull(subtotal)
    val taxValue: Double? get() = parseAmountOrNull(tax)
    val totalValue: Double? get() = parseAmountOrNull(total)

    /** What the line items currently add up to. Recomputed on every edit. */
    val lineItemsTotal: Double
        get() = lineItems.sumOf { it.amountValue ?: 0.0 }

    val hasNumberErrors: Boolean
        get() = subtotalValue == null || taxValue == null || totalValue == null ||
            lineItems.any { it.hasNumberError }

    val missingRequiredFields: List<String>
        get() = buildList {
            if (supplier.isBlank()) add("Supplier")
            if (invoiceNumber.isBlank()) add("Invoice number")
            if (issueDate.isBlank()) add("Issue date")
            if (currency.isBlank()) add("Currency")
        }

    val isValid: Boolean get() = !hasNumberErrors && missingRequiredFields.isEmpty()

    fun toInvoice(): ExtractedInvoice = ExtractedInvoice(
        supplier = supplier.trim(),
        supplierTaxId = supplierTaxId.trim().ifBlank { null },
        invoiceNumber = invoiceNumber.trim(),
        issueDate = issueDate.trim(),
        dueDate = dueDate.trim().ifBlank { null },
        currency = currency.trim().uppercase(),
        lineItems = lineItems.map { it.toLineItem() },
        subtotal = subtotalValue ?: 0.0,
        tax = taxValue ?: 0.0,
        total = totalValue ?: 0.0,
    )

    companion object {
        fun from(invoice: ExtractedInvoice): InvoiceForm = InvoiceForm(
            supplier = invoice.supplier,
            supplierTaxId = invoice.supplierTaxId.orEmpty(),
            invoiceNumber = invoice.invoiceNumber,
            issueDate = invoice.issueDate,
            dueDate = invoice.dueDate.orEmpty(),
            currency = invoice.currency,
            lineItems = invoice.lineItems.mapIndexed { index, item ->
                LineItemForm(
                    key = index.toLong(),
                    description = item.description,
                    quantity = formatQuantity(item.quantity),
                    unitPrice = formatAmount(item.unitPrice),
                    amount = formatAmount(item.amount),
                )
            },
            subtotal = formatAmount(invoice.subtotal),
            tax = formatAmount(invoice.tax),
            total = formatAmount(invoice.total),
        )
    }
}

enum class ScanStep { PICK, EXTRACT, CHECK, SEND }

data class ScanUiState(
    val pickedFile: PickedFile? = null,
    val fileError: String? = null,
    val isReadingFile: Boolean = false,

    val extraction: RequestState = RequestState.Idle,
    val extractProvider: String? = null,
    val form: InvoiceForm? = null,

    val review: RequestState = RequestState.Idle,
    val reviewProvider: String? = null,
    val findings: List<ReviewFinding> = emptyList(),
    val findingsClearedByEdit: Boolean = false,

    val send: RequestState = RequestState.Idle,
    val hasWebhook: Boolean = false,
    val savedInvoiceId: Long? = null,
    val draftSavedMessage: String? = null,
    val sentMessage: String? = null,
    val locked: Boolean = false,
    val formErrorMessage: String? = null,
) {
    val hasExtraction: Boolean get() = form != null

    val busy: Boolean
        get() = isReadingFile || extraction.isLoading || review.isLoading || send.isLoading

    val canExtract: Boolean get() = pickedFile != null && !busy && !locked

    val canReview: Boolean get() = form != null && !form.hasNumberErrors && !busy && !locked

    val canSend: Boolean get() = !locked && form != null && sendDisabledReason == null

    /** Why the send button is disabled, phrased for the user. Null when it is enabled. */
    val sendDisabledReason: String?
        get() = when {
            locked -> null
            form == null -> "Extract an invoice first."
            !hasWebhook -> "No webhook URL is configured. Add one in Settings."
            form.hasNumberErrors -> "Some amounts are not valid numbers."
            form.missingRequiredFields.isNotEmpty() ->
                "Still missing: " + form.missingRequiredFields.joinToString(", ")
            busy -> "A request is already running."
            else -> null
        }

    val currentStep: ScanStep
        get() = when {
            locked -> ScanStep.SEND
            form != null && findings.isNotEmpty() -> ScanStep.SEND
            form != null -> ScanStep.CHECK
            pickedFile != null -> ScanStep.EXTRACT
            else -> ScanStep.PICK
        }
}
