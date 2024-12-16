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

    private lateinit var mContainerView: LinearLayout
    private lateinit var mTitleTextView: TextView
    private lateinit var mSummaryTextView: TextView
    private var mDividerViewTop: View? = null
    private var mDividerViewBottom: View? = null
    private var mIconImageView: ImageView? = null
    private var badgeFrame: LinearLayout? = null

    private var containerLeftPaddingWithIcon: Int = 0
    private var containerLeftPaddingNoIcon: Int = 0
    private var dividerMarginStart: Int = 0
    private var dividerMarginStartWithIcon: Int = 0

    /**
     *  Show divider on top. True by default
     */
    var showTopDivider: Boolean
        get() = mDividerViewTop?.isVisible == true
        set(value) {
            if (value) ensureTopDivider()
            mDividerViewTop?.isVisible = value
        }

    var showBottomDivider: Boolean
        get() = mDividerViewBottom?.isVisible == true
        set(value) {
            if (value) ensureBottomDivider()
            mDividerViewBottom?.isVisible = value
        }

    private fun ensureTopDivider(){
        if (mDividerViewTop == null){
            mDividerViewTop = LayoutInflater.from(context)
                .inflate(R.layout.oui_widget_card_item_divider, this, false)
            addView(mDividerViewTop, 0)
        }
    }

    private fun ensureBottomDivider(){
        if (mDividerViewBottom == null){
            mDividerViewBottom = LayoutInflater.from(context)
                .inflate(R.layout.oui_widget_card_item_divider, this, false)
            addView(mDividerViewBottom, childCount)
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
        get() = mTitleTextView.text
        set(value) {
            if (mTitleTextView.text == value) return
            mTitleTextView.text = value
        }

    var summary: CharSequence?
        get() = mSummaryTextView.text
        set(value) {
            if (mSummaryTextView.text == value) return
            mSummaryTextView.isVisible = !value.isNullOrEmpty()
            mSummaryTextView.text = value
        }

    var icon: Drawable?
        get() = mIconImageView?.drawable
        set(value) {
            if (value != null) ensureInflatedIconView()
            if (mIconImageView?.drawable == value) return
            mIconImageView!!.setImageDrawable(value)
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
            dividerMarginStartWithIcon = containerLeftPaddingWithIcon + getDimensionPixelSize(R.dimen.cardview_icon_size) +
                    getDimensionPixelSize(R.dimen.cardview_icon_margin_end)
        }

        context.obtainStyledAttributes(attrs, R.styleable.CardItemView).use { a ->
            inflate(context, R.layout.oui_widget_card_item, this)
            mContainerView = findViewById(R.id.cardview_container)

            val iconDrawable = a.getDrawable(R.styleable.CardItemView_icon)

            if ( iconDrawable != null) {
                ensureInflatedIconView()
                mIconImageView!!.setImageDrawable(iconDrawable)
                val iconTint = a.getColor(R.styleable.CardItemView_iconTint, -1)
                if (iconTint != -1) {
                    DrawableCompat.setTint(mIconImageView!!.drawable, iconTint)
                }
            }

            mTitleTextView = findViewById<TextView?>(R.id.cardview_title).apply {
                maxLines = a.getInteger(R.styleable.CardItemView_titleMaxLines, 5)
            }
            title = a.getString(R.styleable.CardItemView_title)

            mSummaryTextView = findViewById<TextView>(R.id.cardview_summary).apply {
                maxLines = a.getInteger(R.styleable.CardItemView_summaryMaxLines, 10)
            }
            summary = a.getString(R.styleable.CardItemView_summary)
            if (a.getBoolean(R.styleable.CardItemView_userUpdatable, false)){
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
                mSummaryTextView.setTextColor(ColorStateList(states, colors))
            }

            showTopDivider = a.getBoolean(R.styleable.CardItemView_showTopDivider, true)
            showBottomDivider = a.getBoolean(R.styleable.CardItemView_showBottomDivider, false)
            isEnabled = a.getBoolean(R.styleable.CardItemView_android_enabled, true)
            fullWidthDivider = a.getBoolean(R.styleable.CardItemView_fullWidthDivider, false)

            updateLayoutParams()
        }
    }

    private fun ensureInflatedIconView(){
        if (mIconImageView == null) {
            mIconImageView = (findViewById<ViewStub>(R.id.viewstub_icon_frame).inflate() as FrameLayout)
                .findViewById(R.id.cardview_icon)
        }
    }

    private fun updateLayoutParams(){
        val hasIcon = mIconImageView?.drawable != null

        val newPaddingLeft = if (hasIcon) containerLeftPaddingWithIcon else containerLeftPaddingNoIcon
        mContainerView.apply {
            if (newPaddingLeft == paddingLeft) return@apply
            updatePadding(left = if (hasIcon) containerLeftPaddingWithIcon else containerLeftPaddingNoIcon)
        }

        val newDividerStartMargin = if (!hasIcon || fullWidthDivider) dividerMarginStart else dividerMarginStartWithIcon
        mDividerViewTop?.apply {
            if (newDividerStartMargin == marginStart) return@apply
            updateLayoutParams<LayoutParams> {
                this.marginStart = newDividerStartMargin
            }
        }
       mDividerViewBottom?.apply {
            if (newDividerStartMargin == marginStart) return@apply
            updateLayoutParams<LayoutParams> {
                this.marginStart = newDividerStartMargin
            }
        }
    }

    fun getIconImageView(): ImageView {
        ensureInflatedIconView()
        return mIconImageView!!
    }

    override fun setEnabled(enabled: Boolean) {
        if (isEnabled == enabled) return
        super.setEnabled(enabled)
        mContainerView.apply {
            isFocusable = enabled
            isClickable = enabled
            alpha = when {
                enabled -> 1.0f
                else -> 0.4f
            }
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        mContainerView.setOnClickListener {
            if (isEnabled) {
                l?.onClick(this)
            }
        }
    }
}