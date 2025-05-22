@file:Suppress("NOTHING_TO_INLINE")
package dev.oneuiproject.oneui.ktx

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.util.TypedValue
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.util.SeslMisc
import androidx.core.app.ActivityOptionsCompat
import androidx.picker.app.SeslDatePickerDialog
import androidx.picker.app.SeslTimePickerDialog
import androidx.picker.widget.SeslDatePicker
import androidx.picker.widget.SeslTimePicker
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.popover.PopOverOptions
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.getWindowHeight
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.getWindowWidthNet
import java.util.Calendar

@get:JvmName("getDpToPxFactor")
inline val Context.dpToPxFactor get() = resources.displayMetrics.density

inline fun Context.getThemeAttributeValue(attr: Int): TypedValue? =
    TypedValue().run {
        if (theme.resolveAttribute(attr, this, true)) { this } else null
    }

@SuppressLint("RestrictedApi")
inline fun Context.isLightMode(): Boolean = SeslMisc.isLightTheme(this)

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
@JvmOverloads
inline fun Context.showDatePickerDialog(
    initialValue: Calendar,
    minSelectable: Calendar? = null,
    maxSelectable: Calendar? = null,
    @StringRes titleRes: Int? = null,
    onCreate: SeslDatePickerDialog.() -> Unit = {},
    crossinline onDateSelected: (datePicker: SeslDatePicker, calendar: Calendar)-> Unit
): SeslDatePickerDialog {
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
@JvmOverloads
inline fun Context.showTimePickerDialog(
    hour: Int,
    minute: Int,
    is24HoursFormat: Boolean? = null,
    onCreate: SeslTimePickerDialog.() -> Unit = {},
    crossinline onTimeSelected: (timePicker: SeslTimePicker, hourOfDay: Int, minute: Int)-> Unit
) : SeslTimePickerDialog {

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
 * Starts an activity in PopOver mode. This mode is only available to large display Samsung device with OneUI.
 *
 * @param activityClass The activity class to start.
 * @param popOverOptions See [PopOverOptions]
 * @param activityOptions (Optional) Additional options for how the Activity should be started.
 * See [android.app.ActivityOptions]
 *
 * Example usage:
 * ```
 * startPopOverActivity(
 *     activityClass = SearchActivity::class.java,
 *     popOverOptions = PopOverOptions(
 *         popOverSize = PopOverSize(
 *               731,
 *               360,
 *               if (context.isTablet) 731 else 574,
 *               360
 *        ),
 *        anchorPositions = if (resources.configuration.layoutDirection == LAYOUT_DIRECTION_RTL) {
 *            PopOverPositions(TOP_LEFT, TOP_LEFT)
 *        } else {
 *            PopOverPositions(TOP_RIGHT, TOP_RIGHT)
 *        }
 *   )
 * )
 * ```
 */
@JvmOverloads
inline fun <T : Activity> Context.startPopOverActivity(
    activityClass: Class<T>,
    popOverOptions: PopOverOptions? = null,
    activityOptions: Bundle? = null,
) {
    startPopOverActivity(
        Intent(this, activityClass),
        popOverOptions,
        activityOptions
    )
}

/**
 * Starts an activity in PopOver mode. This mode is only available to large display Samsung device with OneUI.
 *
 * @param intent The Intent for the activity to start.
 * @param popOverOptions See [PopOverOptions]
 * @param activityOptions (Optional) Additional options for how the Activity should be started.
 *
 * Example usage:
 * ```
 * startPopOverActivity(
 *     intent = Intent(context, SearchActivity::class.java),
 *     popOverOptions = PopOverOptions(
 *         popOverSize = PopOverSize(
 *               731,
 *               360,
 *               if (context.isTablet) 731 else 574,
 *               360
 *        ),
 *        anchorPositions = if (resources.configuration.layoutDirection == LAYOUT_DIRECTION_RTL) {
 *            PopOverPositions(TOP_LEFT, TOP_LEFT)
 *        } else {
 *            PopOverPositions(TOP_RIGHT, TOP_RIGHT)
 *        }
 *    )
 * )
 */
@JvmOverloads
fun Context.startPopOverActivity(
    intent: Intent,
    popOverOptions: PopOverOptions? = null,
    activityOptions: Bundle? = null,
) {
    startActivity(
        intent,
        (activityOptions ?: Bundle()).apply {
            popOverOptions?.let {
                putBoolean("android:activity.popOver", true)
                putBoolean("android:activity.popOverAllowOutsideTouch", it.allowOutsideTouch)
                putBoolean("android:activity.popOverRemoveOutlineEffect", it.removeOutline)
                putBoolean("android:activity.popOverRemoveDefaultMargin", it.removeDefaultMargin)
                putBoolean("android:activity.popOverInheritOptions", it.inheritOptions)
                putParcelableArray("android:activity.popOverAnchor", it.anchor.getPointArray())
                putIntArray("android:activity.popOverHeight",it.popOverSize.getHeightArray())
                putIntArray("android:activity.popOverWidth", it.popOverSize.getWidthArray())
                putIntArray("android:activity.popOverAnchorPosition",it.anchorPositions.getFlagArray())
            }
        }
    )
}

@JvmOverloads
fun startPopOverActivityForResult(
    intent: Intent,
    popOverOptions: PopOverOptions? = null,
    activityOptions: ActivityOptionsCompat? = null,
    resultLauncher: ActivityResultLauncher<Intent>
) {
    val activityOptionsBundle =  (activityOptions ?: ActivityOptionsCompat.makeBasic()).toBundle()!!

    activityOptionsBundle.apply {
        putBoolean("android:activity.popOver", true)
        popOverOptions?.let {
            putBoolean("android:activity.popOverAllowOutsideTouch", it.allowOutsideTouch)
            putBoolean("android:activity.popOverRemoveOutlineEffect", it.removeOutline)
            putBoolean("android:activity.popOverRemoveDefaultMargin", it.removeDefaultMargin)
            putBoolean("android:activity.popOverInheritOptions", it.inheritOptions)
            putParcelableArray("android:activity.popOverAnchor", it.anchor.getPointArray())
            putIntArray("android:activity.popOverHeight",it.popOverSize.getHeightArray())
            putIntArray("android:activity.popOverWidth", it.popOverSize.getWidthArray())
            putIntArray("android:activity.popOverAnchorPosition",it.anchorPositions.getFlagArray())
        }
    }


    resultLauncher.launch(
        intent.apply {
            putExtra(
                ActivityResultContracts.StartActivityForResult.EXTRA_ACTIVITY_OPTIONS_BUNDLE,
                activityOptionsBundle
            )
        })

}
