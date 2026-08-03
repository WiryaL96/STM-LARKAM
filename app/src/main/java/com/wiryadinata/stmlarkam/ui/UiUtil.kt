package com.wiryadinata.stmlarkam.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Formats a remaining-time value in millis as mm:ss (e.g. 25:00, 04:07). */
fun formatMmSs(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L)) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

/** Formats an epoch-millis date for display; returns "-" for an unset (0) value. */
fun formatTanggal(millis: Long): String {
    if (millis <= 0L) return "-"
    return dateFormatter.format(Date(millis))
}
