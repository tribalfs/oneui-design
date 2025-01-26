
package dev.oneuiproject.oneui.layout.internal.delegate

import androidx.activity.BackEventCompat
import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.CLEAR_CLOSE
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.CLEAR_DISMISS
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.DISMISS
import dev.oneuiproject.oneui.layout.internal.backapi.BackHandler


@RestrictTo(RestrictTo.Scope.LIBRARY)
open class ToolbarLayoutBackHandler(private val mToolbarLayout: ToolbarLayout
): BackHandler {
    override fun startBackProgress(backEvent: BackEventCompat) {}

    override fun updateBackProgress(backEvent: BackEventCompat) {}

    @CallSuper
    override fun handleBackInvoked() {
        if (mToolbarLayout.isInEditMode) return

        with (mToolbarLayout) {
            when {
                isActionMode -> {
                    if (mToolbarLayout.isSofInputShowing) {
                        searchView.clearFocus()
                    } else endActionMode()
                }
                isSearchMode -> {
                    when (searchModeOBPBehavior) {
                        DISMISS -> {
                            if (mToolbarLayout.isSofInputShowing) {
                                searchView.clearFocus()
                            } else endSearchMode()
                        }

                        CLEAR_CLOSE -> {
                            if (mToolbarLayout.isSofInputShowing) {
                                searchView.clearFocus()
                            } else {
                                searchView.setQuery("", true)
                                updateOnBackCallbackState()
                            }
                        }

                        CLEAR_DISMISS -> {
                            if (mToolbarLayout.isSofInputShowing) {
                                searchView.clearFocus()
                            } else if (searchView.query.isNotEmpty()) {
                                searchView.setQuery("", true)
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
