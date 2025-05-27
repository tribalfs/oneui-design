package dev.oneuiproject.oneui.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

/**
 * A Preference that can update its widget dynamically.
 *
 * This class extends the standard Preference and provides the ability to show or hide
 * the widget associated with the preference. It also allows for dynamically setting
 * the widget layout resource.
 *
 * Example usage:
 * ```xml
 * <dev.oneuiproject.oneui.preference.UpdatableWidgetPreference
 *     android:key="my_preference"
 *     android:title="My Preference"
 *     android:summary="This preference has an updatable widget"
 *     app:widgetLayout="@layout/my_custom_widget" />
 * ```
 *
 * In your code, you can then control the visibility of the widget:
 * ```kotlin
 * val preference = findPreference<UpdatableWidgetPreference>("my_preference")
 * preference?.showWidget = false // Hide the widget
 * preference?.setWidgetLayoutResource(R.layout.another_widget) // Change the widget layout
 * ```
 */
class UpdatableWidgetPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {

    override fun setWidgetLayoutResource(widget: Int) {
        super.setWidgetLayoutResource(widget)
        notifyChanged()
    }

    /** Shows or hides the widget frame. */
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