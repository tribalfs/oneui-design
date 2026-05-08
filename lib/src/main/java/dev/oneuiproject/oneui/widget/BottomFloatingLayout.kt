package dev.oneuiproject.oneui.widget

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.appcompat.animation.SeslAnimationUtils
import androidx.core.content.withStyledAttributes
import androidx.core.view.doOnLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import dev.oneuiproject.oneui.ktx.setListener
import dev.oneuiproject.oneui.layout.internal.util.ToolbarLayoutUtils.InternalLayoutInfo
import dev.oneuiproject.oneui.layout.internal.util.ToolbarLayoutUtils.getLayoutLocationInfo
import dev.oneuiproject.oneui.design.R

/**
 * A custom [LinearLayout] designed to provide a floating container, typically positioned at the
 * bottom of the screen.
 *
 * This layout allows constraining the width of its children via the `maxChildWidth` attribute,
 * which is particularly useful for maintaining readability and consistent UI on wide screens or
 */
class BottomFloatingLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object  {
        private const val TAG = "BottomFloatingLayout"
    }

    private var isShowAnimating = false
    private var maxChildWidth: Int = -1
    private var layoutLocationInfo: InternalLayoutInfo? = null

    init {
        context.withStyledAttributes(attrs, R.styleable.BottomFloatingLayout) {
            maxChildWidth = getDimensionPixelSize(
                R.styleable.BottomFloatingLayout_maxChildWidth,
                -1
            )
        }

        fitsSystemWindows = false
    }
    private val bottomOffsetListener: (Float) -> Unit by lazy(LazyThreadSafetyMode.NONE) {
        { translationY = -it }
    }

    override fun measureChildWithMargins(
        child: View,
        parentWidthMeasureSpec: Int,
        widthUsed: Int,
        parentHeightMeasureSpec: Int,
        heightUsed: Int
    ) {
        if (maxChildWidth <= 0) {
            super.measureChildWithMargins(
                child,
                parentWidthMeasureSpec,
                widthUsed,
                parentHeightMeasureSpec,
                heightUsed
            )
            return
        }

        val lp = child.layoutParams as LayoutParams

        val isHorizontal = orientation == HORIZONTAL
        val hasWeight = lp.weight > 0f

        // Let LinearLayout handle weighted distribution first
        if (isHorizontal && hasWeight) {
            super.measureChildWithMargins(
                child,
                parentWidthMeasureSpec,
                widthUsed,
                parentHeightMeasureSpec,
                heightUsed
            )
            return
        }

        // Check if we need to clamp width before measurement to avoid double measurement
        val childWidthSpec = getChildMeasureSpec(parentWidthMeasureSpec, 
            paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin + widthUsed, lp.width)
        
        val childHeightSpec = getChildMeasureSpec(parentHeightMeasureSpec,
            paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin + heightUsed, lp.height)

        val targetWidth = if (maxChildWidth > 0) {
            val specSize = MeasureSpec.getSize(childWidthSpec)
            val specMode = MeasureSpec.getMode(childWidthSpec)
            
            when {
                specMode == MeasureSpec.AT_MOST && specSize > maxChildWidth -> maxChildWidth
                specMode == MeasureSpec.UNSPECIFIED -> maxChildWidth
                specMode == MeasureSpec.EXACTLY && specSize > maxChildWidth -> maxChildWidth
                else -> specSize
            }
        } else {
            MeasureSpec.getSize(childWidthSpec)
        }

        val finalWidthSpec = MeasureSpec.makeMeasureSpec(targetWidth, MeasureSpec.getMode(childWidthSpec))
        child.measure(finalWidthSpec, childHeightSpec)
    }

    // ---------------- Animations (unchanged) ----------------

    private val slideDownAnim by lazy(LazyThreadSafetyMode.NONE) {
        AnimationUtils.loadAnimation(context, R.anim.oui_des_bottom_tab_slide_down).apply {
            interpolator = SeslAnimationUtils.SINE_IN_OUT_90
            duration = 200L
            setListener(
                onEnd = {
                    if (isShowAnimating) return@setListener
                    this@BottomFloatingLayout.isGone = true
                }
            )
        }
    }

    private val slideUpAnim by lazy(LazyThreadSafetyMode.NONE) {
        AnimationUtils.loadAnimation(context, R.anim.oui_des_bottom_tab_slide_up).apply {
            interpolator = SeslAnimationUtils.SINE_IN_OUT_90
            duration = 200
        }
    }

    fun hide(animate: Boolean) {
        Log.d(TAG, "hide($animate), isVisible: $isVisible")
        isShowAnimating = false
        clearAnimation()
        if (isGone) return
        if (animate) {
            startAnimation(slideDownAnim)
        } else {
            isGone = true
        }
    }

    fun show(animate: Boolean) {
        Log.d(TAG, "show($animate), isVisible: $isVisible")
        isShowAnimating = true
        clearAnimation()
        translationY = 0f
        if (isVisible) return
        isVisible = true
        if (animate) {
            startAnimation(slideUpAnim)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        doOnLayout {
            registerTBLBottomOffsetListener(true)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        registerTBLBottomOffsetListener(false)
    }

    private fun registerTBLBottomOffsetListener(register: Boolean) {
        layoutLocationInfo = layoutLocationInfo ?: getLayoutLocationInfo()
        layoutLocationInfo?.takeIf { it.isInsideTBLMainContainer }?.tblParent?.apply {
            if (register) {
                addOnBottomOffsetChangedListener(bottomOffsetListener)
            } else {
                removeOnBottomOffsetChangedListener(bottomOffsetListener)
                layoutLocationInfo = null
            }
        } ?: run { layoutLocationInfo = null }
    }
}