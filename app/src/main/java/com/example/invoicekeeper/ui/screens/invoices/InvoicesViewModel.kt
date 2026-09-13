package com.example.invoicekeeper.ui.screens.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.invoicekeeper.data.repository.InvoiceRepository
import com.example.invoicekeeper.domain.model.InvoiceStatus
import com.example.invoicekeeper.domain.model.SavedInvoice
import com.example.invoicekeeper.ui.repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StatusFilter(val label: String) {
    ALL("All"),
    DRAFT("Drafts"),
    SENT("Sent");

    fun accepts(status: InvoiceStatus): Boolean = when (this) {
        ALL -> true
        DRAFT -> status == InvoiceStatus.DRAFT
        SENT -> status == InvoiceStatus.SENT
    }
}

/**
 * Totals never cross currencies: an invoice in EUR and one in USD are counted in separate rows,
 * because adding them would produce a number that means nothing.
 */
data class CurrencyTotal(val currency: String, val count: Int, val total: Double)

data class InvoicesUiState(
    val isLoading: Boolean = true,
    val all: List<SavedInvoice> = emptyList(),
    val query: String = "",
    val filter: StatusFilter = StatusFilter.ALL,
) {
    val visible: List<SavedInvoice>
        get() = all.filter { filter.accepts(it.status) && it.matches(query) }

    val totalsByCurrency: List<CurrencyTotal>
        get() = visible
            .groupBy { it.currency }
            .map { (currency, invoices) ->
                CurrencyTotal(currency, invoices.size, invoices.sumOf { it.total })
            }
            .sortedByDescending { it.count }

    val hasNothingSaved: Boolean get() = !isLoading && all.isEmpty()

    val hasNoMatches: Boolean get() = !isLoading && all.isNotEmpty() && visible.isEmpty()

    val isFiltered: Boolean get() = query.isNotBlank() || filter != StatusFilter.ALL
}

class InvoicesViewModel(repository: InvoiceRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoicesUiState())
    val uiState: StateFlow<InvoicesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAll().collect { invoices ->
                _uiState.update { it.copy(isLoading = false, all = invoices) }
            }
        }
    }

    fun onQueryChange(query: String) = _uiState.update { it.copy(query = query) }

    fun onFilterChange(filter: StatusFilter) = _uiState.update { it.copy(filter = filter) }

    fun clearQuery() = _uiState.update { it.copy(query = "") }

    companion object {
        val Factory = viewModelFactory {
            initializer { InvoicesViewModel(this.repository) }
        }
    }
}
