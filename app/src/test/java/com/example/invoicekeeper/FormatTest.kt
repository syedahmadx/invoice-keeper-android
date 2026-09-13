package com.example.invoicekeeper

import com.example.invoicekeeper.util.formatAmount
import com.example.invoicekeeper.util.formatQuantity
import com.example.invoicekeeper.util.isValidBaseUrl
import com.example.invoicekeeper.util.isValidHttpsUrl
import com.example.invoicekeeper.util.moneyMatches
import com.example.invoicekeeper.util.parseAmountOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTest {

    @Test
    fun `amounts always show two decimals with grouping`() {
        assertEquals("1,234.50", formatAmount(1234.5))
        assertEquals("0.00", formatAmount(0.0))
        assertEquals("-19.99", formatAmount(-19.99))
    }

    @Test
    fun `quantities drop trailing zeros`() {
        assertEquals("2", formatQuantity(2.0))
        assertEquals("2.5", formatQuantity(2.5))
        assertEquals("0.25", formatQuantity(0.25))
    }

    @Test
    fun `typed amounts accept grouping separators and blanks`() {
        assertEquals(1234.5, parseAmountOrNull("1,234.50"))
        assertEquals(0.0, parseAmountOrNull(""))
        assertEquals(12.0, parseAmountOrNull(" 12 "))
    }

    @Test
    fun `typed amounts reject text rather than silently becoming zero`() {
        assertNull(parseAmountOrNull("twelve"))
        assertNull(parseAmountOrNull("12a"))
        assertNull(parseAmountOrNull("-"))
    }

    @Test
    fun `money comparison tolerates float noise but not real differences`() {
        assertTrue(moneyMatches(0.1 + 0.2, 0.3))
        assertTrue(moneyMatches(100.0, 100.001))
        assertFalse(moneyMatches(100.0, 100.01))
    }

    @Test
    fun `webhook URLs must be https with a host`() {
        assertTrue(isValidHttpsUrl("https://hook.eu2.make.com/abc123"))
        assertFalse(isValidHttpsUrl("http://hook.eu2.make.com/abc123"))
        assertFalse(isValidHttpsUrl("hook.eu2.make.com"))
        assertFalse(isValidHttpsUrl("https://"))
        assertFalse(isValidHttpsUrl(""))
    }

    @Test
    fun `base URLs may be http for a local test server`() {
        assertTrue(isValidBaseUrl("https://invoice-keeper-web.vercel.app"))
        assertTrue(isValidBaseUrl("http://10.0.2.2:3000"))
        assertFalse(isValidBaseUrl("invoice-keeper-web.vercel.app"))
        assertFalse(isValidBaseUrl(""))
    }
}
