package com.example.invoicekeeper.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.invoicekeeper.data.repository.InvoiceRepository
import com.example.invoicekeeper.domain.model.SavedInvoice
import com.example.invoicekeeper.ui.repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val recentInvoices: List<SavedInvoice> = emptyList(),
    val hasWebhook: Boolean = true,
    val totalSaved: Int = 0,
) {
    val isEmpty: Boolean get() = !isLoading && recentInvoices.isEmpty()
}

class HomeViewModel(repository: InvoiceRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeAll(),
                repository.settings,
            ) { invoices, settings ->
                HomeUiState(
                    isLoading = false,
                    recentInvoices = invoices.take(3),
                    hasWebhook = settings.hasWebhook,
                    totalSaved = invoices.size,
                )
            }.collect { _uiState.value = it }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HomeViewModel(this.repository) }
        }
    }
}
