package dev.oneuiproject.oneui.layout.internal.widget

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Property
import android.view.View
import android.view.animation.Interpolator
import androidx.activity.BackEventCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.view.animation.PathInterpolatorCompat
import dev.oneuiproject.oneui.layout.internal.backapi.BackHandler
import androidx.appcompat.R as appcompatR

internal class SemToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = appcompatR.attr.toolbarStyle
) : Toolbar(context, attrs, defStyleAttr), BackHandler {


    private var backAnimator: ObjectAnimator? = null
    private var backInterpolator: Interpolator ? = null

    private val backProgress by lazy(LazyThreadSafetyMode.NONE) {
        object : Property<View, Float>(
            Float::class.java, "backProgress"
        ) {
            private var currentBackProgress: Float = 0f
            override fun get(view: View): Float = currentBackProgress
            override fun set(view: View, value: Float) {
                currentBackProgress = value
                view.alpha = value
            }
        }
    }

    override fun startBackProgress(backEvent: BackEventCompat) {
        backAnimator?.cancel()
        backAnimator = createBackAnimator()
    }

    private fun createBackAnimator(): ObjectAnimator {
        backInterpolator = PathInterpolatorCompat.create(0.70f, 0f, 0.4f, 0.85f)

        return ObjectAnimator.ofFloat(this, backProgress, 0f, 1f)
    }

    override fun updateBackProgress(backEvent: BackEventCompat) {
        backAnimator?.currentPlayTime = ((backInterpolator?.getInterpolation(backEvent.progress) ?: 0f)
                * backAnimator!!.duration).toLong()
    }

    override fun handleBackInvoked() = resetView()

    override fun cancelBackProgress() = resetView()

    private fun resetView() {
        if (backAnimator == null) return

        val target = backAnimator!!.target as View
        val currentProgress = (backProgress as Property<View, Float>).get(this)
        backAnimator?.cancel()
        backAnimator = null
        backInterpolator = null

        ObjectAnimator
            .ofFloat(target, backProgress, currentProgress, 0f).apply {
                duration = 200
                start()
            }
    }
}
