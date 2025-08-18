@file:Suppress("NOTHING_TO_INLINE")
package dev.oneuiproject.oneui.ktx

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.reflect.DeviceInfo
import dev.oneuiproject.oneui.popover.PopOverOptions

/**Returns whether this activity is in multi-window mode. */
inline val Activity.isInMultiWindowModeCompat get() = VERSION.SDK_INT >= 24 && isInMultiWindowMode

/**Hide the soft input if showing and optionally clear focus on the text input view.*/
@JvmOverloads
inline fun Activity.hideSoftInput(clearFocus: Boolean = true) {
    with (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager) {
        if (isAcceptingText) {
            if (clearFocus) currentFocus?.clearFocus()
            hideSoftInputFromWindow(window.decorView.windowToken, 0)
        }
    }
}

inline val Activity.fitsSystemWindows get() = window.decorView.fitsSystemWindows


/**
 * Starts an activity in PopOver mode. This mode is only available to large display
 * Samsung device with OneUI (API 28+). Otherwise, it behaves like a normal activity launch.
 *
 * This function provides a convenient way to launch an activity as a pop-over,
 * which is a floating window that appears on top of the current content.
 *
 * ## Example usage:
 * ```kotlin
 * startPopOverActivity(
 *     activityClass = SearchActivity::class.java,
 *     popOverOptions = PopOverOptions.topRightAnchored(context)
 *   )
 * )
 * ```
 *
 * @param activityClass The class of the [Activity] to start.
 * @param popOverOptions [PopOverOptions] configuration for the pop-over window.
 * Defaults to a [PopOverOptions.topCenterAnchored].
 *
 * @see PopOverOptions
 * @see startPopOverActivity
 * @see startPopOverActivityForResult
 */
@JvmOverloads
inline fun <T : Activity> Activity.startPopOverActivity(
    activityClass: Class<T>,
    popOverOptions: PopOverOptions = PopOverOptions.topCenterAnchored(this)
) {
    startPopOverActivity(
        Intent(this, activityClass),
        popOverOptions
    )
}

/**
 * Starts an activity in PopOver mode. This mode is only available to large display
 * Samsung device with OneUI (API 28+). Otherwise, it behaves like a normal activity launch.
 *
 * This function provides a convenient way to launch an activity as a pop-over,
 * which is a floating window that appears on top of the current content.
 *
 * ## Example usage:
 * ```kotlin
 * startPopOverActivity(
 *     intent = Intent(context, SearchActivity::class.java),
 *     popOverOptions = PopOverOptions.topRightAnchored(context)
 *   )
 * )
 * ```
 * @param intent The Intent for the activity to start.
 * @param popOverOptions [PopOverOptions] configuration for the pop-over window.
 * Defaults to a [PopOverOptions.topCenterAnchored].
 *
 * @see PopOverOptions
 * @see startPopOverActivity
 * @see startPopOverActivityForResult
 */
@JvmOverloads
fun Activity.startPopOverActivity(
    intent: Intent,
    popOverOptions: PopOverOptions = PopOverOptions.topCenterAnchored(this)
) {
    startActivity(
        intent,
        if (DeviceInfo.isOneUI()) {
            @SuppressLint("NewApi")//OneUI starts from API 28
            ActivityOptions.makeBasic().apply {
                semSetPopOverOptions(
                    popOverOptions.popOverSize.getWidthArray(),
                    popOverOptions.popOverSize.getHeightArray(),
                    popOverOptions.anchor.getPointArray(),
                    popOverOptions.anchorPositions.getFlagArray(),
                )
            }.toBundle()
        } else null
    )
}

/**
 * Starts an activity in PopOver mode for a result. This mode is only available to large display
 * Samsung device with OneUI (API28+). Otherwise, it behaves like a normal activity launch.
 *
 * This function provides a convenient way to launch an activity as a pop-over,
 * which is a floating window that appears on top of the current content.
 *
 * ## Example usage:
 * ```kotlin
 * val searchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
 *     // Handle the result
 * }
 *
 * startPopOverActivityForResult(
 *     intent = Intent(context, SearchActivity::class.java),
 *     popOverOptions = PopOverOptions.topRightAnchored(context)
 *     resultLauncher = searchLauncher
 * )
 * ```
 * @param intent The Intent for the activity to start.
 * @param popOverOptions See [PopOverOptions]
 * @param resultLauncher The [ActivityResultLauncher] to handle the activity result.
 *
 */
@JvmOverloads
inline fun Activity.startPopOverActivityForResult(
    intent: Intent,
    popOverOptions: PopOverOptions = PopOverOptions.topCenterAnchored(this),
    resultLauncher: ActivityResultLauncher<Intent>
) {

    val activityOptionsBundle = if (Build.VERSION.SDK_INT >= 24) {
        ActivityOptions.makeBasic().apply {
            semSetPopOverOptions(
                popOverOptions.popOverSize.getWidthArray(),
                popOverOptions.popOverSize.getHeightArray(),
                popOverOptions.anchor.getPointArray(),
                popOverOptions.anchorPositions.getFlagArray(),
            )
        }.toBundle()
    } else null

    resultLauncher.launch(
        intent.apply {
            putExtra(
                ActivityResultContracts.StartActivityForResult.EXTRA_ACTIVITY_OPTIONS_BUNDLE,
                activityOptionsBundle
            )
        })

}
