package dev.oneuiproject.oneui.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

class UpdatableWidgetPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {

    override fun setWidgetLayoutResource(widget: Int) {
        super.setWidgetLayoutResource(widget)
        notifyChanged()
    }

    var showWidget: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            notifyChanged()
        }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.findViewById<LinearLayout>(android.R.id.widget_frame).apply {
            isVisible = showWidget
        }
    }
}