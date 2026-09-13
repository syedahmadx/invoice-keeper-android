package com.example.invoicekeeper.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Types the backend accepts. Anything else is rejected before a request is made. */
val ACCEPTED_MIME_TYPES = arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf")

/** The Storage Access Framework filter. Narrowed further once the real type is known. */
val PICKER_MIME_FILTER = arrayOf("image/*", "application/pdf")

const val MAX_FILE_BYTES = 8L * 1024 * 1024

data class PickedFile(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
) {
    val readableSize: String get() = formatBytes(sizeBytes)

    val readableType: String
        get() = when (mimeType) {
            "application/pdf" -> "PDF"
            "image/jpeg" -> "JPEG image"
            "image/png" -> "PNG image"
            "image/webp" -> "WebP image"
            else -> mimeType
        }
}

sealed interface FileLoadResult {
    data class Success(val file: PickedFile) : FileLoadResult
    data class Rejected(val reason: String) : FileLoadResult
}

object FilePicker {

    /**
     * Reads the document's name, size and type without reading its contents. Rejects unsupported
     * types and anything over the 8 MB the backend accepts, so an oversized file never becomes an
     * 8 MB base64 string in memory.
     */
    suspend fun inspect(context: Context, uri: Uri): FileLoadResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)?.substringBefore(';')?.trim().orEmpty()

        var name = "document"
        var size = -1L
        runCatching {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                        name = cursor.getString(nameIndex)
                    }
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        }

        if (mimeType !in ACCEPTED_MIME_TYPES) {
            val shown = mimeType.ifBlank { "an unknown type" }
            return@withContext FileLoadResult.Rejected(
                "That file is $shown. Invoice Keeper accepts JPEG, PNG, WebP and PDF.",
            )
        }

        if (size < 0) {
            // Some providers do not report a size; fall back to measuring the stream.
            size = runCatching {
                resolver.openInputStream(uri)?.use { stream ->
                    var counted = 0L
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        counted += read
                        if (counted > MAX_FILE_BYTES) break
                    }
                    counted
                } ?: -1L
            }.getOrElse { -1L }
        }

        if (size < 0) {
            return@withContext FileLoadResult.Rejected(
                "That file could not be read. Try picking it again.",
            )
        }

        if (size > MAX_FILE_BYTES) {
            return@withContext FileLoadResult.Rejected(
                "That file is ${formatBytes(size)}. The limit is 8 MB.",
            )
        }

        if (size == 0L) {
            return@withContext FileLoadResult.Rejected("That file is empty.")
        }

        FileLoadResult.Success(PickedFile(uri, name, size, mimeType))
    }

    /**
     * Reads the document and base64-encodes it for the extract endpoint. Contents are never logged
     * and are held only for the length of the request.
     */
    suspend fun readAsBase64(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val read = stream.read(chunk)
                    if (read < 0) break
                    total += read
                    if (total > MAX_FILE_BYTES) return@runCatching null
                    buffer.write(chunk, 0, read)
                }
                Base64.encodeToString(buffer.toByteArray(), Base64.NO_WRAP)
            }
        }.getOrNull()
    }
}

fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(java.util.Locale.US, "%.0f KB", bytes / 1024.0)
    else -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}
