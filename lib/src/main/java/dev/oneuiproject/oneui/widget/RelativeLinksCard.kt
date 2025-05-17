@file:Suppress("unused", "NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.res.use
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.preference.PreferenceScreen
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue

/**
 * Widget to display relative links which is intended to be added
 * at the bottom of a [PreferenceScreen] or on other layouts.
 *
 * Example usage:
 *
 * ```xml
 * <!--relative_card_layout.xml-->
 *
 *  <dev.oneuiproject.oneui.widget.RelativeLinksCard
 *     xmlns:android="http://schemas.android.com/apk/res/android"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent">
 *
 *     <!--Optional initial links-->
 *
 *     <TextView
 *         android:id="@+id/relatedLink1"
 *         android:layout_width="wrap_content"
 *         android:layout_height="wrap_content"
 *         android:text="Relative link 1"/>
 *
 *     <TextView
 *         android:id="@+id/relatedLink2"
 *         android:layout_width="wrap_content"
 *         android:layout_height="wrap_content"
 *         android:text="Relative link 2"/>
 *
 * </dev.oneuiproject.oneui.widget.RelativeLinksCard>
 * ```
 *
 * ```xml
 * <!--first_fragment.xml-->
 *
 * <LinearLayout
 *     xmlns:android="http://schemas.android.com/apk/res/android"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent">
 *
 *     <!--Other child views-->
 *
 *     <include
 *         android:layout="@layout/relative_card_layout"/>
 *
 * </LinearLayout>
 * ```
 *
 * Adding RelativeLinksCard to preference xml:
 * ```
 *<PreferenceScreen
 *    xmlns:android="http://schemas.android.com/apk/res/android"
 *    xmlns:app="http://schemas.android.com/apk/res-auto">
 *
 *    <!--other preferences-->
 *
 *    <dev.oneuiproject.oneui.preference.InsetPreferenceCategory/>
 *    <dev.oneuiproject.oneui.preference.LayoutPreference
 *         android:layout="@layout/relative_card_layout.xml"/>
 *</PreferenceScreen
 * ```
 * It can accessed inside a PreferenceFragmentCompat
 * using the getRelativeLinksCard() extension function:
 * ```kotlin
 * private lateinit var relativeLinksCard: RelativeLinksCard
 *
 * override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *      super.onViewCreated(view, savedInstanceState)
 *      relativeLinksCard = getRelativeLinksCard()
 * }
 * ```
 * RelativeLinksCard can also be added programmatically inside a PreferenceFragmentCompat
 * using the addRelativeLinksCard() extension function:
 * ```kotlin
 * private lateinit var relativeLinksCard: RelativeLinksCard
 *
 * override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *      super.onViewCreated(view, savedInstanceState)
 *      relativeLinksCard = addRelativeLinksCard()
 *      relativeLinksCard.addLinks(
 *         RelativeLink("Link1 text") { v -> /*Process on click event*/},
 *         RelativeLink("Link2 text") { v -> /*Process on click event*/}
 *      )
 * }
 * ```
 * @see addLinks
 * @see replaceLinks
 * @see [dev.oneuiproject.oneui.ktx.addRelativeLinksCard]
 * @see [dev.oneuiproject.oneui.ktx.getRelativeLinksCard]
 * @see [dev.oneuiproject.oneui.ktx.removeRelativeLinksCard]
 */
class RelativeLinksCard @JvmOverloads constructor(
    mContext: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(mContext, attrs, defStyleAttr, defStyleRes) {
    private var parentView: View? = null
    private var cardTitleText: TextView
    private var linkContainer: ViewGroup
    private var topDivider: View? = null

    init {
        orientation = VERTICAL
        val context = context
        parentView =
            LayoutInflater.from(context).inflate(R.layout.oui_des_widget_relative_links_card, this)
                .apply {
                    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    setPadding(
                        0,
                        0,
                        0,
                        context.resources.getDimensionPixelSize(R.dimen.oui_des_relative_link_gap_height)
                    )
                    context.getThemeAttributeValue(androidx.appcompat.R.attr.roundedCornerColor)
                        ?.let {
                            setBackgroundColor(it.data)
                        }

                }.also {
                    cardTitleText = it.findViewById(R.id.link_title)
                    linkContainer = it.findViewById(R.id.link_container)
                    topDivider = it.findViewById(R.id.link_divider)
                }
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.RelativeLinksCard,
            defStyleAttr,
            defStyleRes
        ).use {
            val cardTitle = it.getString(R.styleable.RelativeLinksCard_title)
            showTopDivider = it.getBoolean(R.styleable.RelativeLinksCard_showTopDivider, false)
            cardTitle?.let { t -> cardTitleText.text = t }
        }

    }

    fun setTitle(@StringRes resid: Int) = cardTitleText.setText(resid)

    fun setTitle(title: CharSequence?) {
        cardTitleText.text = title
    }

    fun addLink(
        linkTitle: CharSequence?,
        onClick: OnClickListener?
    ) = TextView(ContextThemeWrapper(context, R.style.OneUI_RelativeLinkTextViewTextStyle)).apply {
        isFocusable = true
        isClickable = true
        text = linkTitle
        setBackgroundResource(R.drawable.oui_des_relative_links_item_bg)
        setOnClickListener(onClick)
        linkContainer.addView(this, createLinkParams())
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (parentView == null) {
            super.addView(child, index, params)
        } else {
            if (child is TextView) {
                with(child) {
                    isFocusable = true
                    isClickable = true
                    setBackgroundResource(R.drawable.oui_des_relative_links_item_bg)
                    TextViewCompat.setTextAppearance(
                        this,
                        R.style.OneUI_RelativeLinkTextViewTextStyle
                    )
                }
                linkContainer.addView(
                    child,
                    index,
                    createLinkParams()
                )
            } else {
                if (child.id == R.id.relative_links_card_divider) {
                    super.addView(child, index, params)
                }else{
                    throw IllegalArgumentException("${child.javaClass.simpleName} is not allowed as child here, only TextView can be added.")
                }
            }
        }
    }

    private fun createLinkParams() =
        with(resources) {
           LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                marginStart = getDimensionPixelSize(R.dimen.oui_des_relative_link_view_margin_start_end) -
                            getDimensionPixelSize(R.dimen.oui_des_relative_link_text_padding_side)
            }
        }

    fun clearLinks() {
        if (linkContainer.childCount < 2) return
        linkContainer.removeViews(1, linkContainer.childCount - 1)
    }

    var showTopDivider: Boolean
        get() = topDivider?.isVisible == true
        set(value) {
            if (value) ensureTopDivider()
            topDivider?.isVisible = value
        }

    private fun ensureTopDivider(){
        if (topDivider == null){
            topDivider = LayoutInflater.from(context)
                .inflate(R.layout.oui_des_widget_relative_links_card_divider, this, false)
            addView(topDivider, 0)
        }
    }
    companion object {
        private const val TAG = "RelativeLinksCard"
    }

}


data class RelativeLink(
    @JvmField
    val title: CharSequence,
    @JvmField
    val onClick: OnClickListener
)


inline fun RelativeLinksCard.addLinks(vararg relativeLinks: RelativeLink) {
    relativeLinks.forEach {
        addLink(it.title, it.onClick)
    }
}

inline fun RelativeLinksCard.replaceLinks(vararg relativeLinks: RelativeLink) {
    clearLinks()
    addLinks(*relativeLinks)
}

