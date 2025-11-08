
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
internal open class ToolbarLayoutBackHandler(private val toolbarLayout: ToolbarLayout
): BackHandler {
    override fun startBackProgress(backEvent: BackEventCompat) {}

    override fun updateBackProgress(backEvent: BackEventCompat) {}

    @CallSuper
    override fun handleBackInvoked() {
        if (toolbarLayout.isInEditMode) return

        with(toolbarLayout) {
            when {
                isActionMode -> endActionMode()
                isSearchMode -> {
                    when (searchView!!.searchModeOBPBehavior) {
                        DISMISS -> {
                            searchView?.handleBackInvoked()
                            endSearchMode()
                        }

                        CLEAR_CLOSE -> {
                            searchView?.apply {
                                setQuery("", true);
                                handleBackInvoked()
                            }
                            updateOnBackCallbackState()
                        }

                        CLEAR_DISMISS -> {
                             if (searchView?.query?.isNotEmpty() == true) {
                                searchView?.apply {
                                    setQuery("", true);
                                    handleBackInvoked()
                                }
                            } else {
                                searchView?.handleBackInvoked()
                                endSearchMode()
                            }
                        }
                    }
                }

                else -> Unit
            }
        }
    }

    override fun cancelBackProgress() {}


}
