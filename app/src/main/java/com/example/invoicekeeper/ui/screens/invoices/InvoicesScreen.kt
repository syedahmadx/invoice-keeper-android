package com.example.invoicekeeper.ui.screens.invoices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.invoicekeeper.domain.model.SavedInvoice
import com.example.invoicekeeper.ui.components.EmptyState
import com.example.invoicekeeper.ui.components.InvoiceCard
import com.example.invoicekeeper.ui.components.LoadingState
import com.example.invoicekeeper.ui.components.ScreenTitle
import com.example.invoicekeeper.ui.components.Spacing
import com.example.invoicekeeper.ui.theme.MoneyTextStyle
import com.example.invoicekeeper.util.formatAmount

@Composable
fun InvoicesScreen(
    onInvoiceClick: (Long) -> Unit,
    onScanClick: () -> Unit,
    viewModel: InvoicesViewModel = viewModel(factory = InvoicesViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.lg,
            end = Spacing.lg,
            top = Spacing.xl,
            bottom = Spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            ScreenTitle(
                title = "Invoices",
                subtitle = "Everything saved on this device, drafts and sent alike.",
            )
        }

        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search supplier or invoice number") },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearQuery) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StatusFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { viewModel.onFilterChange(filter) },
                        label = { Text(filter.label) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        }

        if (state.visible.isNotEmpty()) {
            item { TotalsPanel(totals = state.totalsByCurrency) }
        }

        when {
            state.isLoading -> item { LoadingState("Loading saved invoices") }

            state.hasNothingSaved -> item {
                EmptyState(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "No invoices yet",
                    description = "Scan your first supplier invoice and it will be listed here.",
                    action = { Button(onClick = onScanClick) { Text("Scan an invoice") } },
                )
            }

            state.hasNoMatches -> item {
                EmptyState(
                    icon = Icons.Outlined.Search,
                    title = "Nothing matches",
                    description = if (state.query.isNotBlank()) {
                        "No saved invoice matches \"${state.query}\" with this filter."
                    } else {
                        "No saved invoice has that status."
                    },
                )
            }

            else -> items(state.visible, key = SavedInvoice::id) { invoice ->
                InvoiceCard(invoice = invoice, onClick = { onInvoiceClick(invoice.id) })
            }
        }
    }
}

@Composable
private fun TotalsPanel(totals: List<CurrencyTotal>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                text = "SHOWING",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))
            totals.forEachIndexed { index, total ->
                if (index > 0) Spacer(Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${total.count} invoice${if (total.count == 1) "" else "s"} " +
                            "in ${total.currency}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(Spacing.md))
                    Text(text = formatAmount(total.total), style = MoneyTextStyle)
                }
            }
        }
    }
}
