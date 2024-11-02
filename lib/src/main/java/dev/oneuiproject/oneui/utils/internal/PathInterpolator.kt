package dev.oneuiproject.oneui.utils.internal

import android.view.animation.Interpolator
import androidx.annotation.RestrictTo
import androidx.core.view.animation.PathInterpolatorCompat

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object PathInterpolator {
    enum class Type(
        val values: FloatArray
    ) {
        TYPE_SINE_IN_OUT_33(floatArrayOf(0.33f, 0.0f, 0.67f, 1.0f)),
        TYPE_SINE_IN_OUT_60(floatArrayOf(0.33f, 0.0f, 0.4f, 1.0f)),
        TYPE_SINE_IN_OUT_70(floatArrayOf(0.33f, 0.0f, 0.3f, 1.0f)),
        TYPE_SINE_IN_OUT_80(floatArrayOf(0.33f, 0.0f, 0.2f, 1.0f)),
        TYPE_SINE_IN_OUT_90(floatArrayOf(0.33f, 0.0f, 0.1f, 1.0f)),
        TYPE_STORY_END_ANIMATION(floatArrayOf(0.4f, 0.0f, 0.2f, 1.0f)),
        TYPE_RELATED_NUMBER_FADE_IN(floatArrayOf(0.0f, 0.85f, 0.13f, 2.0f))
    }

    fun create(type: Type): Interpolator {
        val values = type.values
        return values.let {
            PathInterpolatorCompat.create(it[0], it[1], it[2], it[3])
        }
    }

    fun create(x1: Float, y1: Float, x2: Float, y2: Float): Interpolator {
        return PathInterpolatorCompat.create(x1, y1, x2, y2)
    }
}
