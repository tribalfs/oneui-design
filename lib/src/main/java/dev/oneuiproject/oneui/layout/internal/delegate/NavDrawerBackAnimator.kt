package dev.oneuiproject.oneui.layout.internal.delegate


import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.util.LayoutDirection
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.activity.BackEventCompat
import androidx.annotation.RestrictTo
import androidx.core.view.GravityCompat
import androidx.core.view.animation.PathInterpolatorCompat
import com.google.android.material.R
import com.google.android.material.animation.AnimationUtils
import com.google.android.material.motion.MotionUtils
import dev.oneuiproject.oneui.ktx.dpToPxFactor
import dev.oneuiproject.oneui.layout.internal.backapi.BackAnimator


@SuppressLint("RestrictedApi")
@RestrictTo(RestrictTo.Scope.LIBRARY)
class NavDrawerBackAnimator(private val drawerPane: View,
                            private val contentPane: View): BackAnimator {

    companion object{
        private const val TAG = "DrawerBackAnimationDelegate"
        private const val RESET_ANIMATION_DURATION = 100L

    }

    private val dpToPx = drawerPane.context.dpToPxFactor
    private val maxScaleXDistanceShrink = 30f * dpToPx
    private val maxScaleXDistanceGrow = 30f * dpToPx
    private val maxScaleYDistance = 20f * dpToPx

    private var backEvent: BackEventCompat? = null

    private val progressInterpolator = MotionUtils.resolveThemeInterpolator(
        drawerPane.context, R.attr.motionEasingStandardDecelerateInterpolator,
        PathInterpolatorCompat.create(0f, 0f, 0f, 1f))

    private var startTranslationX = 0f
    override fun isBackProgressStarted(): Boolean = this.backEvent != null

    override fun startBackProgress(backEvent: BackEventCompat) {
        this.backEvent = backEvent
        startTranslationX = if (isRTL) -drawerPane.width.toFloat() else drawerPane.width.toFloat()
    }

    override fun updateBackProgress(backEvent: BackEventCompat) {
        if (this.backEvent == null) {
            Log.w(TAG, "Must call startBackProgress() before updateBackProgress()")
            this.backEvent = backEvent
            return
        }

        val finalBackEvent = this.backEvent!!
        this.backEvent = backEvent

        val leftSwipeEdge = finalBackEvent.swipeEdge == BackEventCompat.EDGE_LEFT
        updateBackProgress(finalBackEvent.progress, leftSwipeEdge)
    }

    private fun updateBackProgress(progress: Float, leftSwipeEdge: Boolean) {
        val drawerWidth = drawerPane.width
        if (drawerWidth <= 0f)  return

        val drawerHeight = drawerPane.height
        if ( drawerHeight <= 0f) return

        val leftGravity = isLeftGravity()
        drawerPane.pivotX = if (leftGravity) 0f else drawerWidth.toFloat()

        val interpolatedProgress = interpolateProgress(progress)
        val maxScaleDeltaShrink = maxScaleYDistance / drawerWidth

        val targetScale = 1 - AnimationUtils.lerp(0f, maxScaleDeltaShrink, interpolatedProgress)

        drawerPane.scaleX = targetScale
        drawerPane.scaleY = targetScale

        val swipeEdgeMatchesGravity = leftSwipeEdge == leftGravity
        val childScaleX = if (swipeEdgeMatchesGravity) targetScale else 1f
        val childScaleY = if (targetScale != 0f) (targetScale / targetScale * childScaleX) else 1f

        if (drawerPane is ViewGroup) {
            val childCount = drawerPane.childCount
            for (i in 0 until childCount) {
                val childView = drawerPane.getChildAt(i)
                // Preserve the original aspect ratio and container alignment of the child content, and add
                // content margins.
                childView.pivotX = if (leftGravity) {
                    (drawerWidth - childView.right + childView.width).toFloat()
                } else {
                    -childView.left.toFloat()
                }
                childView.pivotY = -childView.top.toFloat()
                childView.scaleX = childScaleX
                childView.scaleY = childScaleY
            }
        }
    }

    private val isRTL: Boolean
        get() = drawerPane.resources.configuration.layoutDirection == LayoutDirection.RTL


    override fun handleBackInvoked() {
        if (this.backEvent != null) {
            this.backEvent = null
            doResetAnimation()
        }
    }

    override fun cancelBackProgress() {
        if (this.backEvent == null) {
            Log.w(TAG, "Must call startBackProgress() and updateBackProgress() before cancelBackProgress()")
            return
        }
        this.backEvent = null
        doResetAnimation()
    }

    private fun doResetAnimation() {
        val animatorSet1 = AnimatorSet().apply {
            duration = 100L
        }
        val animatorSet2 = AnimatorSet()

        animatorSet1.playTogether(ObjectAnimator.ofFloat(drawerPane, View.SCALE_X, 1f))
        animatorSet2.playTogether(ObjectAnimator.ofFloat(drawerPane, View.SCALE_Y, 1f))

        for (i in 0 until (drawerPane as ViewGroup).childCount) {
            animatorSet1.playTogether(ObjectAnimator.ofFloat(drawerPane.getChildAt(i), View.SCALE_X, 1f))
            animatorSet2.playTogether(ObjectAnimator.ofFloat(drawerPane.getChildAt(i), View.SCALE_Y, 1f))
        }

        animatorSet1.playTogether(animatorSet2)
        animatorSet1.start()
    }

    private  fun isLeftGravity(): Boolean {
        val absoluteGravity = GravityCompat.getAbsoluteGravity(Gravity.START, drawerPane.getLayoutDirection())
        return (absoluteGravity and Gravity.LEFT) == Gravity.LEFT
    }

    private fun interpolateProgress(progress: Float): Float {
        return progressInterpolator.getInterpolation(progress)
    }

}
