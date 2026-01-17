@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewStub
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import androidx.core.view.updateLayoutParams
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.defaultSummaryColor
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue
import dev.oneuiproject.oneui.ktx.userUpdatableSummaryColor
import dev.oneuiproject.oneui.utils.SemTouchFeedbackAnimator

/**
 * A custom view that displays a card item with a title, summary, icon, and dividers.
 * It is designed to be used as a row item in a list or similar container.
 *
 * The SwitchItemView supports the following custom attributes among others:
 * - `app:title`: The main text displayed in the view.
 * - `app:summary`: The optional summary text displayed in the view.
 * - `app:showTopDivider`: Whether to display a divider line above the view.
 * - `app:showBottomDivider`: Whether to display a divider line below the view.
 * - `app:icon`: The optional drawable displayed at the start of the view.
 * - `app:iconTint`: The tint color to be applied to icon.
 * - `app:drawableEnd`: The optional drawable displayed at the end of the view.
 *
 * ## Example usage:
 * ```xml
 * <dev.oneuiproject.oneui.widget.CardItemView
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:title="Card Title"
 *     app:summary="Card summary text."
 *     app:icon="@drawable/ic_some_icon"
 *     app:showTopDivider="true"
 *     app:showBottomDivider="true" />
 * ```
 *
 * @param context The Context the view is running in, through which it can access the
 * current theme, resources, etc.
 * @param attrs (Optional) The attributes of the XML tag that is inflating the view.
 */
class CardItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private var containerView: FrameLayout
    private var titleTextView: TextView
    private var summaryTextView: TextView? = null
    private var dividerViewTop: View? = null
    private var dividerViewBottom: View? = null
    private var iconImageView: ImageView? = null
    private var endImageView: ImageView? = null
    private var badgeFrame: LinearLayout? = null

    private var containerLeftPaddingWithIcon: Int = 0
    private var containerLeftPaddingNoIcon: Int = 0
    private var dividerMarginStart: Int = 0
    private var dividerMarginStartWithIcon: Int = 0
    private var summaryMaxLines = 10

    private var suspendLayoutUpdates = false

    @RequiresApi(29)
    private lateinit var semTouchFeedbackAnimator: SemTouchFeedbackAnimator

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
            updateLayoutParams()
        }
    }

    private fun ensureBottomDivider(){
        if (dividerViewBottom == null){
            dividerViewBottom = LayoutInflater.from(context)
                .inflate(R.layout.oui_des_widget_card_item_divider, this, false)
            addView(dividerViewBottom, childCount)
            updateLayoutParams()
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

    /**
     * The title of the card item.
     */
    var title: CharSequence?
        get() = titleTextView.text
        set(value) {
            if (titleTextView.text == value) return
            titleTextView.text = value
        }

    /**
     * The summary for the item.
     * This will be shown below the title.
     */
    var summary: CharSequence?
        get() = summaryTextView?.text
        set(value) {
            val showSummary = !value.isNullOrEmpty()
            if (showSummary) ensureInflatedSummaryView()
            summaryTextView?.apply {
                if (text == value) return
                isVisible = showSummary
                text = value
            }
        }

    /**
     * Set whether the summary text can be updated by the user.
     * When set to true, the summary text color will change to indicate it's user-updatable.
     * The default value is false.
     *
     * @see R.attr.userUpdatableSummary
     */
    var isSummaryUserUpdatable: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) {
                summaryTextView?.setTextColor(context.userUpdatableSummaryColor)
            } else {
                summaryTextView?.setTextColor(context.defaultSummaryColor)
            }
        }

    /** The icon to be displayed in the card item view. */
    var icon: Drawable?
        get() = iconImageView?.drawable
        set(value) {
            if (value != null) ensureInflatedIconView()
            if (iconImageView?.drawable == value) return
            iconImageView!!.setImageDrawable(value)
            updateLayoutParams()
        }

    /** Whether to show a badge on this card item. */
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

        dividerMarginStart = containerLeftPaddingNoIcon
        dividerMarginStartWithIcon = containerLeftPaddingWithIcon + resources.getDimensionPixelSize(R.dimen.oui_des_cardview_icon_size) +
                resources.getDimensionPixelSize(R.dimen.oui_des_cardview_icon_margin_end)

        inflate(context, R.layout.oui_des_widget_card_item, this)
        containerView = findViewById(R.id.cardview_container)
        titleTextView = findViewById<TextView>(R.id.cardview_title)

        suspendLayoutUpdates = true
        attrs?.let { parseAttributes(it) }
        suspendLayoutUpdates = false
        updateLayoutParams()

        if (Build.VERSION.SDK_INT >= 29) {
            semTouchFeedbackAnimator = SemTouchFeedbackAnimator(containerView)
        }
    }


    private fun parseAttributes(attrs: AttributeSet) {
        context.withStyledAttributes(attrs, R.styleable.CardItemView) {
            title = getString(R.styleable.CardItemView_title)
            titleTextView.maxLines = getInteger(R.styleable.CardItemView_titleMaxLines, 5)

            val iconDrawable = getDrawable(R.styleable.CardItemView_icon)
            if (iconDrawable != null) {
                icon = iconDrawable
                val iconTint = getColor(R.styleable.CardItemView_iconTint, -1)
                if (iconTint != -1) {
                    DrawableCompat.setTint(iconImageView!!.drawable, iconTint)
                }
            }
            summaryMaxLines = getInteger(R.styleable.CardItemView_summaryMaxLines, 10)
            if (getBoolean(R.styleable.CardItemView_userUpdatableSummary, false)){
                isSummaryUserUpdatable = true
            }
            summary = getString(R.styleable.CardItemView_summary)
            showTopDivider = getBoolean(R.styleable.CardItemView_showTopDivider, true)
            showBottomDivider = getBoolean(R.styleable.CardItemView_showBottomDivider, false)
            isEnabled = getBoolean(R.styleable.CardItemView_android_enabled, true)
            isClickable = getBoolean(R.styleable.CardItemView_android_clickable, true)
            isFocusable = getBoolean(R.styleable.CardItemView_android_focusable, true)
            fullWidthDivider = getBoolean(R.styleable.CardItemView_fullWidthDivider, false)

            if (hasValue(R.styleable.CardItemView_drawableEnd)) {
                ensureInflatedEndView()
                endImageView!!.setImageDrawable(getDrawable(R.styleable.CardItemView_drawableEnd))
            }
        }
    }

    private fun ensureInflatedSummaryView() {
        if (summaryTextView == null) {
            summaryTextView = (findViewById<ViewStub>(R.id.viewstub_cardview_summary).inflate() as TextView).apply {
                setTextColor( if (isSummaryUserUpdatable) context.userUpdatableSummaryColor else context.defaultSummaryColor)
                maxLines = summaryMaxLines
            }
            titleTextView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                bottomToTop = summaryTextView!!.id
            }
            endImageView?.let {
                summaryTextView!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    endToStart = it.id
                }
            }
            findViewById<Space>(R.id.bottom_spacer).updateLayoutParams<ConstraintLayout.LayoutParams> {
                topToBottom = summaryTextView!!.id
            }
        }
    }

    private fun ensureInflatedIconView(){
        if (iconImageView == null) {
            val iconFrame = (findViewById<ViewStub>(R.id.viewstub_icon_frame).inflate() as FrameLayout)
            titleTextView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                startToEnd = iconFrame.id
            }
            iconImageView = iconFrame.findViewById(R.id.cardview_icon)
        }
    }

    private fun ensureInflatedEndView(){
        if (endImageView == null) {
            endImageView = (findViewById<ViewStub>(R.id.viewstub_end_view).inflate() as ImageView)
            summaryTextView?.updateLayoutParams<ConstraintLayout.LayoutParams> {
                endToStart = endImageView!!.id
            }
        }
    }

    private fun updateLayoutParams(){
        if (suspendLayoutUpdates) return

        val hasIcon = iconImageView?.drawable != null
        (iconImageView?.parent as? FrameLayout)?.isVisible = hasIcon

        val desiredDividerStartMargin = if (!hasIcon || fullWidthDivider) dividerMarginStart else dividerMarginStartWithIcon

        dividerViewTop?.apply {
            if (desiredDividerStartMargin == marginStart) return@apply
            updateLayoutParams<LayoutParams> { marginStart = desiredDividerStartMargin }
        }

        dividerViewBottom?.apply {
            if (desiredDividerStartMargin == marginStart) return@apply
            updateLayoutParams<LayoutParams> { marginStart = desiredDividerStartMargin }
        }
    }

    /**
     * Retrieves the [ImageView] positioned at the start of this view.
     *
     * The first invocation to this function inflates and adds the corresponding [ImageView]
     * that will be returned by this function. This image view is intended for setting an icon
     * to this view.
     *
     * @return The [ImageView] designated for the start icon of this view.
     */
    fun getIconImageView(): ImageView {
        ensureInflatedIconView()
        return iconImageView!!
    }

    /**
     * Retrieves the [ImageView] positioned at the end of this view.
     *
     * The first invocation of this function will inflate and add the corresponding [ImageView]
     * if it hasn't been already. This ImageView is intended for displaying a drawable
     * at the end of the view, such as a forward arrow or other indicator.
     *
     * @return The [ImageView] designated for the end drawable of this view.
     */
    fun getEndImageView(): ImageView {
        ensureInflatedEndView()
        return endImageView!!
    }

    /**
     * Returns the [TextView] used to display the title.
     *
     * @return The [TextView] for the title.
     */
    fun getTitleView(): TextView = titleTextView

    /**
     * Returns the [TextView] that displays the summary text.
     *
     * @return The [TextView] for the summary.
     */
    fun getSummaryView(): TextView {
        ensureInflatedSummaryView()
        return summaryTextView!!
    }

    override fun setEnabled(enabled: Boolean) {
        if (isEnabled == enabled) return
        super.setEnabled(enabled)
        containerView.apply {
            isEnabled = enabled
            alpha = when {
                enabled -> 1.0f
                else -> 0.4f
            }
        }
    }


    override fun setClickable(clickable: Boolean) {
        super.setClickable(clickable)
        containerView.isClickable = clickable
    }

    override fun setFocusable(focusable: Boolean) {
        super.setFocusable(focusable)
        containerView.isFocusable = focusable
    }

    override fun setOnClickListener(l: OnClickListener?) {
        containerView.setOnClickListener {
            if (isEnabled) {
                l?.onClick(this)
            }
        }
    }

    override fun dispatchTouchEvent(motionEvent: MotionEvent): Boolean {
        if (Build.VERSION.SDK_INT >= 29 && isEnabled && (isClickable || isFocusable)) {
            semTouchFeedbackAnimator.animate(motionEvent)
        }
        return super.dispatchTouchEvent(motionEvent)
    }
}