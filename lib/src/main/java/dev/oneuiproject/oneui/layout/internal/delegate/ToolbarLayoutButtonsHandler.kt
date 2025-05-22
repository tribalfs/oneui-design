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

    private val mActivity by lazy(LazyThreadSafetyMode.NONE) { toolbar.context.appCompatActivity }

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
        updateNavButton()
    }

    override fun setNavigationButtonTooltip(tooltipText: CharSequence?) {
        toolbar.navigationContentDescription = tooltipText
    }

    private val badgeIcon by lazy(LazyThreadSafetyMode.NONE) { NavigationBadgeIcon(toolbar.context) }

    override fun setNavigationButtonBadge(badge: Badge) {
        badgeIcon.setBadge(badge)
        if (badge != Badge.NONE && navigationBadgeIcon == null) {
            Log.w(TAG, "setNavigationButtonBadge: Unable to show badge, no navigation icon has been set.")
        }
    }

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
        updateNavButton()
    }

    override fun setHeaderButtonIcon(icon: Drawable?, tint: Int?) = Unit
    override fun setHeaderButtonTooltip(tooltipText: CharSequence?) = Unit
    override fun setHeaderButtonOnClickListener(listener: View.OnClickListener?)  = Unit
    override fun setHeaderButtonBadge(badge: Badge)= Unit


    private fun updateNavButton() {
        if (showNavigationButtonAsBack) {
            toolbar.apply {
                if (isInEditMode) {
                    navigationIcon = AppCompatResources.getDrawable(toolbar.context,  appcompatR.drawable.sesl_ic_ab_back_light)!!
                }
                //Backup current tooltip to restore later
                //before setting the mBackButtonTooltip as the current one
                navDrawerButtonTooltip = navigationContentDescription
                mActivity?.apply {
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    setNavigationOnClickListener {
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        } else {
            if (showNavigationButton) {
                toolbar.apply {
                    mActivity?.apply {
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

