package com.example.invoicekeeper

import com.example.invoicekeeper.domain.model.ExtractedInvoice
import com.example.invoicekeeper.domain.model.LineItem
import com.example.invoicekeeper.ui.screens.scan.InvoiceForm
import com.example.invoicekeeper.ui.screens.scan.LineItemForm
import com.example.invoicekeeper.ui.screens.scan.ScanStep
import com.example.invoicekeeper.ui.screens.scan.ScanUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InvoiceFormTest {

    private val sample = ExtractedInvoice(
        supplier = "Contoso Supplies Ltd",
        supplierTaxId = "GB123456789",
        invoiceNumber = "INV-2041",
        issueDate = "2026-08-14",
        dueDate = "2026-09-13",
        currency = "GBP",
        lineItems = listOf(
            LineItem("Paper A4, 80gsm", 10.0, 4.5, 45.0),
            LineItem("Toner cartridge", 2.0, 62.25, 124.5),
        ),
        subtotal = 169.5,
        tax = 33.9,
        total = 203.4,
    )

    @Test
    fun `a form round-trips an extracted invoice`() {
        val restored = InvoiceForm.from(sample).toInvoice()
        assertEquals(sample.supplier, restored.supplier)
        assertEquals(sample.invoiceNumber, restored.invoiceNumber)
        assertEquals(sample.currency, restored.currency)
        assertEquals(sample.lineItems.size, restored.lineItems.size)
        assertEquals(sample.subtotal, restored.subtotal, 0.001)
        assertEquals(sample.total, restored.total, 0.001)
    }

    @Test
    fun `blank optional fields become null rather than empty strings`() {
        val form = InvoiceForm.from(sample).copy(supplierTaxId = "", dueDate = "  ")
        val invoice = form.toInvoice()
        assertNull(invoice.supplierTaxId)
        assertNull(invoice.dueDate)
    }

    @Test
    fun `line items total is recomputed from the edited text`() {
        val form = InvoiceForm.from(sample)
        assertEquals(169.5, form.lineItemsTotal, 0.001)

        val edited = form.copy(
            lineItems = form.lineItems.map {
                if (it.key == 0L) it.copy(amount = "50.00") else it
            },
        )
        assertEquals(174.5, edited.lineItemsTotal, 0.001)
    }

    @Test
    fun `a non-numeric amount marks the form invalid`() {
        val form = InvoiceForm.from(sample).copy(total = "two hundred")
        assertTrue(form.hasNumberErrors)
        assertFalse(form.isValid)
    }

    @Test
    fun `missing required fields are listed by name`() {
        val form = InvoiceForm.from(sample).copy(supplier = "", currency = "")
        assertEquals(listOf("Supplier", "Currency"), form.missingRequiredFields)
    }

    @Test
    fun `currency is normalised to upper case`() {
        val form = InvoiceForm.from(sample).copy(currency = "gbp")
        assertEquals("GBP", form.toInvoice().currency)
    }

    @Test
    fun `send is refused with a reason when no webhook is configured`() {
        val state = ScanUiState(form = InvoiceForm.from(sample), hasWebhook = false)
        assertFalse(state.canSend)
        assertNotNull(state.sendDisabledReason)
        assertTrue(state.sendDisabledReason!!.contains("webhook"))
    }

    @Test
    fun `send is allowed once a webhook exists and the form is valid`() {
        val state = ScanUiState(form = InvoiceForm.from(sample), hasWebhook = true)
        assertTrue(state.canSend)
        assertNull(state.sendDisabledReason)
    }

    @Test
    fun `the step indicator follows the workflow`() {
        assertEquals(ScanStep.PICK, ScanUiState().currentStep)
        assertEquals(
            ScanStep.CHECK,
            ScanUiState(form = InvoiceForm.from(sample)).currentStep,
        )
        assertEquals(
            ScanStep.SEND,
            ScanUiState(form = InvoiceForm.from(sample), locked = true).currentStep,
        )
    }

    @Test
    fun `an empty line item is valid but contributes nothing`() {
        val form = InvoiceForm(lineItems = listOf(LineItemForm(key = 0L)))
        assertFalse(form.lineItems.first().hasNumberError)
        assertEquals(0.0, form.lineItemsTotal, 0.001)
    }
}
