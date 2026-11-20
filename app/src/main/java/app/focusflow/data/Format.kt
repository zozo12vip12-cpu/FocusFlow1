package app.focusflow.data

import java.text.NumberFormat
import java.util.Locale

/** 1500000 ms -> "25:00" (rounded up, so the last second shows 00:01 and never 00:00 early). */
fun formatClock(ms: Long): String {
    val totalSec = ((ms + 999L) / 1000L).toInt().coerceAtLeast(0)
    return String.format(Locale.getDefault(), "%02d:%02d", totalSec / 60, totalSec % 60)
}

/** Number formatted with the digits of the user's language (Arabic-Indic digits in some Arabic locales). */
fun Int.localized(): String = NumberFormat.getIntegerInstance().format(this.toLong())
