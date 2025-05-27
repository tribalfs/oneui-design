@file:Suppress("unused")

package dev.oneuiproject.oneui.preference

import android.content.Context
import android.util.AttributeSet
import android.view.View.OnClickListener
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.preference.R.attr.preferenceStyle
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue
import dev.oneuiproject.oneui.widget.TipsCard

/**
 * A Preference that displays a TipsCard.
 *
 * This preference is used to display a TipsCard typically at the upper portion of a PreferenceScreen.
 * It allows setting a title, summary, and a cancel button with a custom listener.
 * Additionally, it provides functionality to add custom buttons to the bottom bar of the TipsCard.
 *
 * @param context The Context this preference is associated with.
 * @param attrs The attributes of the XML tag that is inflating the preference.
 * @param defStyleAttr An attribute in the current theme that contains a reference to a style resource
 *                     that supplies default values for the view. Can be 0 to not look for defaults.
 * @param defStyleRes A resource identifier of a style resource that supplies default values for the view,
 *                    used only if defStyleAttr is 0 or can not be found in the theme. Can be 0
 *                    to not look for defaults.
 */
class TipsCardPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = preferenceStyle,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    private var cancelBtnOCL: OnClickListener? = null
    private val bottomBarBtns = ArrayList<TextView>()
    private var itemView: TipsCard? = null

    init {
        isSelectable = false
        layoutResource = R.layout.oui_des_preference_tips_layout
        val primaryTextColor = ResourcesCompat.getColorStateList(context.resources,
            context.getThemeAttributeValue(android.R.attr.textColorPrimary)!!.resourceId,
            context.theme)
        seslSetSummaryColor(primaryTextColor)
    }

    override fun onBindViewHolder(preferenceViewHolder: PreferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder)
        itemView = (preferenceViewHolder.itemView as TipsCard).apply {
            setTitle(title)
            setSummary(summary)
            setOnCancelClickListener(cancelBtnOCL)

            // workaround since we can't use setSelectable here
            setOnClickListener { onPreferenceClickListener?.onPreferenceClick(this@TipsCardPreference) }

            if (bottomBarBtns.isNotEmpty()) {
                for (btn in bottomBarBtns) addButton(btn)
                bottomBarBtns.clear()
            }
        }

    }

    fun addButton(text: CharSequence?, listener: OnClickListener?): TextView {
        val txtView = TextView(context, null, 0, R.style.OneUI_TipsCardTextButtonStyle).apply {
            setText(text)
            setOnClickListener(listener)
        }
        itemView?.addButton(txtView) ?: bottomBarBtns.add(txtView)
        return txtView
    }

    fun setOnCancelClickListener(listener: OnClickListener?) {
        if (cancelBtnOCL == listener) return
        cancelBtnOCL = listener
        itemView?.setOnCancelClickListener(listener)
    }
}