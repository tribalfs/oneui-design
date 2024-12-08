package dev.oneuiproject.oneui.layout.internal.delegate

import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.Log
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import dev.oneuiproject.oneui.ktx.appCompatActivity
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.layout.internal.NavigationBadgeIcon
import dev.oneuiproject.oneui.layout.internal.util.NavButtonsHandler
import androidx.appcompat.R as appcompatR


internal open class ToolbarLayoutButtonsHandler(private val toolbar: Toolbar):
    NavButtonsHandler {
    private var mNavDrawerButtonTooltip: CharSequence? = null
    private var mNavigationIcon: Drawable? = null
    private var mNavigationOnClickList: View.OnClickListener? = null
    private var mNavigationBadgeIcon: LayerDrawable? = null

    private val mActivity by lazy(LazyThreadSafetyMode.NONE) { toolbar.context.appCompatActivity }

    override var showNavigationButtonAsBack = false
        set(value) {
            if (value == field) return
            field = value
            updateNavButton()
        }

    override var showNavigationButton = false
        set(value) {
            if (value == field) return
            field = value
            updateNavButton()
        }

    override fun setNavigationButtonOnClickListener(listener: View.OnClickListener?) {
        mNavigationOnClickList = listener
        updateNavButton()
    }

    override fun setNavigationButtonTooltip(tooltipText: CharSequence?) {
        toolbar.navigationContentDescription = tooltipText
    }

    override fun setNavigationButtonBadge(badge: Badge) {
        if (mNavigationIcon != null) {
            when (badge) {
                is Badge.DOT, is Badge.NUMERIC -> {
                    val badgeIcon = NavigationBadgeIcon(toolbar.context)
                    mNavigationBadgeIcon = LayerDrawable(arrayOf(mNavigationIcon!!, badgeIcon))
                    badgeIcon.setBadge(badge)
                }
                is Badge.NONE -> {
                    mNavigationBadgeIcon = null
                }
            }
            updateNavButton()
        } else Log.d(
            TAG, "setNavigationButtonBadge: no navigation icon" +
                    " has been set"
        )
    }

    override fun setNavigationButtonIcon(icon: Drawable?) {
        mNavigationIcon = icon
        if (mNavigationBadgeIcon != null) {
            mNavigationBadgeIcon!!.apply {
                setDrawable(0, mNavigationIcon)
                mNavigationBadgeIcon!!.invalidateSelf()
            }
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
                mNavDrawerButtonTooltip = navigationContentDescription
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
                    val navIconToSet = mNavigationBadgeIcon ?: mNavigationIcon
                    if (navigationIcon != navIconToSet) {
                        navigationIcon = navIconToSet
                    }
                    mActivity?.apply {
                        supportActionBar?.setDisplayHomeAsUpEnabled(false)
                        setNavigationOnClickListener {
                            mNavigationOnClickList?.onClick(it)
                        }
                        navigationContentDescription = mNavDrawerButtonTooltip
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
        private const val TAG = "ToolbarLayoutNabButtonsBehavior"
    }
}

