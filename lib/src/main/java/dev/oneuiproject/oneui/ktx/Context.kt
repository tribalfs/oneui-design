@file:Suppress("NOTHING_TO_INLINE")
package dev.oneuiproject.oneui.ktx

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.format.DateFormat
import android.util.TypedValue
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.appcompat.R.color.sesl_secondary_text_dark
import androidx.appcompat.R.color.sesl_secondary_text_light
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.util.SeslMisc
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.picker.app.SeslDatePickerDialog
import androidx.picker.app.SeslTimePickerDialog
import androidx.picker.widget.SeslDatePicker
import androidx.picker.widget.SeslTimePicker
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.getWindowHeight
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.getWindowWidthNet
import java.util.Calendar
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.min

/**
 * The logical density of the display. This is a scaling factor for the Density
 * Independent Pixel unit, where one DIP is one pixel on an approximately 160 dpi
 * screen (for example a 240x320, 1.5"x2" screen), providing the baseline of
 * the system's display. Thus on a 160dpi screen this density value will be 1;
 * on a 120 dpi screen it would be .75; etc.
 */
@get:JvmName("getDpToPxFactor")
inline val Context.dpToPxFactor get() = resources.displayMetrics.density

/**
 * Retrieves the value of a theme attribute.
 *
 * @param attr The attribute to retrieve.
 * @return The value of the attribute, or null if the attribute could not be resolved.
 */
inline fun Context.getThemeAttributeValue(attr: Int): TypedValue? =
    TypedValue().run {
        if (theme.resolveAttribute(attr, this, true)) { this } else null
    }

/**
 * Checks if the current theme is a light theme.
 *
 * @return True if the current theme is light, false otherwise.
 */
@SuppressLint("RestrictedApi")
inline fun Context.isLightMode(): Boolean = SeslMisc.isLightTheme(this)

/**
 * Returns the [Activity] associated with this context, or null if the context is not an [Activity]
 * or is not associated with one.
 *
 * This property iterates through the context wrappers until it finds an Activity
 * or reaches the base context.
 */
