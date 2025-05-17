@file:Suppress("unused", "NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.dialog.internal

import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal inline fun getTimeText(context: Context, calendar: Calendar, is24HourView: Boolean): String {
    var pattern = (DateFormat.getTimeFormat(context) as SimpleDateFormat).toPattern()
    if (is24HourView) {
        pattern = pattern.replace("a", "").replace("h", "H").trim { it <= ' ' }
    }
    return SimpleDateFormat(
        pattern,
        getDisplayLocale(context)
    ).format(Date(calendar.timeInMillis))
}

internal inline fun getCustomCalendarInstance(hourOfDay: Int, minute: Int, is24HourView: Boolean): Calendar {
    val calendar = Calendar.getInstance()
    calendar.clear()
    calendar[if (is24HourView) 11 else 10] = hourOfDay
    calendar[12] = minute
    return calendar
}

internal inline fun getDisplayLocale(context: Context): Locale {
    var locale: Locale? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        locale = context.resources.configuration.locales[0]
    }
    return locale ?: Locale.getDefault()
}
