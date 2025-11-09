package dev.oneuiproject.oneui.layout.internal.delegate

import androidx.activity.BackEventCompat
import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.CLEAR_CLOSE
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.CLEAR_DISMISS
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.DISMISS
import dev.oneuiproject.oneui.layout.internal.backapi.BackHandler
import dev.oneuiproject.oneui.layout.internal.widget.SemToolbar


@RestrictTo(RestrictTo.Scope.LIBRARY)
internal open class ToolbarLayoutBackHandler(
    private val toolbarLayout: ToolbarLayout
) : BackHandler {

    var predictiveBackEnabled = true

    private val semToolbar = toolbarLayout.toolbar as SemToolbar

    @CallSuper
    override fun startBackProgress(backEvent: BackEventCompat) {
        if (!predictiveBackEnabled) return
        with(toolbarLayout) {
            if (isSearchMode && !isActionMode) {
                searchView?.startBackProgress(backEvent)
                semToolbar.startBackProgress(backEvent)
            }
        }
    }

    @CallSuper
    override fun updateBackProgress(backEvent: BackEventCompat) {
        toolbarLayout.searchView?.updateBackProgress(backEvent)
        semToolbar.updateBackProgress(backEvent)
    }


    @CallSuper
    override fun cancelBackProgress() {
        toolbarLayout.searchView?.cancelBackProgress()
        semToolbar.cancelBackProgress()
    }


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
                            semToolbar.handleBackInvoked()
                            endSearchMode()
                        }

                        CLEAR_CLOSE -> {
                            searchView?.apply {
                                setQuery("", true)
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
                                 semToolbar.handleBackInvoked()
                                 endSearchMode()
                            }
                        }
                    }
                }

                else -> Unit
            }
        }
    }
}
