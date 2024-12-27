
package dev.oneuiproject.oneui.layout.internal.delegate

import androidx.activity.BackEventCompat
import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
import dev.oneuiproject.oneui.ktx.isSoftKeyboardShowing
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
                    if (searchView.isSoftKeyboardShowing) {
                        searchView.clearFocus()
                    } else endActionMode()
                }
                isSearchMode -> {
                    when (searchModeOBPBehavior) {
                        DISMISS -> {
                            if (searchView.isSoftKeyboardShowing) {
                                searchView.clearFocus()
                            } else endSearchMode()
                        }

                        CLEAR_CLOSE -> {
                            if (searchView.isSoftKeyboardShowing) {
                                searchView.clearFocus()
                                //Add delay to account for the keyboard's hiding animation
                                //so we can use the appropriate `isSoftKeyboardShowing` result
                                //in updateObpCallbackState().
                                postDelayed({ updateOnBackCallbackState() }, 400)
                            } else {
                                searchView.setQuery("", true)
                                updateOnBackCallbackState()
                            }
                        }

                        CLEAR_DISMISS -> {
                            if (searchView.isSoftKeyboardShowing) {
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
