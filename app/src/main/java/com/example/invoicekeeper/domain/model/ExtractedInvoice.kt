package com.example.invoicekeeper.domain.model

import kotlinx.serialization.Serializable

/**
 * The invoice shape the backend extracts and the webhook receives. Kept serializable so the exact
 * same object can be posted straight through /api/review and /api/send without a second mapping.
 */
@Serializable
data class ExtractedInvoice(
    val supplier: String = "",
    val supplierTaxId: String? = null,
    val invoiceNumber: String = "",
    val issueDate: String = "",
    val dueDate: String? = null,
    val currency: String = "",
    val lineItems: List<LineItem> = emptyList(),
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double = 0.0,
) {
    /** What the line items actually add up to, which may differ from what the document claims. */
    val lineItemsTotal: Double get() = lineItems.sumOf { it.amount }
}

@Serializable
data class LineItem(
    val description: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val amount: Double = 0.0,
)

@Serializable
data class ReviewFinding(
    val severity: String = Severity.INFO,
    val field: String = "",
    val message: String = "",
) {
    object Severity {
        const val ERROR = "error"
        const val WARNING = "warning"
        const val INFO = "info"
    }

    val normalisedSeverity: String
        get() = when (severity.lowercase()) {
            Severity.ERROR -> Severity.ERROR
            Severity.WARNING -> Severity.WARNING
            else -> Severity.INFO
        }
}
