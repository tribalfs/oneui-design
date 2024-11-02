package dev.oneuiproject.oneui.ktx

import android.animation.Animator
import android.view.ViewPropertyAnimator

inline fun ViewPropertyAnimator.setListener(
    crossinline onStart: (animation: Animator) -> Unit = {},
    crossinline onEnd: (animation: Animator) -> Unit = {},
    crossinline onRepeat: (animation: Animator) -> Unit = {},
    crossinline onCancel: (animation: Animator) -> Unit = {},
    ): Animator.AnimatorListener {
    val listener = object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator) = onStart(animation)
        override fun onAnimationEnd(animation: Animator) = onEnd (animation)
        override fun onAnimationCancel(animation: Animator) = onRepeat(animation)
        override fun onAnimationRepeat(animation: Animator)  = onCancel(animation)
    }
    setListener(listener)
    return listener
}

/**
 * Add an action which will be invoked when the animation has ended.
 *
 * @return the [Animator.AnimatorListener] added to the Animator
 * @see Animator.end
 */
inline fun ViewPropertyAnimator.doOnEnd(
    crossinline action: (animation: Animator) -> Unit
): Animator.AnimatorListener = setListener(onEnd = action)

/**
 * Add an action which will be invoked when the animation has ended.
 *
 * @return the [Animator.AnimatorListener] added to the Animator
 * @see Animator.start
 */
inline fun ViewPropertyAnimator.doOnStart(
    crossinline action: (animation: Animator) -> Unit
): Animator.AnimatorListener = setListener(onStart = action)
