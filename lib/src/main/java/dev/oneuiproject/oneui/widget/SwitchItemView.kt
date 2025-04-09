@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package dev.oneuiproject.oneui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.text.SpannableString
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewStub
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.graphics.ColorUtils
import androidx.core.view.isGone
import androidx.core.view.isVisible
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue

class SwitchItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var mSwitchView: SwitchCompat
    private var mTitleView: TextView
    private var mSummaryView: TextView
    private var mDividerViewTop: View? = null
    private var mDividerViewBottom: View? = null
    private var verticalDivider: View
    private val mMainContent: ConstraintLayout
    private val mContentFrame: FrameLayout
    private var badgeFrame: LinearLayout? = null
    private var bottomSpacer: Space
    private var mIsLargeLayout = false

    /**
     * @param viewId The view id of SwitchItemView
     * @param checked
     */
    var onCheckedChangedListener: ((viewId: Int, checked: Boolean) -> Unit)? = null

    /**
     *  Makes click and check change events separate. Allows you to register
     *  separate callbacks for click and check change events.
     *  false by default.
     */
    var separateSwitch: Boolean
        get() = verticalDivider.isVisible
        set(value) {
            if (verticalDivider.isVisible != value) {
                verticalDivider.isVisible = value
                mSwitchView.isClickable = value
            }
        }

    /**
     *  Show divider on top. True by default
     */
    var showTopDivider: Boolean
        get() = mDividerViewTop?.isVisible == true
        set(value) {
            if (value) ensureTopDivider()
            mDividerViewTop?.isVisible = value
        }

    /**
     *  Show divider at the bottom. False by default
     */
    var showBottomDivider: Boolean
        get() = mDividerViewBottom?.isVisible == true
        set(value) {
            if (value) ensureBottomDivider()
            mDividerViewBottom?.isVisible = value
        }

    private fun ensureTopDivider(){
        if (mDividerViewTop == null){
            mDividerViewTop = LayoutInflater.from(context)
                .inflate(R.layout.oui_des_widget_card_item_divider, this, false)
            addView(mDividerViewTop, 0)
        }
    }

    private fun ensureBottomDivider(){
        if (mDividerViewBottom == null){
            mDividerViewBottom = LayoutInflater.from(context)
                .inflate(R.layout.oui_des_widget_card_item_divider, this, false)
            addView(mDividerViewBottom, childCount)
        }
    }

    var summaryOn: CharSequence? = null
        set(value) {
            if (field != value) {
                field = value
                updateSubtitleVisibility()
            }
        }

    var summaryOff: CharSequence? = null
        set(value) {
            if (field != value) {
                field = value
                updateSubtitleVisibility()
            }
        }

    /**
     * Sets both the [summaryOn] and [summaryOff]
     */
    fun setSummary(summary: String?){
        summaryOn = summary
        summaryOff = summary
    }

    var title: CharSequence?
        get() = mTitleView.text?.toString()
        set(value) {
            if (mTitleView.text != value) {
                mTitleView.text = value
            }
        }

    fun setTiTle(value: SpannableString) {
        mTitleView.text = value
    }

    var isChecked: Boolean
        get() = mSwitchView.isChecked
        set(checked) {
            if (mSwitchView.isChecked == checked) return
            mSwitchView.isChecked = checked
        }

    var showBadge: Boolean
        get() = badgeFrame?.isVisible == true
        set(value) {
            if (value) {
                if (badgeFrame == null){
                    badgeFrame = (findViewById<ViewStub>(R.id.viewstub_badge_frame).inflate() as LinearLayout)
                }
                badgeFrame!!.isVisible = true
            } else {
                badgeFrame?.isGone = true
            }
        }

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        LayoutInflater.from(context).inflate(R.layout.oui_des_widget_switch_item, this@SwitchItemView)

        mTitleView = findViewById(R.id.switch_card_title)
        mSummaryView = findViewById(R.id.switch_card_summary)

        mMainContent = findViewById(R.id.main_content)
        mContentFrame = findViewById(R.id.content_frame)
        verticalDivider = findViewById(R.id.vertical_divider)

        mSwitchView = findViewById<SwitchCompat>(R.id.switch_widget).apply {
            isSaveEnabled = false
        }

        bottomSpacer = findViewById(R.id.bottom_spacer)

        isFocusable = true
        isClickable = true


        context.obtainStyledAttributes(
            attrs,
            R.styleable.SwitchItemView,
            defStyleAttr,
            defStyleRes
        ).use {a ->
            isEnabled = a.getBoolean(R.styleable.SwitchItemView_android_enabled, true)
            isChecked = a.getBoolean(R.styleable.SwitchItemView_android_checked, false)
            title = a.getText(R.styleable.SwitchItemView_title)
            summaryOn = a.getText(R.styleable.SwitchItemView_summaryOn)
            summaryOff = a.getText(R.styleable.SwitchItemView_summaryOff)
            separateSwitch = a.getBoolean(R.styleable.SwitchItemView_separateSwitch, false)
            showTopDivider = a.getBoolean(R.styleable.SwitchItemView_showTopDivider, true)
            showBottomDivider = a.getBoolean(R.styleable.SwitchItemView_showBottomDivider, false)
            if (a.getBoolean(R.styleable.SwitchItemView_userUpdatableSummary, false)){
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
                mSummaryView.setTextColor(ColorStateList(states, colors))
            }


        }

        mContentFrame.setOnClickListener {
            if (isEnabled) {
                if (!separateSwitch) {
                    mSwitchView.performClick()
                }else{
                    super.callOnClick()
                }
            }
        }

        mSwitchView.apply {
            setOnClickListener {v ->
                (v as SwitchCompat).isChecked.let { b ->
                    this.isChecked = b
                }
            }
            setOnCheckedChangeListener { _, isChecked ->
                updateSubtitleVisibility()
                onCheckedChangedListener?.invoke(this@SwitchItemView.id, isChecked)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun performClick(): Boolean {
        return mSwitchView.performClick()
    }

    private fun updateSubtitleVisibility(){
        val summaryToSet = if (isChecked) summaryOn else summaryOff
        mSummaryView.apply {
            if (text == summaryToSet) return
            text = summaryToSet
            isVisible = summaryToSet != null
        }
    }

    override fun setEnabled(enable: Boolean) {
        super.setEnabled(enable)
        mContentFrame.isEnabled = enable
        mMainContent.isEnabled = enable
        mTitleView.isEnabled = enable
        mSummaryView.isEnabled = enable
        mSwitchView.isEnabled = enable
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateSwitchPosition()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        updateSwitchPosition()
    }

    private val responsiveSwitchUpdater = {
        val res = context.resources
        val configuration = res.configuration
        val swDp = configuration.screenWidthDp
        var isLargeLayout = !((swDp > 320 || configuration.fontScale < 1.1f)
                && (swDp >= 411 || configuration.fontScale < 1.3f))

        if (isLargeLayout) {
            val titleLen: Float = mTitleView.paint.measureText(mTitleView.getText().toString())

            val availableWidth =
                mMainContent.width - mMainContent.paddingStart - mMainContent.paddingEnd -
                        (mSwitchView.width + mSwitchView.paddingStart + mSwitchView.paddingEnd)

            if (titleLen < availableWidth) {
                val summaryLen: Float = if (mSummaryView.isVisible) {
                    mSummaryView.paint.measureText(mSummaryView.getText().toString())
                } else 0.0f
                if (summaryLen < availableWidth) isLargeLayout = false
            }
        }

        if (mIsLargeLayout != isLargeLayout) {

            val switchLP = mSwitchView.layoutParams as ConstraintLayout.LayoutParams
            val titleLP = mTitleView.layoutParams as ConstraintLayout.LayoutParams
            val summaryLP = mSummaryView.layoutParams as ConstraintLayout.LayoutParams
            val bottomSpacerLP = bottomSpacer.layoutParams as ConstraintLayout.LayoutParams

            if (isLargeLayout) {
                switchLP.topToTop = ConstraintLayout.LayoutParams.UNSET
                switchLP.topToBottom = R.id.switch_card_summary
                switchLP.height = 22.dpToPx(res)
                res.getDimensionPixelSize(androidx.preference.R.dimen.sesl_preference_switch_padding_vertical)
                    .let {
                        switchLP.bottomMargin = it
                        switchLP.topMargin = it
                    }

                titleLP.endToStart = ConstraintLayout.LayoutParams.UNSET
                summaryLP.endToStart = ConstraintLayout.LayoutParams.UNSET
                summaryLP.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                bottomSpacerLP.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            } else {
                switchLP.topToBottom = ConstraintLayout.LayoutParams.UNSET
                switchLP.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                switchLP.height = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                switchLP.bottomMargin = 0
                switchLP.topMargin = 0

                titleLP.endToStart = R.id.switch_widget
                summaryLP.endToStart = R.id.switch_widget
                bottomSpacerLP.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            }
            mSwitchView.layoutParams = switchLP
            mTitleView.layoutParams = titleLP
            mSummaryView.layoutParams = summaryLP
            bottomSpacer.layoutParams = bottomSpacerLP

            mIsLargeLayout = isLargeLayout

            post { requestLayout() }
        }
    }

    private fun updateSwitchPosition(){
        removeCallbacks(responsiveSwitchUpdater)
        postDelayed(responsiveSwitchUpdater, 100)
    }

}