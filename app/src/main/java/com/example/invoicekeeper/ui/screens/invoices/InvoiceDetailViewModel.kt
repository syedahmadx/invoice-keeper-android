package com.example.invoicekeeper.ui.screens.invoices

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.invoicekeeper.data.remote.NetworkResult
import com.example.invoicekeeper.data.repository.InvoiceRepository
import com.example.invoicekeeper.domain.model.SavedInvoice
import com.example.invoicekeeper.ui.navigation.Routes
import com.example.invoicekeeper.ui.repository
import com.example.invoicekeeper.ui.screens.scan.RequestState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InvoiceDetailUiState(
    val isLoading: Boolean = true,
    val invoice: SavedInvoice? = null,
    val send: RequestState = RequestState.Idle,
    val sentMessage: String? = null,
    val deleted: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val hasWebhook: Boolean = false,
) {
    val notFound: Boolean get() = !isLoading && invoice == null && !deleted
}

class InvoiceDetailViewModel(
    private val repository: InvoiceRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val invoiceId: Long = savedStateHandle.get<Long>(Routes.INVOICE_ID_ARG) ?: -1L

    private val _uiState = MutableStateFlow(InvoiceDetailUiState())
    val uiState: StateFlow<InvoiceDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeById(invoiceId).collect { invoice ->
                _uiState.update { current ->
                    // Once deleted, the flow emits null; do not flip back to "not found".
                    if (current.deleted) current else current.copy(isLoading = false, invoice = invoice)
                }
            }
        }
        viewModelScope.launch {
            repository.settings.collect { settings ->
                _uiState.update { it.copy(hasWebhook = settings.hasWebhook) }
            }
        }
    }

    fun send() {
        val invoice = _uiState.value.invoice ?: return
        if (_uiState.value.send.isLoading) return

        _uiState.update { it.copy(send = RequestState.Loading) }
        viewModelScope.launch {
            when (val result = repository.sendInvoice(invoice)) {
                is NetworkResult.Success -> {
                    repository.markSent(invoice.id, result.value.body.take(500))
                    _uiState.update {
                        it.copy(
                            send = RequestState.Done,
                            sentMessage = "Sent to your webhook (HTTP ${result.value.status}).",
                        )
                    }
                }
                is NetworkResult.Failure -> _uiState.update { it.copy(send = RequestState.Failed(result)) }
            }
        }
    }

    fun askToDelete() = _uiState.update { it.copy(showDeleteDialog = true) }

    fun dismissDeleteDialog() = _uiState.update { it.copy(showDeleteDialog = false) }

    fun confirmDelete() {
        viewModelScope.launch {
            repository.delete(invoiceId)
            _uiState.update { it.copy(showDeleteDialog = false, deleted = true, invoice = null) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                InvoiceDetailViewModel(this.repository, this.createSavedStateHandle())
            }
        }
    }
}
