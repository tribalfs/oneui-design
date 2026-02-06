package dev.oneuiproject.oneui.layout.internal.util

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.Gravity.BOTTOM
import android.view.Gravity.CENTER_HORIZONTAL
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appcompat.view.ContextThemeWrapper
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingChild3
import androidx.core.view.ViewCompat
import androidx.core.view.ViewCompat.SCROLL_AXIS_VERTICAL
import androidx.core.view.ViewCompat.TYPE_TOUCH
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.SeslImmersiveScrollBehavior
import dev.oneuiproject.oneui.ktx.findAncestorOfType
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue
import dev.oneuiproject.oneui.ktx.withAlpha
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.widget.AdaptiveCoordinatorLayout
import kotlin.math.max

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

    @get:ColorInt
    private val defaultBottomViewBgColor by lazy(LazyThreadSafetyMode.NONE) {
        activity.getThemeAttributeValue(androidx.appcompat.R.attr.roundedCornerColor)!!.data
    }

    private var dummyBottomView: FrameLayout? = null
    private var restoreParentIndex: Int = 0
    private var restoreParent: ViewGroup? = null
    private var restoreLayoutParams: ViewGroup.LayoutParams? = null
    private var bottomInset = 0

    private val insetAnimationCallback by lazy(LazyThreadSafetyMode.NONE) {
        object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {

            private fun WindowInsetsAnimationCompat.isImeAnimation() =
                (typeMask and WindowInsetsCompat.Type.ime()) != 0

            override fun onProgress(
                insets: WindowInsetsCompat,
                runningAnimations: MutableList<WindowInsetsAnimationCompat>
            ): WindowInsetsCompat {
                bottomInset = insets.bottomInset()
                if ((bottomView?.height ?: 0) > 0) {
                    dummyBottomView?.updatePadding(bottom = bottomInset)
                }
                return insets
            }

            override fun onEnd(animation: WindowInsetsAnimationCompat) {
                val rootInsets = ViewCompat.getRootWindowInsets(appBarLayout) ?: return
                dummyBottomView?.updatePadding(bottom = rootInsets.bottomInset())

                if (animation.isImeAnimation()) {
                    val imeBottom = rootInsets.imeBottomInset()
                    if (imeBottom == 0) {
                        appBarLayout.immersiveScrollBehavior?.apply {
                            cancelWindowInsetsAnimationController()
                            dispatchImmersiveScrollEnabled()
                        }
                    }
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.R)
    fun activateImmersiveScroll() {
        if (isImmersiveScrollActivated || activity.isDestroyed || activity.isFinishing) return
        isImmersiveScrollActivated = true
        appBarLayout.rootView.requestApplyInsets()
        appBarLayout.ensureImmersiveScrollBehavior()
        appBarLayout.seslSetImmersiveScroll(true)
        updateDummyBottomView()
        setupBottomView()
        dummyBottomView?.let {
            ViewCompat.setWindowInsetsAnimationCallback(it, insetAnimationCallback)
        }
        appBarLayout.doOnLayout {
            simulateNestedScrollDispatch()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun deactivateImmersiveScroll() {
        if (!isImmersiveScrollActivated || activity.isDestroyed || activity.isFinishing) return
        isImmersiveScrollActivated = false
        appBarLayout.seslActivateImmersiveScroll(false, true)
        appBarLayout.seslImmersiveRelease()
        dummyBottomView?.let {
            ViewCompat.setWindowInsetsAnimationCallback(it, null)
        }
        clearDummyBottomView()
        resetCurrentBottomView()
        appBarLayout.rootView.requestApplyInsets()
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
            restoreParent?.apply {
                val restoreTransition = layoutTransition
                removeView(bottomView)
                layoutTransition = restoreTransition
            }
            dummyBottomView?.apply {
                addView(bottomView)
                setBackgroundColor(defaultBottomViewBgColor.withAlpha(bottomViewAlpha))
                val rootInsets = ViewCompat.getRootWindowInsets(this)
                val bottom = rootInsets?.bottomInset() ?: bottomInset
                updatePadding(bottom = bottom)
            }
            bottomView.updateLayoutParams<FrameLayout.LayoutParams> {
                gravity = BOTTOM or CENTER_HORIZONTAL
            }
        } else {
            dummyBottomView?.apply {
                removeAllViews()
                setBackgroundColor(Color.TRANSPARENT)
                val rootInsets = ViewCompat.getRootWindowInsets(this)
                val bottom = rootInsets?.bottomInset() ?: bottomInset
                updatePadding(bottom = bottom)
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

    private fun AppBarLayout.ensureImmersiveScrollBehavior() {
        updateLayoutParams<CoordinatorLayout.LayoutParams> {
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

    private fun WindowInsetsCompat.bottomInset(): Int {
        val nav = getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        val ime = getInsets(WindowInsetsCompat.Type.ime()).bottom
        return max(nav, ime)
    }

    private fun WindowInsetsCompat.imeBottomInset(): Int =
        getInsets(WindowInsetsCompat.Type.ime()).bottom

    // Workaround for incorrect initial collapse height of appbar
    // when layout contains a nested scrolling child.
    private fun simulateNestedScrollDispatch() {
        val container = appBarLayout.findAncestorOfType<ToolbarLayout>()?.mainContainer
        val scrollingChild = container?.findFirstNestedScrollingChild() as? NestedScrollingChild3 ?: return
        val consumed = IntArray(2)
        val offsetInWindow = IntArray(2)
        scrollingChild.apply {
            startNestedScroll(SCROLL_AXIS_VERTICAL, TYPE_TOUCH)
            dispatchNestedPreScroll(0, -1, consumed, offsetInWindow, TYPE_TOUCH)
            dispatchNestedPreScroll(0, 1, consumed, offsetInWindow, TYPE_TOUCH)
            stopNestedScroll(TYPE_TOUCH)
        }
    }

    private fun View.findFirstNestedScrollingChild(): NestedScrollingChild? {
        if (this is NestedScrollingChild)
            return this
        if (this is ViewGroup) {
            for (i in 0 until childCount) {
                val child = getChildAt(i) ?: continue
                val found = child.findFirstNestedScrollingChild()
                if (found != null) return found
            }
        }
        return null
    }


    private val AppBarLayout.immersiveScrollBehavior get() =
        (layoutParams as? CoordinatorLayout.LayoutParams)?.behavior?.let { it as? SeslImmersiveScrollBehavior }


    private val baseContext: Context
        get() = appBarLayout.context.let { (it as? ContextThemeWrapper)?.baseContext ?: it }
}