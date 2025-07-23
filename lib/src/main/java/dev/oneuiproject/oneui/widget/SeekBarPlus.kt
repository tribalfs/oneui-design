package dev.oneuiproject.oneui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.SeslSeekBar
import androidx.core.content.withStyledAttributes
import dev.oneuiproject.oneui.design.R
import androidx.core.graphics.withTranslation
import androidx.appcompat.widget.SeslProgressBar as SeslProgressBar

/**
 * A SeekBar that has a center based mode and seamless mode by default.
 * The seamless mode allows the thumb to be draggable seamlessly
 *
 * @attr ref dev.oneuiproject.oneui.design.R.styleable#SeekBarPlus_centerBasedBar
 * @attr ref dev.oneuiproject.oneui.design.R.styleable#SeekBarPlus_seamless
 *
 * @see centerBasedBar
 * @see setSeamless
 * @see setMode
 *
 */
class SeekBarPlus @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SeslSeekBar(context, attrs) {

    /**
     * If true, this sets [SeslSeekBar] to only put tick marks at the start, middle and end
     * regardless of the min and max value.
     * This will ignore [setMode] value and will always use the [MODE_LEVEL_BAR][SeslSeekBar.MODE_LEVEL_BAR] mode.
     * This is set to true by default.
     */
    var centerBasedBar = true
        set(value){
            if (field != value) {
                field = value
                if (value) setMode(MODE_LEVEL_BAR)
                invalidate()
            }
        }

    init {
        context.withStyledAttributes(attrs, R.styleable.SeekBarPlus) {
            setSeamless(getBoolean(R.styleable.SeekBarPlus_seamless, true))
            centerBasedBar = getBoolean(R.styleable.SeekBarPlus_centerBasedBar, true)
            if (centerBasedBar) super.setMode(MODE_LEVEL_BAR)
        }
        progressTintMode = PorterDuff.Mode.SRC
    }

    override fun performHapticFeedback(feedbackConstant: Int): Boolean {
        val progress = progress
        if (centerBasedBar) {
            if (progress.toFloat() != (min + max) / 2f && progress != min && progress != max) {
                return false
            }
        }
        return super.performHapticFeedback(feedbackConstant)
    }

    @SuppressLint("RestrictedApi")
    override fun drawTickMarks(canvas: Canvas) {
        if (centerBasedBar) {
            val tickMark = tickMark
            if (tickMark != null) {
                val tickWidth = tickMark.intrinsicWidth
                val tickHeight = tickMark.intrinsicHeight
                val right = if (tickWidth >= 0) tickWidth / 2 else 1
                val bottom = if (tickHeight >= 0) tickHeight / 2 else 1
                tickMark.setBounds(-right, -bottom, right, bottom)
                val paddingLeft = paddingLeft
                val width = (width - paddingLeft - paddingRight - tickWidth * 2) / 2.0f
                canvas.withTranslation((paddingLeft + tickWidth).toFloat(), height / 2.0f) {
                    for (i in 0..2) {
                        tickMark.draw(canvas)
                        canvas.translate(width, 0.0f)
                    }
                }
            }
        } else {
            super.drawTickMarks(canvas)
        }
    }
    /**
     * Gets the current mode of the SeekBar.
     *
     * @return The current mode.
     * @see setMode
     */
    @SuppressLint("RestrictedApi")
    fun getCurrentMode(): Int = mCurrentMode

    /**
     * Set the current mode. Ensure to set [centerBasedBar] to `false` first before invoking this.
     * Otherwise, the passed value will be ignored.
     *
     * Supported modes:
     * - [MODE_STANDARD][SeslProgressBar.MODE_STANDARD]: For progress selection; shows no tick mark.
     * - [MODE_EXPAND][SeslProgressBar.MODE_EXPAND]: Similar to MODE_STANDARD but with thicker track.
     * - [MODE_LEVEL_BAR][SeslProgressBar.MODE_LEVEL_BAR]: For levels selection; shows tick marks on selectable levels.
     * - [MODE_DUAL_COLOR][SeslProgressBar.MODE_DUAL_COLOR]: Similar MODE_EXPAND but shows dual colors which can be use to indicate a warning range
     * - [MODE_VERTICAL][SeslProgressBar.MODE_VERTICAL]: For vertical progress selection; shows no tick mark.
     * - [MODE_EXPAND_VERTICAL][SeslProgressBar.MODE_EXPAND_VERTICAL]:  Similar to MODE_VERTICAL but with thicker track.
     *
     * @param mode the mode to set.
     */
    override fun setMode(mode: Int) {
        if (centerBasedBar) {
            Log.w("SeekBarPlus", "Ensure to set `centerBasedBar` to false first before invoking setMode()." )
            return
        }
        super.setMode(mode)
    }
}