package dev.oneuiproject.oneui.utils

import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.animation.SeslRecoilAnimator
import androidx.appcompat.widget.SeslLinearLayoutCompat

@RequiresApi(29)
class SemTouchFeedbackAnimator(private val animateView: View) {
    companion object {
        private const val MOTION_EVENT_ACTION_PEN_DOWN: Int = 211
        private const val MOTION_EVENT_ACTION_PEN_UP: Int = 212
    }

    private val recoilAnimator by lazy(LazyThreadSafetyMode.NONE) {
        SeslRecoilAnimator.Holder(animateView.context)
    }

    private val backgroundHolder by lazy(LazyThreadSafetyMode.NONE) {
        SeslLinearLayoutCompat.ItemBackgroundHolder()
    }

    fun animate(motionEvent: MotionEvent) {
        val action = motionEvent.getAction()
        when (action) {
            MotionEvent.ACTION_DOWN, MOTION_EVENT_ACTION_PEN_DOWN -> {
                backgroundHolder.setPress(animateView)
                recoilAnimator.setPress(animateView)
            }

            MotionEvent.ACTION_UP, MOTION_EVENT_ACTION_PEN_UP -> {
                backgroundHolder.setRelease()
                recoilAnimator.setRelease()
            }

            MotionEvent.ACTION_CANCEL -> {
                backgroundHolder.setCancel()
                recoilAnimator.setRelease()
            }

            else -> Unit
        }
    }
}