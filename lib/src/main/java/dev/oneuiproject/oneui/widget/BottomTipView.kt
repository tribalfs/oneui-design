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


/**
 * A custom view that displays a tip at the bottom of the screen.
 *
 * This view consists of a title, a summary, and an optional link.
 * The title, summary, and link text can be set programmatically or through XML attributes.
 *
 * ## Example usage:
 * ```xml
 * <dev.oneuiproject.oneui.widget.BottomTipView
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:title="Tip Title"
 *     app:summary="This is a helpful tip."
 *     app:linkText="Learn More" />
 * ```
 *
 * You can also set an OnClickListener for the link programmatically:
 * ```kotlin
 * bottomTipView.setOnLinkClickListener {
 *     // Handle link click
 * }
 * ```
 * @param context The Context the view is running in, through which it can access the
 * current theme, resources, etc.
 * @param attrs (Optional) The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr (Optional) An attribute in the current theme that contains a
 * reference to a style resource that supplies default values for the view.
 * @param defStyleRes (Optional) A resource identifier of a style resource that
 * supplies default values for the view, used only if defStyleAttr is not provided
 * or cannot be found in the theme.
 */
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


    /**
     * Sets the title text to be displayed.
     *
     * @param titleText The text to be displayed as the title. Can be null to clear the title.
     */
    fun setTitle(titleText: CharSequence?) {
        titleTextView.text = titleText
    }

    /**
     * Sets the title text to be displayed.
     *
     * @param titleRes The resource ID of the title text.
     */
    fun setTitle(@StringRes titleRes: Int) = setTitle(context.getString(titleRes))

    /**
     * Sets the summary text of the tip view.
     *
     * @param summaryText The text to display as the summary. Can be null to hide the summary.
     */
    fun setSummary(summaryText: CharSequence?){
        tipContentView.text = summaryText
    }

    fun setSummary(@StringRes summaryStringRes: Int){
        setSummary(context.getString(summaryStringRes))
    }

    /**
     * Sets the link text and its click listener.
     * The link text will be underlined.
     *
     * @param linkText The text to be displayed as the link.
     * @param clickListener The listener to be invoked when the link is clicked.
     */
    fun setLink(linkText: CharSequence, clickListener: OnClickListener) {
        setLinkText(linkText)
        linkTextView.setOnClickListener(clickListener)

    }

    /**
     * Sets the link text and its click listener for the bottom tip view.
     *
     * @param linkText The resource ID of the string to be used as the link text.
     * @param clickListener The OnClickListener to be invoked when the link text is clicked.
     */
    fun setLink(@StringRes linkText: Int, clickListener: OnClickListener) {
        setLink(context.getString(linkText), clickListener)
    }

    private fun setLinkText(linkText: CharSequence){
        linkTextView.text = SpannableString(linkText).apply { setSpan(UnderlineSpan(), 0, linkText.length, 0) }
    }

    /**
     * Sets the click listener for the link text.
     *
     * @param clickListener The [OnClickListener] to be set, or null to remove the listener.
     */
    fun setOnLinkClickListener(clickListener: OnClickListener?){
        linkTextView.setOnClickListener(clickListener)
    }


}