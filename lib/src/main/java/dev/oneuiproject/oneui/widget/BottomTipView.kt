package dev.oneuiproject.oneui.widget

import android.content.Context
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.withStyledAttributes
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue
import dev.oneuiproject.oneui.ktx.dpToPx


class BottomTipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : RoundedLinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var titleTextView: TextView
    private var tipContentView: TextView
    private var linkTextView: TextView

    init {
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        orientation = VERTICAL
        val vPadding = 20.dpToPx(resources)
        setPadding(0, vPadding, 0, vPadding)
        context.getThemeAttributeValue(androidx.appcompat.R.attr.roundedCornerColor)?.let {
            setBackgroundColor(it.data)
        }
        inflate(context, R.layout.oui_des_widget_bottom_tip, this)
        titleTextView = findViewById(R.id.tv_tip_title)
        tipContentView = findViewById(R.id.tv_tip_content)
        linkTextView = findViewById(R.id.tv_tip_link)

        if (attrs != null) {
            context.withStyledAttributes(
                attrs,
                R.styleable.BottomTipView
            ) {
                getText(R.styleable.BottomTipView_title)?.let { setTitle(it) }
                getText(R.styleable.BottomTipView_summary)?.let { setSummary(it) }
                getText(R.styleable.BottomTipView_linkText)?.let { setLinkText(it) }
            }
        }
    }


    fun setTitle(titleText: CharSequence?) {
        titleTextView.text = titleText
    }

    fun setTitle(@StringRes titleRes: Int) = setTitle(context.getString(titleRes))

    fun setSummary(summaryText: CharSequence?){
        tipContentView.text = summaryText
    }

    fun setSummary(@StringRes summaryStringRes: Int){
        setSummary(context.getString(summaryStringRes))
    }

    fun setLink(linkText: CharSequence, clickListener: OnClickListener) {
        setLinkText(linkText)
        linkTextView.setOnClickListener(clickListener)

    }

    fun setLink(@StringRes linkText: Int, clickListener: OnClickListener) {
        setLink(context.getString(linkText), clickListener)
    }

    private fun setLinkText(linkText: CharSequence){
        linkTextView.text = SpannableString(linkText).apply { setSpan(UnderlineSpan(), 0, linkText.length, 0) }
    }

    fun setOnLinkClickListener(clickListener: OnClickListener?){
        linkTextView.setOnClickListener(clickListener)
    }


}