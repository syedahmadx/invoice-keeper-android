package com.example.invoicekeeper.data.remote

import com.example.invoicekeeper.domain.model.ExtractedInvoice
import com.example.invoicekeeper.domain.model.ReviewFinding
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// --- /api/extract ---------------------------------------------------------------------------

@Serializable
data class ExtractRequest(
    val fileBase64: String,
    val mimeType: String,
    val fileName: String,
)

@Serializable
data class ExtractResponse(
    val provider: String = "",
    val data: ExtractedInvoice,
)

// --- /api/review ----------------------------------------------------------------------------

@Serializable
data class ReviewRequest(
    val extracted: ExtractedInvoice,
)

@Serializable
data class ReviewResponse(
    val provider: String = "",
    val findings: List<ReviewFinding> = emptyList(),
)

// --- /api/send ------------------------------------------------------------------------------

@Serializable
data class SendRequest(
    val webhookUrl: String,
    val payload: JsonObject,
)

@Serializable
data class SendResponse(
    val ok: Boolean = false,
    val status: Int = 0,
    val body: String = "",
)

// --- /api/config ----------------------------------------------------------------------------

/**
 * The deployed backend answers with `apiKeyConfigured` plus the provider names, while the
 * documented contract says `configured`. Both are accepted so the app works against either.
 * Note what is *not* here: the key itself is never returned, and this app has no field for one.
 */
@Serializable
data class ConfigResponse(
    val configured: Boolean? = null,
    val apiKeyConfigured: Boolean? = null,
    val extractionProvider: String? = null,
    val reviewProvider: String? = null,
) {
    fun toServerConfig() = ServerConfig(
        keyConfigured = apiKeyConfigured ?: configured ?: false,
        extractionProvider = extractionProvider?.takeIf { it.isNotBlank() },
        reviewProvider = reviewProvider?.takeIf { it.isNotBlank() },
    )
}

data class ServerConfig(
    val keyConfigured: Boolean,
    val extractionProvider: String?,
    val reviewProvider: String?,
)

/**
 * Error envelope the backend returns for non-2xx responses. Every field is optional because
 * different failure paths (route handler, platform, upstream provider) word it differently.
 */
@Serializable
data class ApiErrorBody(
    val message: String? = null,
    val error: String? = null,
    @SerialName("detail") val detail: String? = null,
) {
    val text: String? get() = message ?: error ?: detail
}
