
package dev.oneuiproject.oneui.layout.internal.delegate

import androidx.activity.BackEventCompat
import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
import dev.oneuiproject.oneui.layout.DrawerLayout
import dev.oneuiproject.oneui.layout.internal.backapi.BackAnimator


@RestrictTo(RestrictTo.Scope.LIBRARY)
open class DrawerLayoutBackHandler<T: DrawerLayout>(
    private val mDrawerLayout: T,
    private val mDrawerBackAnimator: BackAnimator
): ToolbarLayoutBackHandler(mDrawerLayout) {

    override fun startBackProgress(backEvent: BackEventCompat) {
        if (mDrawerLayout.shouldAnimateDrawer) {
            mDrawerBackAnimator.startBackProgress(backEvent)
        }
    }

    override fun updateBackProgress(backEvent: BackEventCompat) {
        if (mDrawerBackAnimator.isBackProgressStarted()) {
            mDrawerBackAnimator.updateBackProgress(backEvent)
        }
    }

    @CallSuper
    override fun handleBackInvoked() {
        if (mDrawerBackAnimator.isBackProgressStarted() || mDrawerLayout.shouldCloseDrawer) {
            mDrawerLayout.setDrawerOpen(false, animate = true)
            mDrawerBackAnimator.handleBackInvoked()
        }else {
            super.handleBackInvoked()
        }
    }

    override fun cancelBackProgress() {
        if (mDrawerBackAnimator.isBackProgressStarted()) {
            mDrawerBackAnimator.cancelBackProgress()
            mDrawerLayout.updateOnBackCallbackState()
        }
    }

    fun isBackProgressStarted() = mDrawerBackAnimator.isBackProgressStarted()

}
