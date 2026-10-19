package com.xyon.airandroid.util

import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = -1
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return if (value >= 100.0) {
        String.format(Locale.getDefault(), "%.0f %s", value, units[unit])
    } else {
        String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
    }
}

fun formatSpeed(bytesPerSecond: Long): String {
    if (bytesPerSecond <= 0L) return "0 KB/s"
    return formatBytes(bytesPerSecond) + "/s"
}

fun formatPin(pin: String): String {
    if (pin.length != 6) return pin
    return pin.substring(0, 3) + " " + pin.substring(3)
}

fun formatRelativeTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "Ahora"
        minutes < 60 -> "Hace $minutes min"
        hours < 24 -> "Hace $hours h"
        days == 1L -> "Ayer"
        days < 7 -> "Hace $days días"
        else -> java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM)
            .format(java.util.Date(timestamp))
    }
}
