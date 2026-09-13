package com.example.invoicekeeper.ui.screens.scan

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.invoicekeeper.data.remote.NetworkResult
import com.example.invoicekeeper.data.repository.InvoiceRepository
import com.example.invoicekeeper.domain.model.InvoiceStatus
import com.example.invoicekeeper.domain.model.SavedInvoice
import com.example.invoicekeeper.ui.androidApplication
import com.example.invoicekeeper.ui.repository
import com.example.invoicekeeper.util.FileLoadResult
import com.example.invoicekeeper.util.FilePicker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the whole scan workflow. Every piece of screen state lives here, so a rotation replays the
 * same state rather than restarting the request.
 */
class ScanViewModel(
    private val application: Application,
    private val repository: InvoiceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private var inFlight: Job? = null
    private var nextLineItemKey = 0L

    init {
        viewModelScope.launch {
            repository.settings.collect { settings ->
                _uiState.update { it.copy(hasWebhook = settings.hasWebhook) }
            }
        }
    }

    // --- Step 1: pick a file ------------------------------------------------------------------

    fun onFilePicked(uri: Uri?) {
        if (uri == null) return
        _uiState.update {
            it.copy(isReadingFile = true, fileError = null, pickedFile = null)
        }
        viewModelScope.launch {
            when (val result = FilePicker.inspect(application, uri)) {
                is FileLoadResult.Success -> _uiState.update {
                    it.copy(isReadingFile = false, pickedFile = result.file, fileError = null)
                }
                is FileLoadResult.Rejected -> _uiState.update {
                    it.copy(isReadingFile = false, pickedFile = null, fileError = result.reason)
                }
            }
        }
    }

    fun clearFile() {
        _uiState.update { it.copy(pickedFile = null, fileError = null) }
    }

    // --- Step 2: extract ----------------------------------------------------------------------

    fun extract() {
        val state = _uiState.value
        val file = state.pickedFile ?: return
        if (state.busy || state.locked) return

        _uiState.update {
            it.copy(
                extraction = RequestState.Loading,
                review = RequestState.Idle,
                findings = emptyList(),
                reviewProvider = null,
                findingsClearedByEdit = false,
                send = RequestState.Idle,
                sentMessage = null,
                draftSavedMessage = null,
                formErrorMessage = null,
            )
        }

        inFlight = viewModelScope.launch {
            val base64 = FilePicker.readAsBase64(application, file.uri)
            if (base64 == null) {
                _uiState.update {
                    it.copy(
                        extraction = RequestState.Failed(
                            NetworkResult.InvalidRequest(
                                "That file could not be read. Pick it again and retry.",
                            ),
                        ),
                    )
                }
                return@launch
            }

            when (val result = repository.extract(base64, file.mimeType, file.name)) {
                is NetworkResult.Success -> {
                    nextLineItemKey = result.value.invoice.lineItems.size.toLong()
                    _uiState.update {
                        it.copy(
                            extraction = RequestState.Done,
                            extractProvider = result.value.provider,
                            form = InvoiceForm.from(result.value.invoice),
                            savedInvoiceId = null,
                        )
                    }
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(extraction = RequestState.Failed(result))
                }
            }
        }
    }

    // --- Step 3: edit -------------------------------------------------------------------------

    /**
     * Any edit invalidates the review, because the findings described the numbers as they were.
     */
    private fun editForm(transform: (InvoiceForm) -> InvoiceForm) {
        _uiState.update { state ->
            val form = state.form ?: return@update state
            if (state.locked) return@update state
            val hadFindings = state.findings.isNotEmpty() || state.review is RequestState.Done
            state.copy(
                form = transform(form),
                findings = emptyList(),
                review = RequestState.Idle,
                reviewProvider = if (hadFindings) state.reviewProvider else null,
                findingsClearedByEdit = hadFindings,
                send = RequestState.Idle,
                sentMessage = null,
                formErrorMessage = null,
            )
        }
    }

    fun updateSupplier(value: String) = editForm { it.copy(supplier = value) }
    fun updateSupplierTaxId(value: String) = editForm { it.copy(supplierTaxId = value) }
    fun updateInvoiceNumber(value: String) = editForm { it.copy(invoiceNumber = value) }
    fun updateIssueDate(value: String) = editForm { it.copy(issueDate = value) }
    fun updateDueDate(value: String) = editForm { it.copy(dueDate = value) }
    fun updateCurrency(value: String) = editForm { it.copy(currency = value) }
    fun updateSubtotal(value: String) = editForm { it.copy(subtotal = value) }
    fun updateTax(value: String) = editForm { it.copy(tax = value) }
    fun updateTotal(value: String) = editForm { it.copy(total = value) }

    fun updateLineItem(key: Long, transform: (LineItemForm) -> LineItemForm) = editForm { form ->
        form.copy(lineItems = form.lineItems.map { if (it.key == key) transform(it) else it })
    }

    fun addLineItem() = editForm { form ->
        form.copy(lineItems = form.lineItems + LineItemForm(key = nextLineItemKey++))
    }

    fun removeLineItem(key: Long) = editForm { form ->
        form.copy(lineItems = form.lineItems.filterNot { it.key == key })
    }

    /** Copies the live line items total into the subtotal field. */
    fun useLineItemsTotalAsSubtotal() = editForm { form ->
        form.copy(subtotal = com.example.invoicekeeper.util.formatAmount(form.lineItemsTotal))
    }

    // --- Step 4: review -----------------------------------------------------------------------

    fun review() {
        val state = _uiState.value
        val form = state.form ?: return
        if (state.busy || state.locked) return

        _uiState.update {
            it.copy(review = RequestState.Loading, findings = emptyList(), findingsClearedByEdit = false)
        }

        inFlight = viewModelScope.launch {
            when (val result = repository.review(form.toInvoice())) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        review = RequestState.Done,
                        reviewProvider = result.value.provider,
                        findings = result.value.findings,
                    )
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(review = RequestState.Failed(result))
                }
            }
        }
    }

    // --- Steps 5 and 6: send / save -----------------------------------------------------------

    fun send() {
        val state = _uiState.value
        val form = state.form ?: return
        if (state.busy || state.locked) return

        val reason = state.sendDisabledReason
        if (reason != null) {
            _uiState.update { it.copy(formErrorMessage = reason) }
            return
        }

        _uiState.update { it.copy(send = RequestState.Loading, formErrorMessage = null) }

        inFlight = viewModelScope.launch {
            val saved = buildSavedInvoice(state, form, InvoiceStatus.SENT)
            when (val result = repository.send(repository.webhookPayload(saved))) {
                is NetworkResult.Success -> {
                    val body = result.value.body.take(500)
                    val id = repository.save(saved.copy(webhookResponse = body))
                    _uiState.update {
                        it.copy(
                            send = RequestState.Done,
                            locked = true,
                            savedInvoiceId = id,
                            sentMessage = "Sent to your webhook (HTTP ${result.value.status}) and " +
                                "saved locally. The form is now locked.",
                        )
                    }
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(send = RequestState.Failed(result))
                }
            }
        }
    }

    fun saveDraft() {
        val state = _uiState.value
        val form = state.form ?: return
        if (state.busy) return

        viewModelScope.launch {
            val saved = buildSavedInvoice(state, form, InvoiceStatus.DRAFT)
            val id = repository.save(saved)
            _uiState.update {
                it.copy(
                    savedInvoiceId = id,
                    draftSavedMessage = "Saved as a draft. You can send it later from Invoices.",
                )
            }
        }
    }

    private fun buildSavedInvoice(
        state: ScanUiState,
        form: InvoiceForm,
        status: InvoiceStatus,
    ): SavedInvoice = SavedInvoice(
        id = state.savedInvoiceId ?: 0L,
        savedAt = System.currentTimeMillis(),
        status = status,
        extractProvider = state.extractProvider,
        reviewProvider = state.reviewProvider,
        sourceFileName = state.pickedFile?.name,
        invoice = form.toInvoice(),
        findings = state.findings,
    )

    fun dismissMessages() {
        _uiState.update { it.copy(draftSavedMessage = null, findingsClearedByEdit = false) }
    }

    /** Clears everything so the user can scan the next invoice. */
    fun startAnother() {
        inFlight?.cancel()
        inFlight = null
        nextLineItemKey = 0L
        _uiState.update { ScanUiState(hasWebhook = it.hasWebhook) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ScanViewModel(this.androidApplication, this.repository) }
        }
    }
}
