package com.example.invoicekeeper.domain.model

enum class InvoiceStatus {
    DRAFT,
    SENT;

    val label: String
        get() = when (this) {
            DRAFT -> "Draft"
            SENT -> "Sent"
        }
}

data class SavedInvoice(
    val id: Long = 0L,
    val savedAt: Long,
    val status: InvoiceStatus,
    val extractProvider: String?,
    val reviewProvider: String?,
    val sourceFileName: String?,
    val invoice: ExtractedInvoice,
    val findings: List<ReviewFinding> = emptyList(),
    val webhookResponse: String? = null,
) {
    val supplier: String get() = invoice.supplier.ifBlank { "Unknown supplier" }
    val invoiceNumber: String get() = invoice.invoiceNumber.ifBlank { "No number" }
    val currency: String get() = invoice.currency.ifBlank { "---" }
    val total: Double get() = invoice.total

    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return invoice.supplier.lowercase().contains(q) ||
            invoice.invoiceNumber.lowercase().contains(q)
    }
}
