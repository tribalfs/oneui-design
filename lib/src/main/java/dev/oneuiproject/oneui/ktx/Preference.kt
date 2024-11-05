@file:Suppress("MemberVisibilityCanBePrivate", "NOTHING_TO_INLINE", "unused")

package dev.oneuiproject.oneui.ktx


import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.R.color.sesl_secondary_text_dark
import androidx.appcompat.R.color.sesl_secondary_text_light
import androidx.appcompat.util.SeslMisc
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import androidx.preference.TwoStatePreference
import dev.oneuiproject.oneui.design.R
import kotlin.math.min

/**
 * Registers a callback to be invoked when the preference's value is changed.
 *
 * @param onChange A lambda function to be invoked when the preference's value changes.
 *                 The lambda receives the new value as its parameter.
 * @return The preference to allow for chaining calls.
 *
 * Example usage:
 * ```
 * preference.onNewValue<String> { newValue ->
 *     // Handle the new value
 * }
 * ```
 */
inline fun <reified R : Any?> Preference.onNewValue(crossinline onChange: (newValue: R) -> Unit): Preference {
    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
        if (this is TwoStatePreference) {
            isChecked = v as Boolean
        }
        onChange(v as R)
        true
    }
    return this
}

/**
 * Registers a callback to be invoked when the preference's value is changed, with the ability
 * to control whether the internal state is updated.
 *
 * @param onChange A lambda function to be invoked when the preference's value changes.
 *                 The lambda receives the new value as its parameter and should return `true`
 *                 if the internal state should be updated, `false` otherwise.
 * @return The preference to allow for chaining calls.
 *
 * Example usage:
 * ```
 * preference.onUpdateValue<String> { newValue ->
 *     // Handle the new value and return true if the internal state should be updated
 *     true
 * }
 * ```
 */
inline fun <reified R : Any?> Preference.onUpdateValue(crossinline onChange: (newValue: R) -> Boolean): Preference {
    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
        onChange(v as R)
    }
    return this
}


/**
 * Registers a callback to be invoked when this preference is clicked.
 *
 * @param onClickPreference A lambda function to be invoked when the preference is clicked.
 *                          The lambda receives the clicked preference as its parameter.
 * @return The preference to allow for chaining calls.
 *
 * Example usage:
 * ```
 * preference.onClick { clickedPreference ->
 *     // Handle the click event
 * }
 * ```
 */
inline fun <reified T : Preference> T.onClick(crossinline onClickPreference: (preference: T) -> Unit): Preference {
    setOnPreferenceClickListener { v ->
        onClickPreference(v as T)
        true
    }
    return this
}

/**
 * Registers a callback to be invoked when this preference is clicked, with the ability
 * to control whether the click is handled.
 *
 * @param onClickPreference A lambda function to be invoked when the preference is clicked.
 *                          The lambda receives the clicked preference as its parameter and
 *                          should return `true` if the click is handled, `false` otherwise.
 * @return The preference to allow for chaining calls.
 *
 * Example usage:
 * ```
 * preference.onHandleClick { clickedPreference ->
 *     // Handle the click event and return true if handled
 *     true
 * }
 * ```
 */
inline fun <reified T : Preference> T.onHandleClick(crossinline onClickPreference: (preference: T) -> Boolean): Preference {
    setOnPreferenceClickListener { v ->
        onClickPreference(v as T)
    }
    return this
}


inline fun <T : Preference>T.setUpdatableSummaryColor(isUserUpdatable: Boolean): T{
    seslSetSummaryColor(if (isUserUpdatable) userUpdatableSummaryColor else defaultSummaryColor)
    return this
}

inline val Preference.userUpdatableSummaryColor: ColorStateList
    get(){
        val enabledColor = context.getThemeAttributeValue(androidx.appcompat.R.attr.colorPrimaryDark)!!.data
        val enabledAlpha = Color.alpha(enabledColor)
        val disabledAlpha = (enabledAlpha * 0.4).toInt()
            .coerceAtLeast(min(51, enabledAlpha)) // 51 represents 20% alpha
        val disabledColor = ColorUtils.setAlphaComponent(enabledColor, disabledAlpha)
        return ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_enabled),
                intArrayOf(-android.R.attr.state_enabled)
            ),
            intArrayOf(enabledColor, disabledColor)
        )
    }

inline val Preference.defaultSummaryColor: ColorStateList
    @SuppressLint("RestrictedApi")
    get(){
        val colorResId = if (SeslMisc.isLightTheme(context)) sesl_secondary_text_light else sesl_secondary_text_dark
        return ContextCompat.getColorStateList(context, colorResId)!!
    }

inline fun <T : Preference>T.showDotBadge(show: Boolean = true): T{
    dotVisibility = show
    return this
}

inline fun <T : Preference>T.clearBadge(): T{
    dotVisibility = false
    return this
}



