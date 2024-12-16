package dev.oneuiproject.oneui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.util.AttributeSet
import androidx.appcompat.widget.SeslSeekBar
import androidx.core.content.res.use
import dev.oneuiproject.oneui.design.R

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
        context.obtainStyledAttributes(attrs, R.styleable.SeekBarPlus).use{ a ->
            setSeamless(a.getBoolean(R.styleable.SeekBarPlus_seamless, true))
            centerBasedBar = a.getBoolean(R.styleable.SeekBarPlus_centerBasedBar, true)
            if (centerBasedBar) setMode(MODE_LEVEL_BAR)
        }
        progressTintMode = PorterDuff.Mode.SRC
        // This will make the view animate with recoil anim
        // when inside SeslLinearLayoutCompat
        isClickable = true
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
                val save = canvas.save()
                canvas.translate((paddingLeft + tickWidth).toFloat(), height / 2.0f)
                for (i in 0..2) {
                    tickMark.draw(canvas)
                    canvas.translate(width, 0.0f)
                }
                canvas.restoreToCount(save)
            }
        } else {
            super.drawTickMarks(canvas)
        }
    }
    @SuppressLint("RestrictedApi")
    fun getCurrentMode(): Int = mCurrentMode
}