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