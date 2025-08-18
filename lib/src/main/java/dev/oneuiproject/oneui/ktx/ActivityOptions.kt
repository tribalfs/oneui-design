@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.ktx

import android.app.ActivityOptions
import android.app.SemActivityOptions
import android.graphics.Point
import android.util.Log
import androidx.reflect.DeviceInfo
import dev.rikka.tools.refine.Refine

/**
 * Set the pop-over options for the activity. This method is only effective on
 * One UI devices with large displays. If none of the parameters are provided,
 * or all are set to null, the pop-over mode will be disabled.
 *
 * @param widths Optional. The widths of the pop-over window for each orientation.
 *               Index 0 is for portrait, index 1 is for landscape.
 * @param heights Optional. The heights of the pop-over window for each orientation.
 *                Index 0 is for portrait, index 1 is for landscape.
 * @param anchorsMarginsDp Optional. The margins of the anchor view in dp for each orientation.
 *                        Index 0 is for portrait, index 1 is for landscape.
 * @param anchorPositions Optional. The anchor positions for each orientation.
 *                        Index 0 is for portrait, index 1 is for landscape.
 *
 * @return The current [ActivityOptions] object, allowing for method chaining.
 */
@JvmOverloads
inline fun ActivityOptions.semSetPopOverOptions(
    widths: IntArray?= null,
    heights: IntArray? = null,
    anchorsMarginsDp: Array<Point>? = null,
    anchorPositions: IntArray? = null
): ActivityOptions {
    if (DeviceInfo.isOneUI()) {
        try {
            return Refine.unsafeCast<SemActivityOptions>(this)
                .semSetPopOverOptions(widths, heights, anchorsMarginsDp, anchorPositions)
        } catch (e: Throwable) {
            Log.e(this::class.simpleName, "semSetPopOverOptions, error: ${e.message}")
        }
    }
    return this
}
