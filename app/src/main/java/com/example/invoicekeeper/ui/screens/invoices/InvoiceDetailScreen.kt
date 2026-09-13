package com.example.invoicekeeper.ui.screens.invoices

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.invoicekeeper.domain.model.InvoiceStatus
import com.example.invoicekeeper.domain.model.SavedInvoice
import com.example.invoicekeeper.ui.components.AiDisclaimer
import com.example.invoicekeeper.ui.components.EmptyState
import com.example.invoicekeeper.ui.components.ErrorState
import com.example.invoicekeeper.ui.components.FindingRow
import com.example.invoicekeeper.ui.components.LoadingState
import com.example.invoicekeeper.ui.components.MessageCard
import com.example.invoicekeeper.ui.components.MessageTone
import com.example.invoicekeeper.ui.components.ProviderBadge
import com.example.invoicekeeper.ui.components.SectionHeader
import com.example.invoicekeeper.ui.components.Spacing
import com.example.invoicekeeper.ui.components.StatusChip
import com.example.invoicekeeper.ui.screens.scan.LabelledAmountRow
import com.example.invoicekeeper.ui.screens.scan.Panel
import com.example.invoicekeeper.ui.screens.scan.RequestState
import com.example.invoicekeeper.ui.theme.MoneySmallTextStyle
import com.example.invoicekeeper.ui.theme.MoneyTotalTextStyle
import com.example.invoicekeeper.util.formatAmount
import com.example.invoicekeeper.util.formatQuantity
import com.example.invoicekeeper.util.formatTimestampWithTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    onBack: () -> Unit,
    viewModel: InvoiceDetailViewModel = viewModel(factory = InvoiceDetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.invoice?.supplier ?: "Invoice",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.invoice != null) {
                        IconButton(onClick = viewModel::askToDelete) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Delete invoice",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            when {
                state.isLoading -> LoadingState("Loading invoice")

                state.deleted -> {
                    EmptyState(
                        icon = Icons.Outlined.Delete,
                        title = "Deleted",
                        description = "This invoice has been removed from this device.",
                        action = { Button(onClick = onBack) { Text("Back to invoices") } },
                    )
                }

                state.notFound -> {
                    EmptyState(
                        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                        title = "Invoice not found",
                        description = "It may have been deleted.",
                        action = { Button(onClick = onBack) { Text("Back to invoices") } },
                    )
                }

                else -> state.invoice?.let { invoice ->
                    InvoiceDetailContent(
                        invoice = invoice,
                        state = state,
                        onSend = viewModel::send,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xxl))
        }
    }

    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text("Delete this invoice?") },
            text = {
                Text(
                    "It will be removed from this device. Anything already posted to your " +
                        "webhook is unaffected.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun InvoiceDetailContent(
    invoice: SavedInvoice,
    state: InvoiceDetailUiState,
    onSend: () -> Unit,
) {
    Spacer(Modifier.height(Spacing.xs))

    Panel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = invoice.supplier, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = invoice.invoiceNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Spacing.md))
            StatusChip(invoice.status)
        }

        Spacer(Modifier.height(Spacing.lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = invoice.currency,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = formatAmount(invoice.total), style = MoneyTotalTextStyle)
        }

        Spacer(Modifier.height(Spacing.lg))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(Spacing.lg))

        DetailRow("Saved", formatTimestampWithTime(invoice.savedAt))
        invoice.invoice.issueDate.takeIf { it.isNotBlank() }?.let { DetailRow("Issue date", it) }
        invoice.invoice.dueDate?.takeIf { it.isNotBlank() }?.let { DetailRow("Due date", it) }
        invoice.invoice.supplierTaxId?.takeIf { it.isNotBlank() }?.let { DetailRow("Tax ID", it) }
        invoice.sourceFileName?.takeIf { it.isNotBlank() }?.let { DetailRow("Source file", it) }
    }

    // --- Provenance --------------------------------------------------------------------------
    Column {
        SectionHeader("Produced by")
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            invoice.extractProvider?.takeIf { it.isNotBlank() }?.let {
                ProviderBadge("Extract: $it")
            }
            invoice.reviewProvider?.takeIf { it.isNotBlank() }?.let {
                ProviderBadge("Review: $it")
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        AiDisclaimer()
    }

    // --- Line items --------------------------------------------------------------------------
    Column {
        SectionHeader("Line items (${invoice.invoice.lineItems.size})")
        Spacer(Modifier.height(Spacing.sm))
        Panel {
            if (invoice.invoice.lineItems.isEmpty()) {
                Text(
                    text = "No line items were recorded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                invoice.invoice.lineItems.forEachIndexed { index, item ->
                    if (index > 0) {
                        Spacer(Modifier.height(Spacing.md))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(Spacing.md))
                    }
                    Text(
                        text = item.description.ifBlank { "Item ${index + 1}" },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${formatQuantity(item.quantity)} x ${formatAmount(item.unitPrice)}",
                            style = MoneySmallTextStyle.copy(
                                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(text = formatAmount(item.amount), style = MoneySmallTextStyle)
                    }
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(Spacing.lg))

            LabelledAmountRow("Subtotal", formatAmount(invoice.invoice.subtotal))
            Spacer(Modifier.height(Spacing.sm))
            LabelledAmountRow("Tax", formatAmount(invoice.invoice.tax))
            Spacer(Modifier.height(Spacing.md))
            LabelledAmountRow(
                label = "Total ${invoice.currency}",
                amount = formatAmount(invoice.invoice.total),
                emphasised = true,
            )
        }
    }

    // --- Findings ----------------------------------------------------------------------------
    if (invoice.findings.isNotEmpty()) {
        Column {
            SectionHeader("Review findings (${invoice.findings.size})")
            Spacer(Modifier.height(Spacing.sm))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                invoice.findings.forEach { FindingRow(it) }
            }
        }
    }

    // --- Webhook response --------------------------------------------------------------------
    invoice.webhookResponse?.takeIf { it.isNotBlank() }?.let { response ->
        Column {
            SectionHeader("Webhook response")
            Spacer(Modifier.height(Spacing.sm))
            Panel {
                Text(
                    text = response,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    // --- Actions -----------------------------------------------------------------------------
    if (invoice.status == InvoiceStatus.DRAFT) {
        Column {
            Button(
                onClick = onSend,
                enabled = state.hasWebhook && !state.send.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text("Send to webhook", style = MaterialTheme.typography.titleMedium)
            }
            if (!state.hasWebhook) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "No webhook URL is configured. Add one in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.send.isLoading) {
                LoadingState("Posting to your webhook")
            }
            state.send.failure?.let { failure ->
                Spacer(Modifier.height(Spacing.md))
                ErrorState(
                    message = failure.message,
                    retryable = failure.retryable,
                    onRetry = onSend,
                )
            }
        }
    }

    if (state.send is RequestState.Done && state.sentMessage != null) {
        MessageCard(tone = MessageTone.SUCCESS, text = state.sentMessage)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}
