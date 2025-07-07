@file:Suppress("NOTHING_TO_INLINE")
package dev.oneuiproject.oneui.ktx

import android.util.Log
import androidx.annotation.ColorInt
import androidx.appcompat.widget.SeslSeekBar
import androidx.core.content.ContextCompat
import androidx.appcompat.R as appcompatR
import androidx.core.graphics.toColorInt

sealed interface SeslSeekBarDualColors{
    data object Default: SeslSeekBarDualColors
    data class Custom(
        /**
         * Overlap color for background tract
         */
        @field:ColorInt val bgColor: Int = "#f7c0bd".toColorInt(),
        /**
         * Overlap color for activated tract
         */
        @field:ColorInt val fgColor: Int = "#f1462f".toColorInt()
    ): SeslSeekBarDualColors
}

@JvmOverloads
inline fun <T: SeslSeekBar>T.updateDualColorRange(
    overlapPoint: Int,
    dualColors: SeslSeekBarDualColors = SeslSeekBarDualColors.Custom()
) {

    if (overlapPoint < min || overlapPoint > max){
        Log.e(this::class.simpleName, "updateDualColorRange: overlapPoint must be between $min and $max")
        return
    }

    setOverlapPointForDualColor(overlapPoint)

    if (dualColors is SeslSeekBarDualColors.Custom){
        setDualModeOverlapColor(dualColors.bgColor, dualColors.fgColor)
        return
    }

    val context = context
    if (context.isLightMode()){
        setDualModeOverlapColor(
            ContextCompat.getColor(context, appcompatR.color.sesl_seekbar_overlap_color_default_light),
            ContextCompat.getColor(context, appcompatR.color.sesl_seekbar_overlap_color_activated_light))
    }else{
        setDualModeOverlapColor(
            ContextCompat.getColor(context, appcompatR.color.sesl_seekbar_overlap_color_default_dark),
            ContextCompat.getColor(context, appcompatR.color.sesl_seekbar_overlap_color_activated_dark))

    }
}


inline fun <T: SeslSeekBar>T.setShowTickMarks(show: Boolean) {
    tickMark = if (show) {
        val context = context
        ContextCompat.getDrawable(context, androidx.appcompat.R.drawable.sesl_level_seekbar_tick_mark)
    }else null
}

/**
 * Sets a listener to handle progress change events on this [SeslSeekBar].
 *
 * @param onChanged Lambda function to be invoked when the progress of the seek bar changes.
 *                  Receives the current progress value.
 *
 * Example usage:
 * ```
 * seslSeekBar.onProgressChanged { progress ->
 *     // Handle progress change
 * }
 * ```
 */
inline fun <T: SeslSeekBar>T.onProgressChanged(crossinline onChanged: (Int) -> Unit) {
    setOnSeekBarChangeListener(object : SeslSeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeslSeekBar?, progress: Int, fromUser: Boolean) {
            onChanged.invoke(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeslSeekBar?) {

        }

        override fun onStopTrackingTouch(seekBar: SeslSeekBar?) {

        }
    })
}

