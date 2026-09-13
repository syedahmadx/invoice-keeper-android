package com.example.invoicekeeper

import com.example.invoicekeeper.domain.model.ExtractedInvoice
import com.example.invoicekeeper.domain.model.InvoiceStatus
import com.example.invoicekeeper.domain.model.SavedInvoice
import com.example.invoicekeeper.ui.screens.invoices.InvoicesUiState
import com.example.invoicekeeper.ui.screens.invoices.StatusFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InvoicesFilterTest {

    private fun invoice(
        id: Long,
        supplier: String,
        number: String,
        currency: String,
        total: Double,
        status: InvoiceStatus,
    ) = SavedInvoice(
        id = id,
        savedAt = id,
        status = status,
        extractProvider = null,
        reviewProvider = null,
        sourceFileName = null,
        invoice = ExtractedInvoice(
            supplier = supplier,
            invoiceNumber = number,
            currency = currency,
            total = total,
        ),
    )

    private val all = listOf(
        invoice(1, "Contoso Supplies", "INV-2041", "GBP", 203.40, InvoiceStatus.SENT),
        invoice(2, "Northwind Paper", "NW-88", "GBP", 96.00, InvoiceStatus.DRAFT),
        invoice(3, "Fabrikam GmbH", "2026-117", "EUR", 1450.00, InvoiceStatus.SENT),
    )

    private val state = InvoicesUiState(isLoading = false, all = all)

    @Test
    fun `totals are grouped by currency and never added across them`() {
        val totals = state.totalsByCurrency
        assertEquals(2, totals.size)

        val gbp = totals.first { it.currency == "GBP" }
        assertEquals(2, gbp.count)
        assertEquals(299.40, gbp.total, 0.001)

        val eur = totals.first { it.currency == "EUR" }
        assertEquals(1, eur.count)
        assertEquals(1450.00, eur.total, 0.001)
    }

    @Test
    fun `search matches supplier or invoice number, case-insensitively`() {
        assertEquals(listOf(1L), state.copy(query = "contoso").visible.map { it.id })
        assertEquals(listOf(2L), state.copy(query = "nw-88").visible.map { it.id })
        assertEquals(3, state.copy(query = "  ").visible.size)
    }

    @Test
    fun `the status filter narrows the list`() {
        assertEquals(
            listOf(2L),
            state.copy(filter = StatusFilter.DRAFT).visible.map { it.id },
        )
        assertEquals(
            listOf(1L, 3L),
            state.copy(filter = StatusFilter.SENT).visible.map { it.id },
        )
    }

    @Test
    fun `search and filter combine`() {
        val narrowed = state.copy(query = "Fabrikam", filter = StatusFilter.DRAFT)
        assertTrue(narrowed.visible.isEmpty())
        assertTrue(narrowed.hasNoMatches)
        assertFalse(narrowed.hasNothingSaved)
    }

    @Test
    fun `an empty store reports nothing saved rather than no matches`() {
        val empty = InvoicesUiState(isLoading = false, all = emptyList())
        assertTrue(empty.hasNothingSaved)
        assertFalse(empty.hasNoMatches)
    }
}
