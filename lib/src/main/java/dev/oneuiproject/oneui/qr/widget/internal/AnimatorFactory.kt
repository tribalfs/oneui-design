package dev.oneuiproject.oneui.qr.widget.internal

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory

/**
 * Small helper for building common animators used by the QR scanner UI.
 */
internal object AnimatorFactory {

    fun scale(
        view: View,
        from: Float,
        to: Float,
        duration: Long,
        interpolator: Interpolator
    ): ObjectAnimator {
        return ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.SCALE_X, from, to),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, from, to)
        ).apply {
            this.duration = duration
            this.interpolator = interpolator
        }
    }

    fun alpha(
        view: View,
        from: Float,
        to: Float,
        duration: Long = 200L,
        interpolator: Interpolator = LinearInterpolator()
    ): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, View.ALPHA, from, to).apply {
            this.duration = duration
            this.interpolator = interpolator
        }
    }

    fun rotation(
        view: View,
        from: Float = view.rotation,
        to: Float,
        duration: Long = 250L,
        listener: Animator.AnimatorListener? = null
    ): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, View.ROTATION, from, to).apply {
            this.duration = duration
            listener?.let { addListener(it) }
        }
    }

    /**
     * Convenience for the SINE_IN_OUT_60 interpolator used by ROI animations.
     */
    fun sineInOut60(): Interpolator =
        CachedInterpolatorFactory.getOrCreate(
            CachedInterpolatorFactory.Type.SINE_IN_OUT_60
        )

    /**
     * Convenience for the PATH_0_22_0_25_0_0_1_0 interpolator used by scanning animation.
     */
    fun defaultScanningPathInterpolator(): Interpolator =
        CachedInterpolatorFactory.getOrCreate(
            CachedInterpolatorFactory.Type.PATH_0_22_0_25_0_0_1_0
        )
}