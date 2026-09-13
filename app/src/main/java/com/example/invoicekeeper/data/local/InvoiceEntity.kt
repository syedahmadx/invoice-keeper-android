package com.example.invoicekeeper.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.invoicekeeper.data.remote.ApiClient
import com.example.invoicekeeper.domain.model.ExtractedInvoice
import com.example.invoicekeeper.domain.model.InvoiceStatus
import com.example.invoicekeeper.domain.model.ReviewFinding
import com.example.invoicekeeper.domain.model.SavedInvoice
import kotlinx.serialization.builtins.ListSerializer

/**
 * The invoice itself is stored as its JSON document rather than being shredded into columns: it is
 * always read and written whole, and keeping it intact means the payload that reaches the webhook
 * is byte-for-byte what was confirmed on screen. The columns that exist are exactly the ones the
 * list screen filters, searches and sorts on.
 */
@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "saved_at")
    val savedAt: Long,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "supplier")
    val supplier: String,

    @ColumnInfo(name = "invoice_number")
    val invoiceNumber: String,

    @ColumnInfo(name = "currency")
    val currency: String,

    @ColumnInfo(name = "total")
    val total: Double,

    @ColumnInfo(name = "extract_provider")
    val extractProvider: String?,

    @ColumnInfo(name = "review_provider")
    val reviewProvider: String?,

    @ColumnInfo(name = "source_file_name")
    val sourceFileName: String?,

    @ColumnInfo(name = "invoice_json")
    val invoiceJson: String,

    @ColumnInfo(name = "findings_json")
    val findingsJson: String,

    @ColumnInfo(name = "webhook_response")
    val webhookResponse: String?,
)

private val findingsSerializer = ListSerializer(ReviewFinding.serializer())

fun InvoiceEntity.toDomain(): SavedInvoice {
    val invoice = runCatching {
        ApiClient.json.decodeFromString(ExtractedInvoice.serializer(), invoiceJson)
    }.getOrElse { ExtractedInvoice() }

    val findings = runCatching {
        ApiClient.json.decodeFromString(findingsSerializer, findingsJson)
    }.getOrElse { emptyList() }

    return SavedInvoice(
        id = id,
        savedAt = savedAt,
        status = runCatching { InvoiceStatus.valueOf(status) }.getOrElse { InvoiceStatus.DRAFT },
        extractProvider = extractProvider,
        reviewProvider = reviewProvider,
        sourceFileName = sourceFileName,
        invoice = invoice,
        findings = findings,
        webhookResponse = webhookResponse,
    )
}

fun SavedInvoice.toEntity(): InvoiceEntity = InvoiceEntity(
    id = id,
    savedAt = savedAt,
    status = status.name,
    supplier = invoice.supplier,
    invoiceNumber = invoice.invoiceNumber,
    currency = invoice.currency,
    total = invoice.total,
    extractProvider = extractProvider,
    reviewProvider = reviewProvider,
    sourceFileName = sourceFileName,
    invoiceJson = ApiClient.json.encodeToString(ExtractedInvoice.serializer(), invoice),
    findingsJson = ApiClient.json.encodeToString(findingsSerializer, findings),
    webhookResponse = webhookResponse,
)
