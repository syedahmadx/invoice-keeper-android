package com.example.invoicekeeper.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.invoicekeeper.domain.model.SavedInvoice
import com.example.invoicekeeper.ui.components.EmptyState
import com.example.invoicekeeper.ui.components.InvoiceCard
import com.example.invoicekeeper.ui.components.LoadingState
import com.example.invoicekeeper.ui.components.MessageCard
import com.example.invoicekeeper.ui.components.MessageTone
import com.example.invoicekeeper.ui.components.ScreenTitle
import com.example.invoicekeeper.ui.components.SectionHeader
import com.example.invoicekeeper.ui.components.Spacing

@Composable
fun HomeScreen(
    onScanClick: () -> Unit,
    onInvoiceClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onSeeAllClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        state = state,
        onScanClick = onScanClick,
        onInvoiceClick = onInvoiceClick,
        onSettingsClick = onSettingsClick,
        onSeeAllClick = onSeeAllClick,
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onScanClick: () -> Unit,
    onInvoiceClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onSeeAllClick: () -> Unit,
) {
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
                title = "Invoice Keeper",
                subtitle = "Turn a supplier invoice into checked, structured JSON and post it " +
                    "straight to your accounting automation.",
            )
        }

        item { Spacer(Modifier.height(Spacing.sm)) }

        item {
            Button(
                onClick = onScanClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(Icons.Outlined.DocumentScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(Spacing.md))
                Text("Scan an invoice", style = MaterialTheme.typography.titleMedium)
            }
        }

        if (!state.hasWebhook) {
            item {
                MessageCard(
                    tone = MessageTone.WARNING,
                    title = "No webhook configured",
                    text = "You can extract and review invoices, but nothing can be sent " +
                        "anywhere until you add a webhook URL.",
                    actionLabel = "Open Settings",
                    onAction = onSettingsClick,
                )
            }
        }

        item { Spacer(Modifier.height(Spacing.sm)) }

        item {
            SectionHeader(title = "Recent") {
                if (state.totalSaved > 3) {
                    TextButton(onClick = onSeeAllClick) {
                        Text("See all ${state.totalSaved}")
                    }
                }
            }
        }

        when {
            state.isLoading -> item { LoadingState("Loading saved invoices") }

            state.isEmpty -> item {
                EmptyState(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "Nothing saved yet",
                    description = "Invoices you extract will appear here so you can check what " +
                        "was sent and when.",
                )
            }

            else -> items(state.recentInvoices, key = SavedInvoice::id) { invoice ->
                InvoiceCard(invoice = invoice, onClick = { onInvoiceClick(invoice.id) })
            }
        }

        item { Spacer(Modifier.height(Spacing.sm)) }

        item {
            Text(
                text = "Your document is uploaded to the Invoice Keeper backend, which calls an " +
                    "AI provider on your behalf. No API keys are stored on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            )
        }
    }
}
