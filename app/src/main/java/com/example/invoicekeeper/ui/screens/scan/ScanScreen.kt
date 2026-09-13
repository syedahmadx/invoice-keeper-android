package com.example.invoicekeeper.ui.screens.scan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.invoicekeeper.ui.components.AiDisclaimer
import com.example.invoicekeeper.ui.components.ErrorState
import com.example.invoicekeeper.ui.components.FindingRow
import com.example.invoicekeeper.ui.components.LoadingState
import com.example.invoicekeeper.ui.components.MessageCard
import com.example.invoicekeeper.ui.components.MessageTone
import com.example.invoicekeeper.ui.components.ProviderBadge
import com.example.invoicekeeper.ui.components.ScreenTitle
import com.example.invoicekeeper.ui.components.SectionHeader
import com.example.invoicekeeper.ui.components.Spacing
import com.example.invoicekeeper.util.PICKER_MIME_FILTER
import com.example.invoicekeeper.util.formatAmount
import com.example.invoicekeeper.util.moneyMatches

@Composable
fun ScanScreen(
    onSettingsClick: () -> Unit,
    onOpenSavedInvoice: (Long) -> Unit,
    viewModel: ScanViewModel = viewModel(factory = ScanViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val pickFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> viewModel.onFilePicked(uri) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Spacer(Modifier.height(Spacing.sm))

        ScreenTitle(
            title = "Scan an invoice",
            subtitle = "Pick a photo or PDF, let the model read it, check the figures, then send.",
        )

        StepIndicator(current = state.currentStep)

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // --- 1. File ---------------------------------------------------------------------
        StepSection(number = 1, title = "Choose a file") {
            FilePickerCard(
                state = state,
                onPick = { pickFile.launch(PICKER_MIME_FILTER) },
                onClear = viewModel::clearFile,
            )
        }

        // --- 2. Extract ------------------------------------------------------------------
        StepSection(number = 2, title = "Extract the fields") {
            Button(
                onClick = viewModel::extract,
                enabled = state.canExtract,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    if (state.hasExtraction) "Extract again" else "Extract",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (state.pickedFile == null && !state.isReadingFile) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "Pick a file first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.extraction.isLoading) {
                LoadingState("Reading the document with an AI model. This can take up to a minute.")
            }

            state.extraction.failure?.let { failure ->
                Spacer(Modifier.height(Spacing.md))
                ErrorState(
                    message = failure.message,
                    retryable = failure.retryable,
                    onRetry = viewModel::extract,
                )
            }
        }

        // --- 3. Check --------------------------------------------------------------------
        val form = state.form
        if (form != null) {
            StepSection(number = 3, title = "Check and correct") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Extracted fields",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    state.extractProvider?.let { ProviderBadge(it) }
                }
                Spacer(Modifier.height(Spacing.sm))
                AiDisclaimer()
                Spacer(Modifier.height(Spacing.lg))

                InvoiceFormEditor(
                    form = form,
                    enabled = !state.locked && !state.busy,
                    viewModel = viewModel,
                )
            }

            // --- 4. Review ---------------------------------------------------------------
            StepSection(number = 4, title = "Ask for a second opinion") {
                OutlinedButton(
                    onClick = viewModel::review,
                    enabled = state.canReview,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Review for problems")
                }

                if (state.review.isLoading) {
                    LoadingState("Reviewing the extracted figures")
                }

                state.review.failure?.let { failure ->
                    Spacer(Modifier.height(Spacing.md))
                    ErrorState(
                        message = failure.message,
                        retryable = failure.retryable,
                        onRetry = viewModel::review,
                    )
                }

                if (state.findingsClearedByEdit) {
                    Spacer(Modifier.height(Spacing.md))
                    MessageCard(
                        tone = MessageTone.INFO,
                        text = "You edited a field, so the previous findings were cleared. " +
                            "They described the old numbers.",
                    )
                }

                if (state.review is RequestState.Done) {
                    Spacer(Modifier.height(Spacing.lg))
                    if (state.findings.isEmpty()) {
                        MessageCard(
                            tone = MessageTone.SUCCESS,
                            text = "No problems found. Still worth a glance against the original.",
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${state.findings.size} finding" +
                                    if (state.findings.size == 1) "" else "s",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            state.reviewProvider?.let { ProviderBadge(it) }
                        }
                        Spacer(Modifier.height(Spacing.md))
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            state.findings.forEach { FindingRow(it) }
                        }
                    }
                }
            }

            // --- 5 and 6. Send / save ----------------------------------------------------
            StepSection(number = 5, title = "Send or save") {
                if (state.locked) {
                    MessageCard(
                        tone = MessageTone.SUCCESS,
                        title = "Sent",
                        text = state.sentMessage.orEmpty(),
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Button(onClick = viewModel::startAnother) { Text("Scan another") }
                        state.savedInvoiceId?.let { id ->
                            OutlinedButton(onClick = { onOpenSavedInvoice(id) }) { Text("View saved") }
                        }
                    }
                } else {
                    Button(
                        onClick = viewModel::send,
                        enabled = state.canSend,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.sm))
                        Text("Send to webhook", style = MaterialTheme.typography.titleMedium)
                    }

                    state.sendDisabledReason?.let { reason ->
                        Spacer(Modifier.height(Spacing.sm))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (!state.hasWebhook) {
                                TextButton(onClick = onSettingsClick) { Text("Settings") }
                            }
                        }
                    }

                    Spacer(Modifier.height(Spacing.sm))

                    OutlinedButton(
                        onClick = viewModel::saveDraft,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Save as draft")
                    }

                    if (state.send.isLoading) {
                        LoadingState("Posting to your webhook")
                    }

                    state.send.failure?.let { failure ->
                        Spacer(Modifier.height(Spacing.md))
                        ErrorState(
                            message = failure.message,
                            retryable = failure.retryable,
                            onRetry = viewModel::send,
                        )
                    }

                    state.draftSavedMessage?.let { message ->
                        Spacer(Modifier.height(Spacing.md))
                        MessageCard(
                            tone = MessageTone.SUCCESS,
                            text = message,
                            actionLabel = "Dismiss",
                            onAction = viewModel::dismissMessages,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.xxl))
    }
}

