package dev.oneuiproject.oneui.ktx

import androidx.picker.widget.SeslSpinningDatePicker
import java.util.Calendar
import java.util.Calendar.DAY_OF_MONTH
import java.util.Calendar.MONTH
import java.util.Calendar.YEAR

/**
 * Initializes the [SeslSpinningDatePicker] with the specified initial [Calendar] instance
 * and sets a listener to handle date selection events.
 *
 * @param initialValue [Calendar] instance to get the initial date to show.
 * @param onDateSelected Lambda to be invoked for the result containing the [Calendar] instance with the updated year, month and day of month.
 */
@JvmName("initDatePicker")
inline fun SeslSpinningDatePicker.init(
    initialValue: Calendar,
    crossinline onDateSelected: (Calendar) -> Unit = {}
){
    init(
        initialValue.get(YEAR),
        initialValue.get(MONTH),
        initialValue.get(DAY_OF_MONTH)
    ) { _: SeslSpinningDatePicker?, yr: Int, moy: Int, dom: Int ->
        onDateSelected.invoke((initialValue.clone() as Calendar).apply{set(yr, moy, dom)})
    }
}