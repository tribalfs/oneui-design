
package dev.oneuiproject.oneui.layout.internal.delegate

import androidx.activity.BackEventCompat
import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
import dev.oneuiproject.oneui.ktx.hideSoftInput
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.CLEAR_CLOSE
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.CLEAR_DISMISS
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.DISMISS
import dev.oneuiproject.oneui.layout.internal.backapi.BackHandler


@RestrictTo(RestrictTo.Scope.LIBRARY)
open class ToolbarLayoutBackHandler(private val toolbarLayout: ToolbarLayout
): BackHandler {
    override fun startBackProgress(backEvent: BackEventCompat) {}

    override fun updateBackProgress(backEvent: BackEventCompat) {}

    @CallSuper
    override fun handleBackInvoked() {
        if (toolbarLayout.isInEditMode) return

        with (toolbarLayout) {
            when {
                isActionMode -> {
                    if (toolbarLayout.isSofInputShowing) {
                        activity?.hideSoftInput()
                    } else endActionMode()
                }
                isSearchMode -> {
                    when (searchModeOBPBehavior) {
                        DISMISS -> {
                            if (toolbarLayout.isSofInputShowing) {
                                activity?.hideSoftInput()
                            } else endSearchMode()
                        }

                        CLEAR_CLOSE -> {
                            if (toolbarLayout.isSofInputShowing) {
                                activity?.hideSoftInput()
                            } else {
                                searchView?.setQuery("", true)
                                updateOnBackCallbackState()
                            }
                        }

                        CLEAR_DISMISS -> {
                            if (toolbarLayout.isSofInputShowing) {
                                activity?.hideSoftInput()
                            } else if (searchView?.query?.isNotEmpty() == true) {
                                searchView?.setQuery("", true)
                            } else endSearchMode()
                        }
                    }
                }

                else -> Unit
            }
        }
    }

    override fun cancelBackProgress() {}


}
