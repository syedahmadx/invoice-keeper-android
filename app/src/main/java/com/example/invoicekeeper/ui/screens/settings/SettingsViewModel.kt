package com.example.invoicekeeper.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.invoicekeeper.data.remote.ApiClient
import com.example.invoicekeeper.data.remote.NetworkResult
import com.example.invoicekeeper.data.remote.ServerConfig
import com.example.invoicekeeper.data.repository.InvoiceRepository
import com.example.invoicekeeper.ui.repository
import com.example.invoicekeeper.ui.screens.scan.RequestState
import com.example.invoicekeeper.util.isValidBaseUrl
import com.example.invoicekeeper.util.isValidHttpsUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ServerStatus {
    data object Idle : ServerStatus
    data object Checking : ServerStatus
    data class Reachable(val config: ServerConfig) : ServerStatus
    data class Unreachable(val failure: NetworkResult.Failure) : ServerStatus
}

data class SettingsUiState(
    val isLoading: Boolean = true,
    val webhookInput: String = "",
    val savedWebhookUrl: String = "",
    val baseUrlInput: String = "",
    val savedBaseUrl: String = ApiClient.DEFAULT_BASE_URL,
    val webhookSavedMessage: String? = null,
    val baseUrlSavedMessage: String? = null,
    val serverStatus: ServerStatus = ServerStatus.Idle,
    val test: RequestState = RequestState.Idle,
    val testMessage: String? = null,
    val showClearDialog: Boolean = false,
    val clearedMessage: String? = null,
    val savedInvoiceCount: Int = 0,
) {
    val webhookError: String?
        get() = when {
            webhookInput.isBlank() -> null
            !isValidHttpsUrl(webhookInput) -> "Must be a full https:// URL, for example " +
                "https://hook.eu2.make.com/abc123"
            else -> null
        }

    val webhookDirty: Boolean get() = webhookInput.trim() != savedWebhookUrl

    val canSaveWebhook: Boolean get() = webhookDirty && webhookError == null

    val baseUrlError: String?
        get() = when {
            baseUrlInput.isBlank() -> "The backend URL cannot be empty."
            !isValidBaseUrl(baseUrlInput) -> "Must start with https:// (or http:// for a local test server)."
            else -> null
        }

    val baseUrlDirty: Boolean get() = baseUrlInput.trim().trimEnd('/') != savedBaseUrl

    val canSaveBaseUrl: Boolean get() = baseUrlDirty && baseUrlError == null

    val canSendTest: Boolean get() = savedWebhookUrl.isNotBlank() && !test.isLoading
}

class SettingsViewModel(private val repository: InvoiceRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = repository.settings.first()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    webhookInput = settings.webhookUrl,
                    savedWebhookUrl = settings.webhookUrl,
                    baseUrlInput = settings.baseUrl,
                    savedBaseUrl = settings.baseUrl,
                )
            }
            checkServer()
        }
        viewModelScope.launch {
            repository.observeAll().collect { invoices ->
                _uiState.update { it.copy(savedInvoiceCount = invoices.size) }
            }
        }
    }

    fun onWebhookChange(value: String) {
        _uiState.update { it.copy(webhookInput = value, webhookSavedMessage = null, testMessage = null) }
    }

    fun saveWebhook() {
        val state = _uiState.value
        if (!state.canSaveWebhook) return
        viewModelScope.launch {
            val url = state.webhookInput.trim()
            repository.saveWebhookUrl(url)
            _uiState.update {
                it.copy(
                    savedWebhookUrl = url,
                    webhookInput = url,
                    webhookSavedMessage = if (url.isBlank()) {
                        "Webhook URL cleared."
                    } else {
                        "Webhook URL saved."
                    },
                    test = RequestState.Idle,
                    testMessage = null,
                )
            }
        }
    }

    fun onBaseUrlChange(value: String) {
        _uiState.update { it.copy(baseUrlInput = value, baseUrlSavedMessage = null) }
    }

    fun saveBaseUrl() {
        val state = _uiState.value
        if (!state.canSaveBaseUrl) return
        viewModelScope.launch {
            val url = state.baseUrlInput.trim().trimEnd('/')
            repository.saveBaseUrl(url)
            _uiState.update {
                it.copy(
                    savedBaseUrl = url,
                    baseUrlInput = url,
                    baseUrlSavedMessage = "Backend URL saved.",
                )
            }
            checkServer()
        }
    }

    fun resetBaseUrl() {
        viewModelScope.launch {
            repository.saveBaseUrl(ApiClient.DEFAULT_BASE_URL)
            _uiState.update {
                it.copy(
                    baseUrlInput = ApiClient.DEFAULT_BASE_URL,
                    savedBaseUrl = ApiClient.DEFAULT_BASE_URL,
                    baseUrlSavedMessage = "Reset to the default backend.",
                )
            }
            checkServer()
        }
    }

    fun checkServer() {
        if (_uiState.value.serverStatus is ServerStatus.Checking) return
        _uiState.update { it.copy(serverStatus = ServerStatus.Checking) }
        viewModelScope.launch {
            val status = when (val result = repository.serverConfig()) {
                is NetworkResult.Success -> ServerStatus.Reachable(result.value)
                is NetworkResult.Failure -> ServerStatus.Unreachable(result)
            }
            _uiState.update { it.copy(serverStatus = status) }
        }
    }

    fun sendTestPayload() {
        if (!_uiState.value.canSendTest) return
        _uiState.update { it.copy(test = RequestState.Loading, testMessage = null) }
        viewModelScope.launch {
            when (val result = repository.sendTestPayload()) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        test = RequestState.Done,
                        testMessage = "Your webhook answered with HTTP ${result.value.status}.",
                    )
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(test = RequestState.Failed(result))
                }
            }
        }
    }

    fun askToClear() = _uiState.update { it.copy(showClearDialog = true) }

    fun dismissClearDialog() = _uiState.update { it.copy(showClearDialog = false) }

    fun confirmClear() {
        viewModelScope.launch {
            repository.clearAllLocalData()
            _uiState.update {
                SettingsUiState(
                    isLoading = false,
                    baseUrlInput = ApiClient.DEFAULT_BASE_URL,
                    savedBaseUrl = ApiClient.DEFAULT_BASE_URL,
                    clearedMessage = "All saved invoices and settings have been deleted from this device.",
                )
            }
            checkServer()
        }
    }

    fun dismissMessages() {
        _uiState.update {
            it.copy(
                webhookSavedMessage = null,
                baseUrlSavedMessage = null,
                testMessage = null,
                clearedMessage = null,
                test = RequestState.Idle,
            )
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel(this.repository) }
        }
    }
}
