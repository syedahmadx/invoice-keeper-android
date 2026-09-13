package com.example.invoicekeeper.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Money is formatted with a fixed two-decimal grouping and the currency code shown beside it
 * rather than as a symbol: the app handles invoices in whatever currency the supplier used, and a
 * bare symbol invites the reader to assume the wrong one.
 */
fun formatAmount(value: Double): String =
    String.format(Locale.US, "%,.2f", value)

fun formatMoney(value: Double, currency: String): String {
    val code = currency.trim().uppercase()
    return if (code.isEmpty()) formatAmount(value) else "${formatAmount(value)} $code"
}

// Built per call rather than held in a val: the locale can change while the app is running, and
// SimpleDateFormat is not thread-safe anyway.
private fun dateFormat(pattern: String) = SimpleDateFormat(pattern, Locale.getDefault())

fun formatTimestamp(millis: Long): String = dateFormat("d MMM yyyy").format(Date(millis))

fun formatTimestampWithTime(millis: Long): String =
    dateFormat("d MMM yyyy, HH:mm").format(Date(millis))

/** Tolerance for comparing two money figures that should agree. */
private const val MONEY_EPSILON = 0.005

fun moneyMatches(a: Double, b: Double): Boolean = abs(a - b) < MONEY_EPSILON

/**
 * Parses a user-typed amount leniently: strips grouping separators and currency noise but refuses
 * anything that is not a number, so a typo clears the field rather than silently becoming zero.
 */
fun parseAmountOrNull(input: String): Double? {
    val cleaned = input.trim().replace(",", "").replace(" ", "")
    if (cleaned.isEmpty()) return 0.0
    if (cleaned == "-" || cleaned == "." || cleaned == "-.") return null
    return cleaned.toDoubleOrNull()
}

/** Trims trailing zeros so a quantity of 2.0 shows as "2". */
fun formatQuantity(value: Double): String =
    if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }

fun isValidHttpsUrl(url: String): Boolean {
    val trimmed = url.trim()
    if (!trimmed.startsWith("https://", ignoreCase = true)) return false
    val rest = trimmed.removePrefix("https://").removePrefix("HTTPS://")
    return rest.isNotBlank() && rest.contains('.') && !rest.startsWith("/")
}

fun isValidBaseUrl(url: String): Boolean {
    val trimmed = url.trim()
    val hasScheme = trimmed.startsWith("https://", ignoreCase = true) ||
        trimmed.startsWith("http://", ignoreCase = true)
    if (!hasScheme) return false
    val rest = trimmed.substringAfter("://")
    return rest.isNotBlank() && !rest.startsWith("/")
}
