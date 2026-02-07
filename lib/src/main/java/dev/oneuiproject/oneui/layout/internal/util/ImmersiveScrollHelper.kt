package dev.oneuiproject.oneui.layout.internal.util

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Parcelable
import android.view.Gravity.BOTTOM
import android.view.Gravity.CENTER_HORIZONTAL
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appcompat.view.ContextThemeWrapper
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.SeslImmersiveScrollBehavior
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue
import dev.oneuiproject.oneui.widget.AdaptiveCoordinatorLayout
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.R)
internal class ImmersiveScrollHelper(
    private val activity: ComponentActivity,
    private val appBarLayout: AppBarLayout,
    private var bottomView: View?,
    @field:FloatRange(0.0, 1.0)
    private val bottomViewAlpha: Float = 1f
) {

    var isImmersiveScrollActivated = false
        private set

    private var dummyBottomView: FrameLayout? = null

    private val defaultBottomViewBgColor by lazy(LazyThreadSafetyMode.NONE) {
        activity.getThemeAttributeValue(androidx.appcompat.R.attr.roundedCornerColor)!!.data
    }

    private var restoreParentIndex: Int = 0
    private var restoreParent: ViewGroup? = null
    private var restoreLayoutParams: ViewGroup.LayoutParams? = null

    @RequiresApi(Build.VERSION_CODES.R)
    fun activateImmersiveScroll() {
        if (isImmersiveScrollActivated) return
        isImmersiveScrollActivated = true
        ensureImmersiveScrollBehavior()
        appBarLayout.seslSetImmersiveScroll(true)
        updateDummyBottomView()
        setupBottomView()
        appBarLayout.apply {
            //Workaround wrong collapsed height
            if (seslIsCollapsed()) setExpanded(false, false)
            requestApplyInsets()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun deactivateImmersiveScroll() {
        if (!isImmersiveScrollActivated || activity.isDestroyed || activity.isFinishing) return
        isImmersiveScrollActivated = false
        appBarLayout.seslActivateImmersiveScroll(false, true)
        appBarLayout.seslImmersiveRelease()
        clearDummyBottomView()
        resetCurrentBottomView()
        appBarLayout.apply {
            if (seslIsCollapsed()) setExpanded(false, true)
        }
    }

    fun setBottomView(view: View?) {
        if (view === bottomView) return
        resetCurrentBottomView()
        bottomView = view
        if (isImmersiveScrollActivated) {
            updateDummyBottomView()
            setupBottomView()
        }
    }

    private fun updateDummyBottomView() {
        if (dummyBottomView == null) {
            dummyBottomView = createDummyBottomView()
        }

        dummyBottomView?.apply {
            if (parent == null) {
                appBarLayout.let {
                    (it.parent as AdaptiveCoordinatorLayout).addView(
                        this,
                        CoordinatorLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    )
                }
                (layoutParams as? CoordinatorLayout.LayoutParams)?.gravity =
                    BOTTOM or CENTER_HORIZONTAL
            }

            setOnApplyWindowInsetsListener { v, insets ->
                updatePadding(bottom = insets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()).bottom)
                insets
            }
            requestApplyInsets()
            appBarLayout.seslSetBottomView(this)
        }
    }

    private fun clearDummyBottomView() {
        dummyBottomView?.apply {
            (parent as? ViewGroup)?.removeView(this)
            dummyBottomView = null
            appBarLayout.seslSetBottomView(null)
            appBarLayout.requestLayout()
        }
    }

    private fun setupBottomView() {
        if (bottomView != null) {
            val bottomView = bottomView!!
            restoreParent = bottomView.parent as? ViewGroup
            restoreLayoutParams = bottomView.layoutParams
            restoreParentIndex = restoreParent!!.indexOfChild(bottomView)
            restoreParent?.removeView(bottomView)
            dummyBottomView?.apply {
                addView(bottomView)
                setBackgroundColor(ColorUtils.setAlphaComponent(defaultBottomViewBgColor, (255 * bottomViewAlpha).toInt()))
            }
            bottomView.updateLayoutParams<FrameLayout.LayoutParams> {
                gravity = BOTTOM or CENTER_HORIZONTAL
            }
        } else {
            dummyBottomView?.apply {
                removeAllViews()
                setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    private fun resetCurrentBottomView() {
        bottomView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            restoreParent?.apply {
                it.layoutParams = restoreLayoutParams
                addView(it, restoreParentIndex.coerceAtMost(childCount))
            }
        }
    }

    private fun ensureImmersiveScrollBehavior() {
        appBarLayout.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            if (behavior !is SeslImmersiveScrollBehavior) {
                behavior = SeslImmersiveScrollBehavior(baseContext, null)
            }
            (behavior as SeslImmersiveScrollBehavior).apply {
                setNeedToCheckBottomViewMargin(false)
                setAutoRestoreTopAndBottom(false)
            }
        }
    }

    private fun createDummyBottomView(): FrameLayout =
        FrameLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

    private val baseContext: Context
        get() = appBarLayout.context.let { (it as? ContextThemeWrapper)?.baseContext ?: it }

    @Parcelize
    class State(
        val isImmersiveScrollActivated: Boolean,
        val withFooter: Boolean
    ) : Parcelable

}