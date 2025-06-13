package dev.oneuiproject.oneui.layout.internal.delegate

import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.Log
import android.view.View
import androidx.annotation.RestrictTo
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.appCompatActivity
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.layout.internal.NavigationBadgeIcon
import dev.oneuiproject.oneui.layout.internal.util.NavButtonsHandler
import androidx.appcompat.R as appcompatR

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal open class ToolbarLayoutButtonsHandler(private val toolbar: Toolbar):
    NavButtonsHandler {
    private var navDrawerButtonTooltip: CharSequence? = null
    private var navigationOnClickList: View.OnClickListener? = null
    private var navigationBadgeIcon: LayerDrawable? = null

    private val activity by lazy(LazyThreadSafetyMode.NONE) { toolbar.context.appCompatActivity }
    private val backButtonDrawable by lazy(LazyThreadSafetyMode.NONE) {
        AppCompatResources.getDrawable(toolbar.context, appcompatR.drawable.sesl_ic_ab_back_light)!!
    }

    /**Note: This doesn't check for the current value*/
    override var showNavigationButtonAsBack = false
        set(value) {
            field = value
            updateNavButton()
        }

    /**Note: This doesn't check for the current value*/
    override var showNavigationButton = false
        set(value) {
            field = value
            updateNavButton()
        }

    override fun setNavigationButtonOnClickListener(listener: View.OnClickListener?) {
        navigationOnClickList = listener
    }

    /**
     * This applies only when [showNavigationButtonAsBack] is `false` (default)
     * and [showNavigationButton] is true
     */
    override fun setNavigationButtonTooltip(tooltipText: CharSequence?) {
        toolbar.navigationContentDescription = tooltipText
        navDrawerButtonTooltip = tooltipText
    }

    private val badgeIcon by lazy(LazyThreadSafetyMode.NONE) { NavigationBadgeIcon(toolbar.context) }

    override fun setNavigationButtonBadge(badge: Badge) {
        badgeIcon.setBadge(badge)
        if (badge != Badge.NONE && navigationBadgeIcon == null) {
            Log.w(TAG, "setNavigationButtonBadge: Unable to show badge, no navigation icon has been set.")
        }
    }

    /**
     * Set the icon on the navigation button.
     * This applies only when [showNavigationButtonAsBack] is `false` (default).
     */
    override fun setNavigationButtonIcon(icon: Drawable?) {
        if (icon != null) {
            if (navigationBadgeIcon == null) {
                navigationBadgeIcon = LayerDrawable(arrayOf(icon, badgeIcon)).apply {
                    setId(0, R.id.nav_button_icon_layer_id)
                }
            }else {
                navigationBadgeIcon!!.setDrawableByLayerId(R.id.nav_button_icon_layer_id, icon)
            }
        }else {
            navigationBadgeIcon = null
        }

        if (!showNavigationButtonAsBack && showNavigationButton) {
            toolbar.apply {
                activity?.apply {
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                    setNavigationOnClickListener {
                        navigationOnClickList?.onClick(it)
                    }
                    navigationContentDescription = navDrawerButtonTooltip
                }

                if (navigationIcon != navigationBadgeIcon) {
                    navigationIcon = navigationBadgeIcon
                }
            }
        }
    }

    override fun setHeaderButtonIcon(icon: Drawable?, tint: Int?) = Unit
    override fun setHeaderButtonTooltip(tooltipText: CharSequence?) = Unit
    override fun setHeaderButtonOnClickListener(listener: View.OnClickListener?)  = Unit
    override fun setHeaderButtonBadge(badge: Badge)= Unit


    private fun updateNavButton() {
        if (showNavigationButtonAsBack) {
            toolbar.apply {
                navigationIcon = backButtonDrawable
                navigationContentDescription = context.getString(appcompatR.string.sesl_action_bar_up_description)
                activity?.apply {
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    setNavigationOnClickListener {
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        } else {
            if (showNavigationButton) {
                toolbar.apply {
                    activity?.apply {
                        supportActionBar?.setDisplayHomeAsUpEnabled(false)
                        setNavigationOnClickListener {
                            navigationOnClickList?.onClick(it)
                        }
                        navigationContentDescription = navDrawerButtonTooltip
                    }

                    if (navigationIcon != navigationBadgeIcon) {
                        navigationIcon = navigationBadgeIcon
                    }
                }
            }else {
                toolbar.apply {
                    navigationIcon = null
                    navigationContentDescription = null
                    setNavigationOnClickListener(null)
                }
            }
        }
    }

    companion object{
        private const val TAG = "TBLButtonsHandler"
    }
}

