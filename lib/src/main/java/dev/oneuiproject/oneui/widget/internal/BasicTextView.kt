package dev.oneuiproject.oneui.widget.internal

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.RestrictTo
import kotlin.math.round

/**
 * A custom TextView that adjusts its text size based on the device's font scale and density.
 *
 * This class is designed to ensure text remains legible and appropriately scaled across different
 * device configurations. It achieves this by:
 * 1.  Calculating a target text scale based on the system's font scale, capped at a predefined [FONT_SCALE_LARGE].
 * 2.  Applying this target scale to the initial text size.
 * 3.  If auto-sizing is enabled (API 26+), it scales the auto-size parameters (min, max, step granularity)
 *     using the same target text scale.
 *
 * @param context The Context the view is running in, through which it can
 *        access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view. May be null.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class BasicTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : TextView(context, attrs) {

    private companion object {
        const val FONT_SCALE_LARGE = 1.3f
    }

    var isButton: Boolean = false

    init {
        @Suppress("DEPRECATION") val scaledDensity = context.resources.displayMetrics.scaledDensity
        val density = context.resources.displayMetrics.density
        val textSizeInScaledPixels = round(textSize / scaledDensity).toFloat()
        val fontScale = context.resources.configuration.fontScale
        var targetTextScale = fontScale.coerceAtMost(FONT_SCALE_LARGE)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeInScaledPixels * targetTextScale * density)

        if (Build.VERSION.SDK_INT >= 26 && autoSizeTextType == AUTO_SIZE_TEXT_TYPE_UNIFORM) {
            val autoSizeMinTextSizeInScaledPixels = round(autoSizeMinTextSize / scaledDensity).toFloat()
            val scaledMinTextSizeInPixels = (autoSizeMinTextSizeInScaledPixels * targetTextScale * density).toInt()
            val scaledMaxTextSizeInPixels = (round(autoSizeMaxTextSize / scaledDensity).toFloat() * targetTextScale * density).toInt()
            val scaledStepGranularityInPixels = (round(autoSizeStepGranularity / scaledDensity).toFloat() * targetTextScale * density).toInt()
            setAutoSizeTextTypeUniformWithConfiguration(
                scaledMinTextSizeInPixels.coerceAtLeast(1),
                scaledMaxTextSizeInPixels.coerceAtLeast(1),
                scaledStepGranularityInPixels.coerceAtLeast(1),
                TypedValue.COMPLEX_UNIT_PX
            )
        }
    }

    override fun getAccessibilityClassName(): CharSequence =
         if (isButton) "android.widget.Button" else super.getAccessibilityClassName()

    fun setButtonAttr(isButton: Boolean) {
        this.isButton = isButton
    }
}