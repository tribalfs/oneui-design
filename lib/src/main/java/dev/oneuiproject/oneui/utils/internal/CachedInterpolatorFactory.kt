package dev.oneuiproject.oneui.utils.internal

import android.annotation.SuppressLint
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import androidx.annotation.RestrictTo
import androidx.appcompat.animation.SeslAnimationUtils
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object CachedInterpolatorFactory {
    private val interpolatorCache = ConcurrentHashMap<Type, SoftReference<Interpolator>>()

    enum class Type {
        SINE_IN_OUT_33,
        SINE_IN_OUT_60,
        SINE_IN_OUT_70,
        SINE_IN_OUT_80,
        SINE_IN_OUT_90,
        STORY_END_ANIMATION,
        RELATED_NUMBER_FADE_IN,
        ELASTIC_50,
        ELASTIC_CUSTOM,
        OVERSHOOT
    }

    fun getOrCreate(type: Type): Interpolator {
        return interpolatorCache[type]?.get() ?: createInterpolator(type).also {
            interpolatorCache[type] = SoftReference(it)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun createInterpolator(type: Type): Interpolator {
        return when (type) {
            Type.SINE_IN_OUT_33 -> PathInterpolator(0.33f, 0.0f, 0.67f, 1.0f)
            Type.SINE_IN_OUT_60 -> PathInterpolator(0.33f, 0.0f, 0.4f, 1.0f)
            Type.STORY_END_ANIMATION -> PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f)
            Type.RELATED_NUMBER_FADE_IN -> PathInterpolator(0.0f, 0.85f, 0.13f, 2.0f)
            Type.SINE_IN_OUT_70 -> SeslAnimationUtils.SINE_IN_OUT_70
            Type.SINE_IN_OUT_80 -> SeslAnimationUtils.SINE_IN_OUT_80
            Type.SINE_IN_OUT_90 -> SeslAnimationUtils.SINE_IN_OUT_90
            Type.ELASTIC_50 ->  SeslAnimationUtils.ELASTIC_50
            Type.ELASTIC_CUSTOM ->  createSeslElasticInterpolator(1.0f, 1.3f) ?: SeslAnimationUtils.ELASTIC_40
            Type.OVERSHOOT -> OvershootInterpolator()
        }
    }
}
