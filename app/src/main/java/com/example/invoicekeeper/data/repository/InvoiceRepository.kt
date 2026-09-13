package com.example.invoicekeeper.data.repository

import com.example.invoicekeeper.data.local.AppSettings
import com.example.invoicekeeper.data.local.InvoiceDao
import com.example.invoicekeeper.data.local.SettingsStore
import com.example.invoicekeeper.data.local.toDomain
import com.example.invoicekeeper.data.local.toEntity
import com.example.invoicekeeper.data.remote.ApiClient
import com.example.invoicekeeper.data.remote.ApiService
import com.example.invoicekeeper.data.remote.CallTimeout
import com.example.invoicekeeper.data.remote.ExtractRequest
import com.example.invoicekeeper.data.remote.NetworkResult
import com.example.invoicekeeper.data.remote.ReviewRequest
import com.example.invoicekeeper.data.remote.SendRequest
import com.example.invoicekeeper.data.remote.SendResponse
import com.example.invoicekeeper.data.remote.ServerConfig
import com.example.invoicekeeper.data.remote.map
import com.example.invoicekeeper.data.remote.safeApiCall
import com.example.invoicekeeper.domain.model.ExtractedInvoice
import com.example.invoicekeeper.domain.model.InvoiceStatus
import com.example.invoicekeeper.domain.model.ReviewFinding
import com.example.invoicekeeper.domain.model.SavedInvoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

data class ExtractionOutcome(
    val provider: String,
    val invoice: ExtractedInvoice,
)

data class ReviewOutcome(
    val provider: String,
    val findings: List<ReviewFinding>,
)

/**
 * The single seam between the ViewModels and everything else. ViewModels never touch Retrofit,
 * Room or DataStore directly.
 */
class InvoiceRepository(
    private val api: ApiService,
    private val dao: InvoiceDao,
    private val settingsStore: SettingsStore,
) {

    // --- Settings -----------------------------------------------------------------------------

    val settings: Flow<AppSettings> = settingsStore.settings

    suspend fun currentSettings(): AppSettings = settingsStore.current()

    suspend fun saveWebhookUrl(url: String) = settingsStore.setWebhookUrl(url)

    suspend fun saveBaseUrl(url: String) = settingsStore.setBaseUrl(url)

    // --- Remote -------------------------------------------------------------------------------

    suspend fun extract(
        fileBase64: String,
        mimeType: String,
        fileName: String,
    ): NetworkResult<ExtractionOutcome> {
        val url = endpoint("/api/extract")
        return safeApiCall {
            api.extract(url, CallTimeout.LONG, ExtractRequest(fileBase64, mimeType, fileName))
        }.map { ExtractionOutcome(it.provider.ifBlank { "AI provider" }, it.data) }
    }

    suspend fun review(invoice: ExtractedInvoice): NetworkResult<ReviewOutcome> {
        val url = endpoint("/api/review")
        return safeApiCall {
            api.review(url, CallTimeout.SHORT, ReviewRequest(invoice))
        }.map { ReviewOutcome(it.provider.ifBlank { "AI provider" }, it.findings) }
    }

    suspend fun send(payload: JsonObject): NetworkResult<SendResponse> {
        val config = currentSettings()
        if (config.webhookUrl.isBlank()) {
            return NetworkResult.InvalidRequest("No webhook URL is configured. Add one in Settings.")
        }
        val url = endpoint("/api/send")
        return safeApiCall { api.send(url, CallTimeout.SHORT, SendRequest(config.webhookUrl, payload)) }
    }

    suspend fun sendInvoice(invoice: SavedInvoice): NetworkResult<SendResponse> =
        send(webhookPayload(invoice))

    suspend fun sendTestPayload(): NetworkResult<SendResponse> = send(
        buildJsonObject {
            put("source", "Invoice Keeper for Android")
            put("type", "test")
            put("message", "If you can see this, your webhook is reachable.")
            put("sentAt", System.currentTimeMillis())
        },
    )

    suspend fun serverConfig(): NetworkResult<ServerConfig> {
        val url = endpoint("/api/config")
        return safeApiCall { api.config(url, CallTimeout.SHORT) }.map { it.toServerConfig() }
    }

    private suspend fun endpoint(path: String): String {
        val base = currentSettings().baseUrl.trimEnd('/')
        return base + path
    }

    /** The JSON that actually reaches the user's accounting automation. */
    fun webhookPayload(invoice: SavedInvoice): JsonObject = buildJsonObject {
        put("source", "Invoice Keeper for Android")
        put("confirmedByUser", true)
        put("extractProvider", invoice.extractProvider ?: "")
        put("reviewProvider", invoice.reviewProvider ?: "")
        put("sourceFileName", invoice.sourceFileName ?: "")
        put("invoice", ApiClient.json.encodeToJsonElement(invoice.invoice))
    }

    // --- Local --------------------------------------------------------------------------------

    fun observeAll(): Flow<List<SavedInvoice>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(Dispatchers.IO)

    fun observeRecent(limit: Int = 3): Flow<List<SavedInvoice>> =
        dao.observeRecent(limit).map { list -> list.map { it.toDomain() } }.flowOn(Dispatchers.IO)

    fun observeById(id: Long): Flow<SavedInvoice?> =
        dao.observeById(id).map { it?.toDomain() }.flowOn(Dispatchers.IO)

    suspend fun save(invoice: SavedInvoice): Long = withContext(Dispatchers.IO) {
        if (invoice.id == 0L) {
            dao.insert(invoice.toEntity())
        } else {
            dao.update(invoice.toEntity())
            invoice.id
        }
    }

    suspend fun markSent(id: Long, response: String?) = withContext(Dispatchers.IO) {
        val existing = dao.getById(id)?.toDomain() ?: return@withContext
        dao.update(existing.copy(status = InvoiceStatus.SENT, webhookResponse = response).toEntity())
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) { dao.deleteById(id) }

    suspend fun clearAllLocalData() = withContext(Dispatchers.IO) {
        dao.deleteAll()
        settingsStore.clear()
    }

    suspend fun countSaved(): Int = withContext(Dispatchers.IO) { dao.observeAll().first().size }
}