val Context.activity: Activity?
    get() {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

/**
 * Returns the [AppCompatActivity] associated with this context, or null if the context is not
 * associated with an AppCompatActivity.
 *
 * This property iterates through the context wrappers until it finds an AppCompatActivity
 * or reaches the base context.
 */
val Context.appCompatActivity: AppCompatActivity?
    get() {
        var context = this
        while (context is ContextWrapper) {
            if (context is AppCompatActivity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

/**
 * Returns the [ComponentActivity] associated with this context, or null if the context is not
 * associated with an ComponentActivity.
 *
 * This property iterates through the context wrappers until it finds an AppCompatActivity
 * or reaches the base context.
 */
val Context.componentActivity: ComponentActivity?
    get() {
        var context = this
        while (context is ContextWrapper) {
            if (context is ComponentActivity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

/**
 * Retrieves the width of the activity window when called with an activity context,
 * excluding insets for navigation bars and display cutouts.
 *
 * If called with a non-activity context, it returns the width of the entire display,
 * minus the height of system decorations on API 29 and below.
 *
 * @return The width in pixels, adjusted for insets if applicable.
 */
inline val Context.windowWidthNetOfInsets: Int get() = getWindowWidthNet(this)

/**
 * Retrieves the height of the app window' when called with an activity context.
 * If called with a non-activity context, it returns the height of the
 * entire display (minus system decoration height on api29-)
 */
inline val Context.windowHeight: Int get() = getWindowHeight(this)

/**
 * Convenience method to show [SeslDatePickerDialog]
 *
 * @param initialValue Calendar instance to get the initial date to show
 * @param minSelectable  (optional) Calendar instance to be set as minimum date selectable
 * @param maxSelectable  (optional) Calendar instance to be set as maximum date selectable
 * @param titleRes (optional) For title to be shown on the dialog.
 * @param onCreate (Optional) Lambda to be invoked on [SeslDatePickerDialog] creation.
 * @param onDateSelected Lambda to be invoked for the result containing the [Calendar] instance with updated year, month and day of month.
 * @return [SeslDatePickerDialog] created and shown.
 */
@OptIn(ExperimentalContracts::class)
@JvmOverloads
inline fun Context.showDatePickerDialog(
    initialValue: Calendar,
    minSelectable: Calendar? = null,
    maxSelectable: Calendar? = null,
    @StringRes titleRes: Int? = null,
    onCreate: SeslDatePickerDialog.() -> Unit = {},
    crossinline onDateSelected: (datePicker: SeslDatePicker, calendar: Calendar)-> Unit
): SeslDatePickerDialog {
    contract { callsInPlace(onCreate, InvocationKind.EXACTLY_ONCE) }
    return SeslDatePickerDialog(
        this,
        getThemeAttributeValue(R.attr.datePickerDialogTheme)?.resourceId ?: 0,
        { datePicker: SeslDatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int ->
            onDateSelected.invoke(
                datePicker,
                (initialValue.clone() as Calendar).apply {
                    set(year, monthOfYear, dayOfMonth)
                })
        },
        initialValue[Calendar.YEAR],
        initialValue[Calendar.MONTH],
        initialValue[Calendar.DAY_OF_MONTH]
    ).apply{
        minSelectable?.let{
            datePicker.minDate =  it.time.time
        }
        maxSelectable?.let{
            datePicker.maxDate =  it.timeInMillis
        }
        titleRes?.let{
            setTitle(getString(it))
        }
        onCreate.invoke(this)
        show()
    }
}


/**
 * Convenience method to show [SeslTimePickerDialog]
 *
 * @param initialValue [Calendar] instance to get hour and minute to show as initial values.
 * @param is24HoursFormat (Optional) Boolean to force enable/disable 24-hour format. Uses system settings by default.
 * @param onCreate (Optional) Lambda to be invoked on [SeslTimePickerDialog] creation.
 * @param onTimeSelected Lambda to be invoked containing the selected hour and minute.
 * @return [SeslTimePickerDialog] created and shown.
 */
@JvmOverloads
inline fun Context.showTimePickerDialog(
    initialValue: Calendar,
    is24HoursFormat: Boolean? = null,
    crossinline onCreate: SeslTimePickerDialog.() -> Unit = {},
    crossinline onTimeSelected: (timePicker: SeslTimePicker, hourOfDay: Int, minute: Int)-> Unit
) : SeslTimePickerDialog {
    return showTimePickerDialog(initialValue[Calendar.HOUR_OF_DAY], initialValue[Calendar.MINUTE], is24HoursFormat, onCreate, onTimeSelected)
}

/**
 * Convenience method to create and show [SeslTimePickerDialog]
 *
 * @param hour Initial hour value to show based on the 24-hour clock.
 * @param minute Initial minute value to show.
 * @param is24HoursFormat (Optional) Boolean to force enable/disable 24-hour format. Uses system settings by default.
 * @param onCreate (Optional) Lambda to be invoked on [SeslTimePickerDialog] creation.
 * @param onTimeSelected Lambda to be invoked containing the selected hour and minute.
 * @return [SeslTimePickerDialog] created and shown.
 */
@OptIn(ExperimentalContracts::class)
@JvmOverloads
inline fun Context.showTimePickerDialog(
    hour: Int,
    minute: Int,
    is24HoursFormat: Boolean? = null,
    onCreate: SeslTimePickerDialog.() -> Unit = {},
    crossinline onTimeSelected: (timePicker: SeslTimePicker, hourOfDay: Int, minute: Int)-> Unit
) : SeslTimePickerDialog {
    contract { callsInPlace(onCreate, InvocationKind.EXACTLY_ONCE) }
    return SeslTimePickerDialog(
        this,
        getThemeAttributeValue(R.attr.timePickerDialogTheme)?.resourceId ?: 0,
        { stp, hod, m ->
            onTimeSelected.invoke(stp, hod, m)
        },
        hour,
        minute,
        is24HoursFormat ?: DateFormat.is24HourFormat(this)
    ).apply {
        onCreate()
        show()
    }
}

/**
 * Retrieves a [ColorStateList] suitable for a list item summary that is user-updatable.
 *
 * This color state list uses the `colorPrimaryDark` theme attribute for the enabled state.
 * For the disabled state, it uses the same color but with a reduced alpha (40% of the enabled alpha,
 * or at least 20% alpha, whichever is greater).
 *
 * This provides a visual cue that the summary's content can be changed by the user.
 */
inline val Context.userUpdatableSummaryColor: ColorStateList
    get(){
        val enabledColor = getThemeAttributeValue(androidx.appcompat.R.attr.colorPrimaryDark)!!.data
        val enabledAlpha = Color.alpha(enabledColor)
        val disabledAlpha = (enabledAlpha * 0.4).toInt()
            .coerceAtLeast(min(51, enabledAlpha)) // 51 represents 20% alpha
        val disabledColor = ColorUtils.setAlphaComponent(enabledColor, disabledAlpha)
        return ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_enabled),
                intArrayOf(-android.R.attr.state_enabled)
            ),
            intArrayOf(enabledColor, disabledColor)
        )
    }

/**
 * Retrieves the default summary color for a list item summary based on the current theme (light or dark).
 * This property returns a [ColorStateList] that represents the default color used for preference summaries.
 *
 * This is typically used as for summaries that are not user-updatable.
 *
 * @return A [ColorStateList] representing the default summary color.
 * @see userUpdatableSummaryColor
 */
inline val Context.defaultSummaryColor: ColorStateList
    @SuppressLint("RestrictedApi")
    get(){
        val colorResId = if (SeslMisc.isLightTheme(this)) sesl_secondary_text_light else sesl_secondary_text_dark
        return ContextCompat.getColorStateList(this, colorResId)!!
    }