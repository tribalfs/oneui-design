
package dev.oneuiproject.oneui.layout.internal.delegate

import androidx.activity.BackEventCompat
import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
import dev.oneuiproject.oneui.layout.DrawerLayout
import dev.oneuiproject.oneui.layout.internal.backapi.BackAnimator


@RestrictTo(RestrictTo.Scope.LIBRARY)
open class DrawerLayoutBackHandler<T: DrawerLayout>(
    private val drawerLayout: T,
    private val drawerBackAnimator: BackAnimator
): ToolbarLayoutBackHandler(drawerLayout) {

    override fun startBackProgress(backEvent: BackEventCompat) {
        if (drawerLayout.shouldAnimateDrawer) {
            drawerBackAnimator.startBackProgress(backEvent)
        }
    }

    override fun updateBackProgress(backEvent: BackEventCompat) {
        if (drawerBackAnimator.isBackProgressStarted()) {
            drawerBackAnimator.updateBackProgress(backEvent)
        }else if (drawerLayout.shouldAnimateDrawer) {
            drawerBackAnimator.startBackProgress(backEvent)
        }
    }

    @CallSuper
    override fun handleBackInvoked() {
        if (drawerBackAnimator.isBackProgressStarted() || drawerLayout.shouldCloseDrawer) {
            drawerLayout.setDrawerOpen(false, animate = true)
            drawerBackAnimator.handleBackInvoked()
        }else {
            super.handleBackInvoked()
        }
    }

    override fun cancelBackProgress() {
        if (drawerBackAnimator.isBackProgressStarted()) {
            drawerBackAnimator.cancelBackProgress()
            drawerLayout.updateOnBackCallbackState()
        }
    }

    fun isBackProgressStarted() = drawerBackAnimator.isBackProgressStarted()

}
