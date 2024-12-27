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
import dev.oneuiproject.oneui.layout.internal.util.DrawerLayoutInterface


@SuppressLint("RestrictedApi")
@RestrictTo(RestrictTo.Scope.LIBRARY)
class DrawerBackAnimator(drawerLayoutInterface: DrawerLayoutInterface): BackAnimator {

    companion object{
        private const val TAG = "DrawerBackAnimationDelegate"
        private const val RESET_ANIMATION_DURATION = 100L

    }
    private val drawerPane: View = drawerLayoutInterface.getDrawerPane()
    private val contentPane: View = drawerLayoutInterface.getContentPane()

    private val dpToPx = drawerPane.context.dpToPxFactor
    private val maxScaleXDistanceShrink = 35f * dpToPx
    private val maxScaleXDistanceGrow = 30f * dpToPx
    private val maxScaleYDistance = 26f * dpToPx

    private var backEvent: BackEventCompat? = null
    private var startProgress = 0f

    private val progressInterpolator = MotionUtils.resolveThemeInterpolator(
        drawerPane.context, R.attr.motionEasingStandardDecelerateInterpolator,
        PathInterpolatorCompat.create(0f, 0f, 0f, 1f))

    private var startTranslationX = 0f
    override fun isBackProgressStarted(): Boolean = this.backEvent != null

    override fun startBackProgress(backEvent: BackEventCompat) {
        this.backEvent = backEvent
        startProgress = backEvent.progress
        startTranslationX = if (isRTL) -drawerPane.width.toFloat() else drawerPane.width.toFloat()
    }

    override fun updateBackProgress(backEvent: BackEventCompat) {
        if (this.backEvent == null) {
            Log.w(TAG, "Must call startBackProgress() before updateBackProgress()")
            this.backEvent = backEvent
            startProgress = backEvent.progress
            return
        }

        val finalBackEvent = this.backEvent!!
        this.backEvent = backEvent

        val leftSwipeEdge = finalBackEvent.swipeEdge == BackEventCompat.EDGE_LEFT
        updateBackProgress(finalBackEvent.progress - startProgress, leftSwipeEdge)
    }


    private fun updateBackProgress(progress: Float, leftSwipeEdge: Boolean) {
        val drawerWidth = drawerPane.width
        if (drawerWidth <= 0f)  return

        val drawerHeight = drawerPane.height
        if ( drawerHeight <= 0f) return

        val interpolatedProgress = interpolateProgress(progress)
        val leftGravity: Boolean = isLeftGravity()

        val swipeEdgeMatchesGravity = leftSwipeEdge == leftGravity

        val maxScaleXDeltaShrink: Float = maxScaleXDistanceShrink / drawerWidth
        val maxScaleXDeltaGrow: Float = maxScaleXDistanceGrow / drawerWidth
        val maxScaleYDelta: Float = maxScaleYDistance / drawerHeight

        drawerPane.pivotX = (if (leftGravity) 0 else drawerWidth).toFloat()
        val endScaleXDelta = if (swipeEdgeMatchesGravity) maxScaleXDeltaGrow else -maxScaleXDeltaShrink
        val scaleXDelta = AnimationUtils.lerp(0f, endScaleXDelta, interpolatedProgress)
        val scaleX = 1 + scaleXDelta
        val scaleYDelta = AnimationUtils.lerp(0f, maxScaleYDelta, interpolatedProgress)
        val scaleY = 1 - scaleYDelta

        drawerPane.scaleX = scaleX
        drawerPane.scaleY = scaleY

        contentPane.translationX = startTranslationX +
                if (isRTL) -scaleXDelta * drawerWidth else scaleXDelta * drawerWidth

        if (drawerPane is ViewGroup) {
            val viewGroup = drawerPane
            for (i in 0 until viewGroup.childCount) {
                val childView = viewGroup.getChildAt(i)
                // Preserve the original aspect ratio and container alignment of the child content, and add
                // content margins.
                childView.pivotX = (if (leftGravity)
                    (drawerWidth - childView.right + childView.width)
                else
                    -childView.left).toFloat()
                childView.pivotY = -childView.top.toFloat()
                val childScaleX = if (swipeEdgeMatchesGravity) 1 - scaleXDelta else 1f
                val childScaleY = if (scaleY != 0f) scaleX / scaleY * childScaleX else 1f

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
            doResetAnimation(false)
        }
    }

    override fun cancelBackProgress() {
        if (this.backEvent == null) {
            Log.w(TAG, "Must call startBackProgress() and updateBackProgress() before cancelBackProgress()")
            return
        }
        this.backEvent = null
        doResetAnimation(true)
    }

    private fun doResetAnimation(isCancel: Boolean) {
        val animatorSet1 = AnimatorSet().apply {
            duration = RESET_ANIMATION_DURATION
        }
        val animatorSet2 = AnimatorSet().apply {
            startDelay = if (isCancel) 0 else 250
        }

        animatorSet1.playTogether(ObjectAnimator.ofFloat(drawerPane, View.SCALE_X, 1f))

        animatorSet2.playTogether(ObjectAnimator.ofFloat(drawerPane, View.SCALE_Y, 1f))
        if (isCancel) {
            animatorSet2.playTogether(ObjectAnimator.ofFloat(contentPane, View.TRANSLATION_X, startTranslationX))
        }

        if (drawerPane is ViewGroup) {
            for (i in 0 until drawerPane.childCount) {
                animatorSet1.playTogether(ObjectAnimator.ofFloat(drawerPane.getChildAt(i), View.SCALE_X, 1f))
                animatorSet2.playTogether(ObjectAnimator.ofFloat(drawerPane.getChildAt(i), View.SCALE_Y, 1f))
            }
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
