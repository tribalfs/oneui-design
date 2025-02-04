package dev.oneuiproject.oneui.layout.internal.util

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Parcelable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout.LayoutParams
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.updateLayoutParams
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.SeslImmersiveScrollBehavior
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue
import kotlinx.parcelize.Parcelize

@RequiresApi(Build.VERSION_CODES.R)
internal class ImmersiveScrollHelper(
    private val activity: AppCompatActivity,
    private val appBarLayout: AppBarLayout,
    private var bottomView: View?,
    @FloatRange(0.0, 1.0)
    private val bottomViewAlpha: Float = 1f
) {

    private var originalParent: ViewGroup? = null
    private var restoreParentIndex: Int = 0
    private var defaultBottomViewBgColor =
        activity.getThemeAttributeValue(androidx.appcompat.R.attr.roundedCornerColor)!!.data


    var isImmersiveScrollActivated = false
        private set

    @RequiresApi(Build.VERSION_CODES.R)
    fun activateImmersiveScroll(){
        if (isImmersiveScrollActivated) return
        isImmersiveScrollActivated = true
        ensureImmersiveScrollBehavior()
        appBarLayout.seslSetImmersiveScroll(true)
        setupBottomView()
        appBarLayout.apply {
            //Workaround wrong collapsed height
            if (seslIsCollapsed()) setExpanded(false, false)
            requestApplyInsets()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun deactivateImmersiveScroll(){
        if (!isImmersiveScrollActivated) return
        isImmersiveScrollActivated = false
        appBarLayout.seslActivateImmersiveScroll(false, true)
        setupBottomView()
        appBarLayout.apply {
            //Workaround wrong collapsed height
            if (seslIsCollapsed()) setExpanded(false, true)
            requestApplyInsets()
        }
    }

    fun setBottomView(view: View?){
        resetBottomView(); bottomView = view; setupBottomView()
    }


    private fun setupBottomView(){
        if (activity.isDestroyed || activity.isFinishing) {
            return
        }

        if (isImmersiveScrollActivated){
            bottomView?.apply {
                switchFooterParent(true)
                ColorUtils.setAlphaComponent(defaultBottomViewBgColor,
                    // Set 0.01 as min alpha - SeslImmersiveScrollBehavior is glitching with 0f
                    (255 * bottomViewAlpha.coerceAtLeast(0.01f)).toInt()).let {
                    @Suppress("DEPRECATION")
                    activity.window.navigationBarColor = it
                    setBackgroundColor(it)
                }
                appBarLayout.seslSetBottomView(this)
            }
        }else{
            bottomView?.apply {
                switchFooterParent(false)
                resetBottomView()
                appBarLayout.seslSetBottomView(null)
            }
        }
    }

    private fun resetBottomView(){
        bottomView?.apply {
            @Suppress("DEPRECATION")
            activity.window.navigationBarColor = Color.TRANSPARENT
            setBackgroundColor(defaultBottomViewBgColor)
            translationY = 0f
        }
    }


    private fun switchFooterParent(isBottomViewMode: Boolean) {
        bottomView?.clearAnimation()

        if (isBottomViewMode){
            (bottomView?.parent as? ViewGroup)?.let {
                if (it == appBarLayout.parent){
                    //Nothing to do
                    return
                }
                originalParent = it
                restoreParentIndex = it.indexOfChild(bottomView)
                it.removeView(bottomView)
            }

            (appBarLayout.parent as CoordinatorLayout).addView(
                bottomView,
                CoordinatorLayout.LayoutParams(
                    CoordinatorLayout.LayoutParams.MATCH_PARENT,
                    CoordinatorLayout.LayoutParams.WRAP_CONTENT
                )
            )
            bottomView?.apply {
                updateLayoutParams<CoordinatorLayout.LayoutParams> {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                }
                setBackgroundColor(ColorUtils.setAlphaComponent(defaultBottomViewBgColor,
                    if (SDK_INT >= 35) (255*.8).toInt() else 255))
            }
            appBarLayout.parent.requestLayout()
        }else{
            originalParent?.apply {
                (bottomView?.parent as ViewGroup).removeView(bottomView)
                addView(bottomView, restoreParentIndex, LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ))
                bottomView?.apply {
                    updateLayoutParams<LayoutParams> {
                        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    }
                    setBackgroundColor(defaultBottomViewBgColor)
                }
            }
            appBarLayout.parent.requestLayout()
        }
    }

    private fun ensureImmersiveScrollBehavior(): Boolean {
        try {
            appBarLayout.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                if (behavior !is SeslImmersiveScrollBehavior) {
                    behavior = SeslImmersiveScrollBehavior(baseContext, null)
                }
                (behavior as SeslImmersiveScrollBehavior).apply {
                    setNeedToCheckBottomViewMargin(false)
                    setAutoRestoreTopAndBottom(false)
                }
            }
            return true
        }catch(_: Throwable){
            return false
        }
    }

    private val baseContext: Context
        get() = appBarLayout.context.let{ (it as? ContextThemeWrapper)?.baseContext ?: it }


    @Parcelize
    class State(val isImmersiveScrollActivated: Boolean,
                val withFooter: Boolean): Parcelable

}