@Composable
private fun StepSection(
    number: Int,
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Step $number - $title")
        Spacer(Modifier.height(Spacing.md))
        content()
    }
}

@Composable
private fun FilePickerCard(
    state: ScanUiState,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    Panel {
        when {
            state.isReadingFile -> LoadingState("Checking the file")

            state.pickedFile != null -> {
                val file = state.pickedFile
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(Spacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${file.readableType} - ${file.readableSize}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!state.locked) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Outlined.Close, contentDescription = "Remove file")
                        }
                    }
                }
                if (!state.locked) {
                    Spacer(Modifier.height(Spacing.md))
                    TextButton(onClick = onPick, contentPadding = ButtonDefaults.TextButtonContentPadding) {
                        Text("Choose a different file")
                    }
                }
            }

            else -> {
                Text(
                    text = "JPEG, PNG, WebP or PDF, up to 8 MB.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.md))
                OutlinedButton(
                    onClick = onPick,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Choose a file")
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "Opens the system file picker. Invoice Keeper never asks for access " +
                        "to your whole storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        state.fileError?.let { error ->
            Spacer(Modifier.height(Spacing.md))
            MessageCard(tone = MessageTone.ERROR, text = error)
        }
    }
}

@Composable
private fun InvoiceFormEditor(
    form: InvoiceForm,
    enabled: Boolean,
    viewModel: ScanViewModel,
) {
    Panel {
        FormField(
            label = "Supplier",
            value = form.supplier,
            onValueChange = viewModel::updateSupplier,
            enabled = enabled,
            isError = form.supplier.isBlank(),
            supportingText = if (form.supplier.isBlank()) "Required" else null,
        )
        Spacer(Modifier.height(Spacing.md))
        FormField(
            label = "Supplier tax ID",
            value = form.supplierTaxId,
            onValueChange = viewModel::updateSupplierTaxId,
            enabled = enabled,
            placeholder = "Optional",
        )
        Spacer(Modifier.height(Spacing.md))
        FormField(
            label = "Invoice number",
            value = form.invoiceNumber,
            onValueChange = viewModel::updateInvoiceNumber,
            enabled = enabled,
            isError = form.invoiceNumber.isBlank(),
            supportingText = if (form.invoiceNumber.isBlank()) "Required" else null,
        )
        Spacer(Modifier.height(Spacing.md))
        FormField(
            label = "Issue date",
            value = form.issueDate,
            onValueChange = viewModel::updateIssueDate,
            enabled = enabled,
            placeholder = "YYYY-MM-DD",
            isError = form.issueDate.isBlank(),
            supportingText = if (form.issueDate.isBlank()) "Required" else null,
        )
        Spacer(Modifier.height(Spacing.md))
        FormField(
            label = "Due date",
            value = form.dueDate,
            onValueChange = viewModel::updateDueDate,
            enabled = enabled,
            placeholder = "Optional",
        )
        Spacer(Modifier.height(Spacing.md))
        FormField(
            label = "Currency",
            value = form.currency,
            onValueChange = viewModel::updateCurrency,
            enabled = enabled,
            placeholder = "EUR",
            isError = form.currency.isBlank(),
            supportingText = if (form.currency.isBlank()) "Required" else null,
        )
    }

    Spacer(Modifier.height(Spacing.lg))

    LineItemsEditor(form = form, enabled = enabled, viewModel = viewModel)

    Spacer(Modifier.height(Spacing.lg))

    Panel {
        Text("Totals", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(Spacing.md))

        val lineTotal = form.lineItemsTotal
        val subtotalValue = form.subtotalValue
        val matches = subtotalValue != null && moneyMatches(subtotalValue, lineTotal)

        LabelledAmountRow(
            label = "Line items add up to",
            amount = formatAmount(lineTotal),
            tint = if (matches) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.error
            },
        )
        if (!matches && enabled) {
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "This does not match the subtotal below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f, fill = false),
                )
                TextButton(onClick = viewModel::useLineItemsTotalAsSubtotal) { Text("Use it") }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        MoneyField(
            label = "Subtotal",
            value = form.subtotal,
            onValueChange = viewModel::updateSubtotal,
            enabled = enabled,
            isError = form.subtotalValue == null,
            supportingText = if (form.subtotalValue == null) "Not a number" else null,
        )
        Spacer(Modifier.height(Spacing.md))
        MoneyField(
            label = "Tax",
            value = form.tax,
            onValueChange = viewModel::updateTax,
            enabled = enabled,
            isError = form.taxValue == null,
            supportingText = if (form.taxValue == null) "Not a number" else null,
        )
        Spacer(Modifier.height(Spacing.md))
        MoneyField(
            label = "Total",
            value = form.total,
            onValueChange = viewModel::updateTotal,
            enabled = enabled,
            isError = form.totalValue == null,
            supportingText = if (form.totalValue == null) "Not a number" else null,
        )

        val sub = form.subtotalValue
        val tax = form.taxValue
        val total = form.totalValue
        if (sub != null && tax != null && total != null && !moneyMatches(sub + tax, total)) {
            Spacer(Modifier.height(Spacing.md))
            MessageCard(
                tone = MessageTone.WARNING,
                text = "Subtotal plus tax is ${formatAmount(sub + tax)}, but the total says " +
                    "${formatAmount(total)}.",
            )
        }
    }
}

