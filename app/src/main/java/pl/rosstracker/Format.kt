package pl.rosstracker

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val polish = Locale.forLanguageTag("pl-PL")
private val zone = ZoneId.systemDefault()
private val dayMonth = DateTimeFormatter.ofPattern("dd.MM", polish)
private val dateTime = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", polish)

fun formatPrice(value: Double): String =
    NumberFormat.getNumberInstance(polish).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(value) + " zł"

fun formatDayMonth(epochMillis: Long): String = dayMonth.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

fun formatDateTime(epochMillis: Long): String = dateTime.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

/** Formats an ISO date such as `2026-11-04T00:00:00Z` as `04.11`. */
fun formatIsoDayMonth(iso: String?): String? =
    iso?.let { runCatching { formatDayMonth(Instant.parse(it).toEpochMilli()) }.getOrNull() }

fun discountPercent(price: Double, oldPrice: Double): Int = Math.round((1 - price / oldPrice) * 100).toInt()
