@file:Suppress("NOTHING_TO_INLINE")
package dev.oneuiproject.oneui.ktx

import androidx.picker.widget.SeslDatePicker
import java.util.Calendar
import java.util.Calendar.DAY_OF_MONTH
import java.util.Calendar.MONTH
import java.util.Calendar.YEAR

/**
 * Initializes the [SeslDatePicker] with the specified initial date and sets a listener to handle date selection events.
 *
 * @param initialValue The initial date to display in the SeslDatePicker, taken from this [Calendar] instance.
 * @param onDateSelected Lambda function to be invoked when a date is selected.
 *                       Receives a [Calendar] instance containing the updated year, month, and day of month.
 *
 * Example usage:
 * ```
 * val initialDate = Calendar.getInstance()
 *
 * seslDatePicker.init(initialDate) { selectedDate ->
 *     // Handle selected date
 * }
 * ```
 */
inline fun SeslDatePicker.init(initialValue: Calendar, crossinline onDateSelected: (Calendar) -> Unit = {}) {
    init(
        initialValue.get(YEAR),
        initialValue.get(MONTH),
        initialValue.get(DAY_OF_MONTH)
    ) { _: SeslDatePicker?, yr: Int, moy: Int, dom: Int ->
        onDateSelected.invoke(
            (initialValue.clone() as Calendar).apply { set(yr, moy, dom) }
        )
    }
}


/**
 * Updates the [SeslDatePicker] to display the date specified by the given [Calendar].
 *
 * @param calendar The [Calendar] instance containing the date to be set in the SeslDatePicker.
 *
 * Example usage:
 * ```
 * val currentDate = Calendar.getInstance()
 * seslDatePicker.update(currentDate)
 * ```
 */
inline fun SeslDatePicker.update(calendar: Calendar) {
    updateDate(calendar.get(YEAR), calendar.get(MONTH), calendar.get(DAY_OF_MONTH))
}
