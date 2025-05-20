package dev.oneuiproject.oneui.navigation.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.accessibility.AccessibilityEvent
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Dimension
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.appcompat.view.menu.MenuView
import androidx.appcompat.widget.TooltipCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import androidx.core.widget.TextViewCompat
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.utils.getRegularFont
import dev.oneuiproject.oneui.utils.getSemiBoldFont
import kotlin.math.min

@SuppressLint("RestrictedApi")
internal class DrawerMenuItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), MenuView.ItemView {

    private var iconSize = context.resources.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_item_icon_size)
    private var needsEmptyIcon = true
    private var offset: Float = 1f

    @JvmField
    var checkable: Boolean = false
    @JvmField
    var isBold: Boolean = true

    private lateinit var titleView: TextView
    private lateinit var iconView: ImageView
    private lateinit var rippleBackgroundView: View
    private var countView: TextView? = null
    private var iconBackgroundView: View? = null
    private var actionArea: DrawerActionViewContainer? = null
    private var toggle: ImageView? = null

    @SuppressLint("RestrictedApi")
    private var itemData: MenuItemImpl? = null

    private var iconTintList: ColorStateList? = null
    private var hasIconTintList = false
    private var emptyDrawable: Drawable? = null

    private var menuItemType: Int = -1
    private var isViewInited = false

    private val accessibilityDelegate: AccessibilityDelegateCompat =
        object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View, info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.isCheckable = checkable
            }
        }

    private fun setCountText(count: String?) {
        showCount(count != null)
        countView?.text = count
    }

    private fun showCount(show: Boolean) {
        if (show) {
            if (countView == null) {
                countView = findViewById<ViewStub>(R.id.drawer_menu_item_count_stub).inflate() as TextView
            } else {
                countView!!.isVisible = true
            }
        }else{
            countView?.isGone = true
        }
    }

    private fun inflateToggle(){
        if (toggle == null){
            toggle = findViewById<ViewStub>(R.id.drawer_menu_expand_button_stub).inflate() as ImageView
        }
    }



    override fun initialize(itemData: MenuItemImpl, menuType: Int) {
        this.menuItemType = menuType

        if (!isViewInited) {
            titleView = findViewById(R.id.drawer_menu_item_title)
            iconView = findViewById(R.id.drawer_menu_item_icon)
            rippleBackgroundView = findViewById(R.id.ripple_background)
            iconBackgroundView = findViewById(R.id.icon_background)

            if (!isInEditMode) {
                ViewCompat.setAccessibilityDelegate(titleView, accessibilityDelegate)
            }

            isViewInited = true
        }

        this.itemData = itemData
        if (itemData.itemId > 0) {
            id = itemData.itemId
        }

        //visibility = if (itemData.isVisible) VISIBLE else GONE

        isEnabled = itemData.isEnabled
        setTitle(itemData.title)
        setIcon(itemData.icon)
        if (menuType != MENU_TYPE_SUBHEADER) {
            setCountText(itemData.badgeText)
            setCheckable(itemData.isCheckable)
            setChecked(itemData.isChecked)

            val actionView = itemData.actionView
            if (actionView != null && actionView !is DrawerCategoryItemView) {
                setActionView(actionView)
            }
            if (menuType == MENU_TYPE_SUBMENU){
                iconView.scaleX = .7f
                iconView.scaleY = .7f
            }
        }else{
            inflateToggle()
        }
        contentDescription = itemData.contentDescription
        TooltipCompat.setTooltipText(this, itemData.tooltipText)
        doOnLayout { updateOffset() }
    }

    fun initialize(itemData: MenuItemImpl, isBold: Boolean) {
        this.isBold = isBold
        initialize(itemData, MENU_TYPE_NORMAL)
    }

    fun recycle() {
        actionArea?.removeAllViews()
    }

    private fun setActionView(actionView: View) {
        if (actionArea == null) {
            actionArea = findViewById<ViewStub>(R.id.nav_drawer_menu_item_action_area_stub).inflate() as DrawerActionViewContainer
        }

        // Make sure to remove the existing parent if the View is reused
        (actionView.parent as? ViewGroup)?.removeView(actionView)

        actionArea!!.apply {
            removeAllViews()
            addView(actionView)
        }
    }

    override fun getItemData(): MenuItemImpl {
        return itemData!!
    }

    override fun setTitle(title: CharSequence?) {
        titleView.apply {
            isVisible = title != null
            text = title
        }
    }

    override fun setCheckable(checkable: Boolean) {
        refreshDrawableState()
        if (this.checkable != checkable) {
            this.checkable = checkable
            accessibilityDelegate.sendAccessibilityEvent(titleView,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            )
        }
    }

    override fun setChecked(checked: Boolean) {
        if (isSelected == checked) return
        refreshDrawableState()
        isSelected = checked
        iconBackgroundView?.let {
            it.isSelected = checked
        }
        titleView.typeface = if (checked && isBold) getSemiBoldFont() else getRegularFont()
    }

    override fun setShortcut(showShortcut: Boolean, shortcutKey: Char) {}

    override fun setIcon(icon: Drawable?) {
        var mIcon = icon
        if (mIcon != null) {
            if (hasIconTintList) {
                val state = mIcon.constantState
                mIcon = DrawableCompat.wrap(state?.newDrawable() ?: mIcon).mutate()
                DrawableCompat.setTintList(mIcon, iconTintList)
            }
            mIcon.setBounds(0, 0, iconSize, iconSize)
        } else if (needsEmptyIcon) {
            if (emptyDrawable == null) {
                emptyDrawable = ResourcesCompat.getDrawable(
                    resources,
                    com.google.android.material.R.drawable.navigation_empty_icon,
                    context.theme
                )
                emptyDrawable?.setBounds(0, 0, iconSize, iconSize)
            }
            mIcon = emptyDrawable
        }
        iconView.setImageDrawable(mIcon)
    }

    fun setIconSize(@Dimension iconSize: Int) {
        this.iconSize = iconSize
    }

    override fun prefersCondensedTitle(): Boolean {
        return false
    }

    override fun showsIcon(): Boolean {
        return true
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (itemData != null && itemData!!.isCheckable && itemData!!.isChecked) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET)
        }
        return drawableState
    }

    fun setIconTintList(tintList: ColorStateList?) {
        iconTintList = tintList
        hasIconTintList = iconTintList != null
        if (itemData != null) {
            setIcon(itemData!!.icon)
        }
    }

    fun setTextAppearance(textAppearance: Int) {
        TextViewCompat.setTextAppearance(titleView, textAppearance)
    }

    fun setTextColor(colors: ColorStateList?) {
        titleView.setTextColor(colors)
    }

    fun setNeedsEmptyIcon(needsEmptyIcon: Boolean) {
        this.needsEmptyIcon = needsEmptyIcon
    }

    fun setHorizontalPadding(padding: Int) {
        setPadding(padding, paddingTop, padding, paddingBottom)
    }

    fun setMaxLines(maxLines: Int) {
        titleView.maxLines = maxLines
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (isAttachedToWindow) updateOffset()
    }

    fun applyOffset(offset: Float) {
        if (this@DrawerMenuItemView.offset == offset) return
        this@DrawerMenuItemView.offset = offset
        if (isAttachedToWindow) updateOffset()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateOffset()
    }

    fun animateToggle(isExpanded: Boolean){
        if (MENU_TYPE_SUBHEADER == menuItemType){
            toggle!!.animate()
                .rotation(if (isExpanded) -180f else 0f)
                .setDuration(140)
                .setInterpolator(AccelerateDecelerateInterpolator())
        }
    }

    private fun updateOffset(skipIcon: Boolean = false) = doOnLayout{
        when(menuItemType){
            MENU_TYPE_NORMAL, MENU_TYPE_SUBHEADER -> {
                if (!skipIcon) {
                    iconView.alpha = if (isEnabled) 0.7f else 0.4f
                }
                val interpolatedAlpha = interpolatedAlpha(0.95f).also{
                    titleView.alpha = it
                    rippleBackgroundView.alpha = it
                }
                iconBackgroundView?.apply {
                    isVisible = offset < .5
                    if (isSelected) {
                        alpha = (1f - min(interpolatedAlpha * 12f, 1f))
                    }
                }
                actionArea?.setOffset(offset)

            }
            MENU_TYPE_CATEGORY -> {
                if (!skipIcon) {
                    iconView.alpha = if (isEnabled) 1f else 0.5f
                }

                val interpolatedAlpha = interpolatedAlpha().also{
                    titleView.alpha = it
                    rippleBackgroundView.alpha = it
                }

                iconBackgroundView?.apply {
                    isVisible = offset == 0f
                    val offsetX = iconView.marginStart - ((width - iconView.width) / 2f) - marginStart
                    iconView.translationX =  (offsetX * (1f - interpolatedAlpha).coerceIn(0f, 1f) * (if (isRtl) 1f else -1f))
                }
                if (isInEditMode) {
                    iconView.translationX = -10f.dpToPx(resources).toFloat()
                }
            }
            MENU_TYPE_SUBMENU -> {
                val interpolatedAlpha = interpolatedAlpha(0.65f)
                iconView.alpha = interpolatedAlpha
                titleView.alpha = interpolatedAlpha
                isVisible = interpolatedAlpha > 0.05f
            }
        }
    }

    private fun interpolatedAlpha(maxAlpha: Float = 1f): Float =
        ((offset - (1 - .95F)) / .95F).coerceIn(0f, 1f).run {
            if (isEnabled) minOf(this, maxAlpha) else minOf(0.5f, this)
        }


    companion object {
        private const val TAG = "NavDrawerMenuItemView"

        private val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)
        const val MENU_TYPE_NORMAL = 0
        const val MENU_TYPE_CATEGORY = 1
        const val MENU_TYPE_SUBHEADER = 2
        const val MENU_TYPE_SUBMENU = 3
    }
}