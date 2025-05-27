package dev.oneuiproject.oneui.preference

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.SeslSwitchBar
import androidx.appcompat.widget.SeslSwitchBar.OnSwitchChangeListener
import androidx.appcompat.widget.SwitchCompat
import androidx.preference.PreferenceViewHolder
import androidx.preference.TwoStatePreference
import dev.oneuiproject.oneui.design.R

/**
 * A Preference that provides a two-state toggleable option using a [SeslSwitchBar].
 *
 * This preference will store a boolean into the SharedPreferences.
 *
 * @attr ref androidx.preference.R.styleable#SwitchPreference_summaryOff
 * @attr ref androidx.preference.R.styleable#SwitchPreference_summaryOn
 * @attr ref androidx.preference.R.styleable#SwitchPreference_switchTextOff
 * @attr ref androidx.preference.R.styleable#SwitchPreference_switchTextOn
 * @attr ref androidx.preference.R.styleable#TwoStatePreference_disableDependentsState
 */
class SwitchBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : TwoStatePreference(context, attrs, defStyleAttr, defStyleRes) {
    init {
        setLayoutResource(R.layout.oui_des_preference_switch_bar_layout)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val switchBar = holder.itemView as SeslSwitchBar
        switchBar.setChecked(mChecked)
        switchBar.addOnSwitchChangeListener(OnSwitchChangeListener { switchView: SwitchCompat?, isChecked: Boolean ->
            if (isChecked == mChecked) return@OnSwitchChangeListener
            if (!callChangeListener(isChecked)) {
                switchBar.setChecked(!isChecked)
                return@OnSwitchChangeListener
            }
            setChecked(isChecked)
        })
        holder.setDividerAllowedAbove(false)
        holder.setDividerAllowedBelow(false)
    }
}