@Composable
private fun LineItemsEditor(
    form: InvoiceForm,
    enabled: Boolean,
    viewModel: ScanViewModel,
) {
    Panel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Line items (${form.lineItems.size})",
                style = MaterialTheme.typography.titleSmall,
            )
            if (enabled) {
                TextButton(onClick = viewModel::addLineItem) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text("Add")
                }
            }
        }

        if (form.lineItems.isEmpty()) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "No line items were found. Add them by hand if your automation needs them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        form.lineItems.forEachIndexed { index, item ->
            Spacer(Modifier.height(Spacing.lg))
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(Spacing.lg))
            }
            LineItemRow(
                index = index,
                item = item,
                enabled = enabled,
                onChange = { transform -> viewModel.updateLineItem(item.key, transform) },
                onRemove = { viewModel.removeLineItem(item.key) },
            )
        }
    }
}

/**
 * Stacked rather than tabular. A four-column table of editable fields is unusable at phone widths,
 * so the description gets a full-width row and the three numbers share the row below it.
 */
@Composable
private fun LineItemRow(
    index: Int,
    item: LineItemForm,
    enabled: Boolean,
    onChange: ((LineItemForm) -> LineItemForm) -> Unit,
    onRemove: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Item ${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (enabled) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Remove item ${index + 1}",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        FormField(
            label = "Description",
            value = item.description,
            onValueChange = { value -> onChange { it.copy(description = value) } },
            enabled = enabled,
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            MoneyField(
                label = "Qty",
                value = item.quantity,
                onValueChange = { value -> onChange { it.copy(quantity = value) } },
                enabled = enabled,
                isError = item.quantityValue == null,
                modifier = Modifier.weight(0.8f),
            )
            MoneyField(
                label = "Unit price",
                value = item.unitPrice,
                onValueChange = { value -> onChange { it.copy(unitPrice = value) } },
                enabled = enabled,
                isError = item.unitPriceValue == null,
                modifier = Modifier.weight(1.1f),
            )
            MoneyField(
                label = "Amount",
                value = item.amount,
                onValueChange = { value -> onChange { it.copy(amount = value) } },
                enabled = enabled,
                isError = item.amountValue == null,
                modifier = Modifier.weight(1.1f),
            )
        }

        val qty = item.quantityValue
        val unit = item.unitPriceValue
        val amount = item.amountValue
        if (qty != null && unit != null && amount != null && !moneyMatches(qty * unit, amount)) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Quantity times unit price is ${formatAmount(qty * unit)}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
