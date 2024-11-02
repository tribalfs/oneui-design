@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.utils.internal

import android.annotation.SuppressLint
import android.view.animation.Interpolator
import androidx.annotation.RestrictTo
import androidx.reflect.SeslBaseReflector
import java.lang.reflect.Constructor

@SuppressLint("RestrictedApi")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun createSeslElasticInterpolator(amplitude: Float, period: Float): Interpolator? {
        val constructor: Constructor<*> = SeslBaseReflector.getConstructor(
            "androidx.appcompat.animation.SeslElasticInterpolator", Float::class.java, Float::class.java) ?: return null
        constructor.isAccessible = true
        return constructor.newInstance(amplitude, period) as? Interpolator
}