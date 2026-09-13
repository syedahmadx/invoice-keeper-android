package com.example.invoicekeeper.ui.screens.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.invoicekeeper.ui.components.ErrorState
import com.example.invoicekeeper.ui.components.LoadingState
import com.example.invoicekeeper.ui.components.MessageCard
import com.example.invoicekeeper.ui.components.MessageTone
import com.example.invoicekeeper.ui.components.ScreenTitle
import com.example.invoicekeeper.ui.components.SectionHeader
import com.example.invoicekeeper.ui.components.Spacing
import com.example.invoicekeeper.ui.screens.scan.FormField
import com.example.invoicekeeper.ui.screens.scan.Panel
import com.example.invoicekeeper.ui.theme.LocalSemanticColors

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Spacer(Modifier.height(Spacing.sm))

        ScreenTitle(
            title = "Settings",
            subtitle = "Where confirmed invoices are posted, and which backend does the AI work.",
        )

        if (state.isLoading) {
            LoadingState("Loading settings")
        }

        state.clearedMessage?.let { message ->
            MessageCard(
                tone = MessageTone.SUCCESS,
                text = message,
                actionLabel = "Dismiss",
                onAction = viewModel::dismissMessages,
            )
        }

        // --- Webhook -----------------------------------------------------------------------
        Column {
            SectionHeader("Webhook")
            Spacer(Modifier.height(Spacing.sm))
            Panel {
                Text(
                    text = "Confirmed invoices are posted here as JSON. Point it at your Make, " +
                        "Zapier or n8n scenario, or your own endpoint.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.lg))
                FormField(
                    label = "Webhook URL",
                    value = state.webhookInput,
                    onValueChange = viewModel::onWebhookChange,
                    placeholder = "https://hook.eu2.make.com/...",
                    isError = state.webhookError != null,
                    supportingText = state.webhookError,
                    keyboardType = KeyboardType.Uri,
                )
                Spacer(Modifier.height(Spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Button(onClick = viewModel::saveWebhook, enabled = state.canSaveWebhook) {
                        Text("Save")
                    }
                    OutlinedButton(
                        onClick = viewModel::sendTestPayload,
                        enabled = state.canSendTest,
                    ) {
                        Text("Send a test payload")
                    }
                }

                state.webhookSavedMessage?.let { message ->
                    Spacer(Modifier.height(Spacing.md))
                    MessageCard(tone = MessageTone.SUCCESS, text = message)
                }

                if (state.savedWebhookUrl.isBlank()) {
                    Spacer(Modifier.height(Spacing.md))
                    MessageCard(
                        tone = MessageTone.WARNING,
                        text = "No webhook is configured yet, so invoices can only be saved as drafts.",
                    )
                }

                if (state.test.isLoading) {
                    LoadingState("Posting a small test payload")
                }

                state.testMessage?.let { message ->
                    Spacer(Modifier.height(Spacing.md))
                    MessageCard(tone = MessageTone.SUCCESS, text = message)
                }

                state.test.failure?.let { failure ->
                    Spacer(Modifier.height(Spacing.md))
                    ErrorState(
                        message = failure.message,
                        retryable = failure.retryable,
                        onRetry = viewModel::sendTestPayload,
                    )
                }
            }
        }

        // --- Backend -----------------------------------------------------------------------
        Column {
            SectionHeader("Backend")
            Spacer(Modifier.height(Spacing.sm))
            Panel {
                Text(
                    text = "The proxy that talks to the AI provider. Its API keys live on the " +
                        "server; this app never sees them.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.lg))
                FormField(
                    label = "Base URL",
                    value = state.baseUrlInput,
                    onValueChange = viewModel::onBaseUrlChange,
                    isError = state.baseUrlError != null,
                    supportingText = state.baseUrlError,
                    keyboardType = KeyboardType.Uri,
                )
                Spacer(Modifier.height(Spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Button(onClick = viewModel::saveBaseUrl, enabled = state.canSaveBaseUrl) {
                        Text("Save")
                    }
                    TextButton(onClick = viewModel::resetBaseUrl) { Text("Reset to default") }
                }

                state.baseUrlSavedMessage?.let { message ->
                    Spacer(Modifier.height(Spacing.md))
                    MessageCard(tone = MessageTone.SUCCESS, text = message)
                }

                Spacer(Modifier.height(Spacing.lg))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(Spacing.lg))

                ServerStatusRow(status = state.serverStatus, onRefresh = viewModel::checkServer)
            }
        }

        // --- Data and privacy --------------------------------------------------------------
        Column {
            SectionHeader("What leaves this device")
            Spacer(Modifier.height(Spacing.sm))
            Panel {
                BulletLine(
                    "The document you pick is uploaded to the backend above, which forwards it " +
                        "to an AI provider for extraction and review.",
                )
                BulletLine(
                    "Extracted invoice fields are sent to the provider a second time during " +
                        "review, and to your webhook when you confirm and send.",
                )
                BulletLine(
                    "Saved invoices, the webhook URL and the backend URL are stored only on " +
                        "this device. Nothing is synced anywhere.",
                )
                BulletLine(
                    "No AI API key is stored in this app, in its resources, or in the APK. " +
                        "Only the backend holds keys.",
                )
                BulletLine(
                    "The app requests only the INTERNET permission. Files are read through the " +
                        "system picker, one document at a time.",
                )
            }
        }

        // --- Danger zone -------------------------------------------------------------------
        Column {
            SectionHeader("Local data")
            Spacer(Modifier.height(Spacing.sm))
            Panel {
                Text(
                    text = "${state.savedInvoiceCount} invoice" +
                        (if (state.savedInvoiceCount == 1) "" else "s") +
                        " saved on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.md))
                OutlinedButton(
                    onClick = viewModel::askToClear,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Clear all local data")
                }
            }
        }

        Spacer(Modifier.height(Spacing.xxl))
    }

    if (state.showClearDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissClearDialog,
            title = { Text("Clear all local data?") },
            text = {
                Text(
                    "This deletes every saved invoice, the webhook URL and the backend URL from " +
                        "this device. It cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmClear,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Delete everything") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissClearDialog) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ServerStatusRow(status: ServerStatus, onRefresh: () -> Unit) {
    val semantic = LocalSemanticColors.current

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        when (status) {
            is ServerStatus.Checking -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(Spacing.md))
                Text(
                    text = "Checking the backend",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }

            is ServerStatus.Reachable -> {
                val config = status.config
                StatusDot(if (config.keyConfigured) semantic.success else semantic.warning)
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (config.keyConfigured) {
                            "Backend reachable, AI key configured"
                        } else {
                            "Backend reachable, but no AI key is configured on it"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    val providers = listOfNotNull(
                        config.extractionProvider?.let { "extraction by $it" },
                        config.reviewProvider?.let { "review by $it" },
                    )
                    if (providers.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = providers.joinToString(", ").replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "The server reports only whether a key exists. It never returns one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is ServerStatus.Unreachable -> {
                StatusDot(MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(Spacing.md))
                Text(
                    text = status.failure.message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }

            ServerStatus.Idle -> {
                StatusDot(MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(Spacing.md))
                Text(
                    text = "Backend not checked yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (status !is ServerStatus.Checking) {
            TextButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text("Check")
            }
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Surface(color = color, shape = CircleShape, modifier = Modifier.size(10.dp)) {}
}

@Composable
private fun BulletLine(text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm)) {
        Text(
            text = "-",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
