@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewStub
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue

class CardItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private lateinit var containerView: LinearLayout
    private lateinit var titleTextView: TextView
    private lateinit var summaryTextView: TextView
    private var dividerViewTop: View? = null
    private var dividerViewBottom: View? = null
    private var iconImageView: ImageView? = null
    private var badgeFrame: LinearLayout? = null

    private var containerLeftPaddingWithIcon: Int = 0
    private var containerLeftPaddingNoIcon: Int = 0
    private var dividerMarginStart: Int = 0
    private var dividerMarginStartWithIcon: Int = 0

    /**
     *  Show divider on top. True by default
     */
    var showTopDivider: Boolean
        get() = dividerViewTop?.isVisible == true
        set(value) {
            if (value) ensureTopDivider()
            dividerViewTop?.isVisible = value
        }

    /**
     *  Show divider at the bottom. False by default
     */
    var showBottomDivider: Boolean
        get() = dividerViewBottom?.isVisible == true
        set(value) {
            if (value) ensureBottomDivider()
            dividerViewBottom?.isVisible = value
        }

    private fun ensureTopDivider(){
        if (dividerViewTop == null){
            dividerViewTop = LayoutInflater.from(context)
                .inflate(R.layout.oui_des_widget_card_item_divider, this, false)
            addView(dividerViewTop, 0)
        }
    }

    private fun ensureBottomDivider(){
        if (dividerViewBottom == null){
            dividerViewBottom = LayoutInflater.from(context)
                .inflate(R.layout.oui_des_widget_card_item_divider, this, false)
            addView(dividerViewBottom, childCount)
        }
    }

    /**
     * Show full divider width even when there's an icon.
     */
    var fullWidthDivider: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            updateLayoutParams()
        }

    var title: CharSequence?
        get() = titleTextView.text
        set(value) {
            if (titleTextView.text == value) return
            titleTextView.text = value
        }

    var summary: CharSequence?
        get() = summaryTextView.text
        set(value) {
            if (summaryTextView.text == value) return
            summaryTextView.isVisible = !value.isNullOrEmpty()
            summaryTextView.text = value
        }

    var icon: Drawable?
        get() = iconImageView?.drawable
        set(value) {
            if (value != null) ensureInflatedIconView()
            if (iconImageView?.drawable == value) return
            iconImageView!!.setImageDrawable(value)
            updateLayoutParams()
        }

    var showBadge: Boolean
        get() = badgeFrame?.isVisible == true
        set(value) {
            if (value) {
                (badgeFrame
                    ?: (findViewById<ViewStub>(R.id.viewstub_badge_frame).inflate() as LinearLayout).also { badgeFrame = it }
                ).isVisible = true
            } else {
                badgeFrame?.isVisible = false
            }
        }

    init {
        orientation = VERTICAL
        val resources = context.resources
        context.getThemeAttributeValue(android.R.attr.listPreferredItemPaddingStart)!!.run {
            resources.getDimensionPixelSize(resourceId)
        }.let {
            containerLeftPaddingNoIcon = it
            containerLeftPaddingWithIcon = it - 4
        }

        resources.apply {
            dividerMarginStart = containerLeftPaddingNoIcon
            dividerMarginStartWithIcon = containerLeftPaddingWithIcon + getDimensionPixelSize(R.dimen.oui_des_cardview_icon_size) +
                    getDimensionPixelSize(R.dimen.oui_des_cardview_icon_margin_end)
        }

        context.obtainStyledAttributes(attrs, R.styleable.CardItemView).use { a ->
            inflate(context, R.layout.oui_des_widget_card_item, this)
            containerView = findViewById(R.id.cardview_container)

            val iconDrawable = a.getDrawable(R.styleable.CardItemView_icon)

            if ( iconDrawable != null) {
                ensureInflatedIconView()
                iconImageView!!.setImageDrawable(iconDrawable)
                val iconTint = a.getColor(R.styleable.CardItemView_iconTint, -1)
                if (iconTint != -1) {
                    DrawableCompat.setTint(iconImageView!!.drawable, iconTint)
                }
            }

            titleTextView = findViewById<TextView?>(R.id.cardview_title).apply {
                maxLines = a.getInteger(R.styleable.CardItemView_titleMaxLines, 5)
            }
            title = a.getString(R.styleable.CardItemView_title)

            summaryTextView = findViewById<TextView>(R.id.cardview_summary).apply {
                maxLines = a.getInteger(R.styleable.CardItemView_summaryMaxLines, 10)
            }
            summary = a.getString(R.styleable.CardItemView_summary)
            if (a.getBoolean(R.styleable.CardItemView_userUpdatableSummary, false)){
                val colorEnabled = ContextCompat.getColor(context,
                    context.getThemeAttributeValue(androidx.appcompat.R.attr.colorPrimaryDark)!!.resourceId)
                val states = arrayOf(
                    intArrayOf(android.R.attr.state_enabled),
                    intArrayOf(-android.R.attr.state_enabled)
                )
                val colors = intArrayOf(
                    colorEnabled,
                    ColorUtils.setAlphaComponent(colorEnabled, (255 * 0.4).toInt())
                )
                summaryTextView.setTextColor(ColorStateList(states, colors))
            }

            showTopDivider = a.getBoolean(R.styleable.CardItemView_showTopDivider, true)
            showBottomDivider = a.getBoolean(R.styleable.CardItemView_showBottomDivider, false)
            isEnabled = a.getBoolean(R.styleable.CardItemView_android_enabled, true)
            fullWidthDivider = a.getBoolean(R.styleable.CardItemView_fullWidthDivider, false)

            updateLayoutParams()
        }
    }

    private fun ensureInflatedIconView(){
        if (iconImageView == null) {
            iconImageView = (findViewById<ViewStub>(R.id.viewstub_icon_frame).inflate() as FrameLayout)
                .findViewById(R.id.cardview_icon)
        }
    }

    private fun updateLayoutParams(){
        val hasIcon = iconImageView?.drawable != null

        val newPaddingLeft = if (hasIcon) containerLeftPaddingWithIcon else containerLeftPaddingNoIcon
        containerView.apply {
            if (newPaddingLeft == paddingLeft) return@apply
            updatePadding(left = if (hasIcon) containerLeftPaddingWithIcon else containerLeftPaddingNoIcon)
        }

        val newDividerStartMargin = if (!hasIcon || fullWidthDivider) dividerMarginStart else dividerMarginStartWithIcon
        dividerViewTop?.apply {
            if (newDividerStartMargin == marginStart) return@apply
            updateLayoutParams<LayoutParams> {
                this.marginStart = newDividerStartMargin
            }
        }
       dividerViewBottom?.apply {
            if (newDividerStartMargin == marginStart) return@apply
            updateLayoutParams<LayoutParams> {
                this.marginStart = newDividerStartMargin
            }
        }
    }

    fun getIconImageView(): ImageView {
        ensureInflatedIconView()
        return iconImageView!!
    }

    override fun setEnabled(enabled: Boolean) {
        if (isEnabled == enabled) return
        super.setEnabled(enabled)
        containerView.apply {
            isFocusable = enabled
            isClickable = enabled
            alpha = when {
                enabled -> 1.0f
                else -> 0.4f
            }
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        containerView.setOnClickListener {
            if (isEnabled) {
                l?.onClick(this)
            }
        }
    }
}