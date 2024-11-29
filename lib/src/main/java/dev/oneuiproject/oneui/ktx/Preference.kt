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
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import androidx.preference.TwoStatePreference
import dev.oneuiproject.oneui.preference.HorizontalRadioPreference
import kotlin.math.min

/**
 * Registers a callback to be invoked when the preference's value is changed.
 *
 * @param action A lambda function to be invoked when the preference's value changes.
 *                 The lambda receives the new value as its parameter. It can optionally
 *                 return a `false` boolean to prevent the preference from persisting
 *                 the new value.
 * @return The preference to allow for chaining calls.
 *
 * Caution: Ensure that the type parameter matches the expected type of the preference's value.
 * This function uses unchecked casts, so providing an incorrect type parameter may lead to runtime exceptions.
 *
 * Example usage:
 * ```
 * preference.onNewValue<String> { newValue ->
 *     // Handle the new value
 * }
 * ```
 */
@Deprecated(
    message = "Please use the type-safe onNewValue() function specific to the preference type.",
    replaceWith = ReplaceWith("onNewValue()")
)
inline fun <reified T : Any?> Preference.onNewValue(crossinline action: (newValue: T) -> Any): Preference {
    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
        action(v as T) as? Boolean ?: true
    }
    return this
}

/**
 * Registers a callback to be invoked when this [TwoStatePreference] changes its value.
 *
 * This function provides a type-safe way to set an [onPreferenceChangeListener][Preference.OnPreferenceChangeListener]
 * where the new value is provided as a [Boolean]. The listener will always return `true`, allowing the
 * preference to persist the new value automatically.
 *
 * If you need to conditionally prevent the preference from persisting the new value, consider using
 * [onNewValueConditional] instead.
 *
 * @param action A lambda function to be invoked when the preference's value changes.
 *               The lambda receives the new value as its parameter.
 * @return The preference instance to allow for chaining calls.
 *
 * Example usage:
 * ```kotlin
 * switchPreference.onNewValue { newValue ->
 *     // Handle the new value (Boolean)
 *     // The preference will automatically persist the new value.
 * }
 * ```
 */
inline fun <R : TwoStatePreference> R.onNewValue(crossinline action: (newValue: Boolean) -> Unit): R {
    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
        action(v as Boolean)
        true
    }
    return this
}

/**
 * Registers a callback to be invoked when this [TwoStatePreference] changes its value, allowing you to
 * conditionally control whether the new value should be persisted.
 *
 * This function provides a type-safe way to set an [onPreferenceChangeListener][Preference.OnPreferenceChangeListener]
 * for a [TwoStatePreference], where the new value is supplied as a [Boolean]. The listener's return value determines whether the
 * preference should persist the new value:
 *
 * - Return `true` to allow the preference to persist the new value automatically.
 * - Return `false` to prevent the preference from persisting the new value.
 *
 * Use this function when you need to validate or perform checks before accepting the new value.
 *
 * @param action A lambda function to be invoked when the preference's value changes.
 *               The lambda receives the new value as its parameter and should return `true` to accept
 *               the new value or `false` to reject it.
 * @return The preference instance to allow for chaining calls.
 *
 * Example usage:
 * ```kotlin
 * switchPreference.onNewValueConditional { newValue ->
 *     if (isValid(newValue)) {
 *         // Accept and persist the new value
 *         true
 *     } else {
 *         // Reject the new value
 *         false
 *     }
 * }
 * ```
 */
inline fun <R : TwoStatePreference> R.onNewValueConditional(crossinline action: (newValue: Boolean) -> Boolean): R {
    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
        action(v as Boolean)
    }
    return this
}

/**
 * Registers a callback to be invoked when this [SeekBarPreference] changes its value.
 *
 * This function provides a type-safe way to set an [onPreferenceChangeListener][Preference.OnPreferenceChangeListener]
 * for a [SeekBarPreference], where the new value is provided as a [Int]. The listener will always return `true`,
 * allowing the preference to persist the new value automatically.
 *
 * If you need to conditionally prevent the preference from persisting the new value, consider using
 * [onNewValueConditional] instead.
 *
 * @param action A lambda function to be invoked when the preference's value changes.
 *               The lambda receives the new value as its parameter.
 * @return The preference instance to allow for chaining calls.
 *
 * Example usage:
 * ```kotlin
 * seekbarPreference.onNewValue { newValue ->
 *     // Handle the new value (Boolean)
 *     // The preference will automatically persist the new value.
 * }
 * ```
 */
inline fun <R : SeekBarPreference> R.onNewValue(crossinline action: (newValue: Int) -> Unit): R {
    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
        action(v as Int)
        true
    }
    return this
}

/**
 * Registers a callback to be invoked when this [SeekBarPreference] changes its value, allowing you to
 * conditionally control whether the new value should be persisted.
 *
 * This function provides a type-safe way to set an [onPreferenceChangeListener][Preference.OnPreferenceChangeListener]
 * for a [SeekBarPreference], where the new value is supplied as a [Int]. The listener's return value determines whether the
 * preference should persist the new value:
 *
 * - Return `true` to allow the preference to persist the new value automatically.
 * - Return `false` to prevent the preference from persisting the new value.
 *
 * Use this function when you need to validate or perform checks before accepting the new value.
 *
 * @param action A lambda function to be invoked when the preference's value changes.
 *               The lambda receives the new value as its parameter and should return `true` to accept
 *               the new value or `false` to reject it.
 * @return The preference instance to allow for chaining calls.
 *
 * Example usage:
 * ```kotlin
 * seekbarPreference.onNewValueConditional { newValue ->
 *     if (isValid(newValue)) {
 *         // Accept and persist the new value
 *         true
 *     } else {
 *         // Reject the new value
 *         false
 *     }
 * }
 * ```
 */
inline fun <R : SeekBarPreference> R.onNewValueConditional(crossinline action: (newValue: Int) -> Boolean): R {
    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
        action(v as Int)
    }
    return this
}

/**
 * Registers a callback to be invoked when this [EditTextPreference] changes its value.
 *
 * This function provides a type-safe way to set an [onPreferenceChangeListener][Preference.OnPreferenceChangeListener]
 * for a [EditTextPreference], where the new value is provided as a [String]. The listener will always return `true`,
 * allowing the preference to persist the new value automatically.
 *
 * If you need to conditionally prevent the preference from persisting the new value, consider using
 * [onNewValueConditional] instead.
 *
 * @param action A lambda function to be invoked when the preference's value changes.
 *               The lambda receives the new value as its parameter.
 * @return The preference instance to allow for chaining calls.
 *
 * Example usage:
 * ```kotlin
 * editTextPreference.onNewValue { newValue ->
 *     // Handle the new value (Boolean)
 *     // The preference will automatically persist the new value.
 * }
 * ```
 */
inline fun <R : EditTextPreference> R.onNewValue(crossinline action: (newValue: String) -> Unit): R {
    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
        action(v as String)
        true
    }
    return this
}

/**
 * Registers a callback to be invoked when this [EditTextPreference] changes its value, allowing you to
 * conditionally control whether the new value should be persisted.
 *
 * This function provides a type-safe way to set an [onPreferenceChangeListener][Preference.OnPreferenceChangeListener]
 * for a [EditTextPreference], where the new value is supplied as a [String]. The listener's return value determines whether the
 * preference should persist the new value:
 *
 * - Return `true` to allow the preference to persist the new value automatically.
 * - Return `false` to prevent the preference from persisting the new value.
 *
 * Use this function when you need to validate or perform checks before accepting the new value.
 *
 * @param action A lambda function to be invoked when the preference's value changes.
 *               The lambda receives the new value as its parameter and should return `true` to accept
 *               the new value or `false` to reject it.
 * @return The preference instance to allow for chaining calls.
 *
 * Example usage:
 * ```kotlin
 * editTextPreference.onNewValueConditional { newValue ->
 *     if (isValid(newValue)) {
 *         // Accept and persist the new value
 *         true
 *     } else {
 *         // Reject the new value
 *         false
 *     }
 * }
 * ```
 */
inline fun <R : EditTextPreference> R.onNewValueConditional(crossinline action: (newValue: String) -> Boolean): R {
    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
        action(v as String)
    }
    return this
}

/**
 * Registers a callback to be invoked when this [ListPreference] changes its value.
 *
 * This function provides a type-safe way to set an [onPreferenceChangeListener][Preference.OnPreferenceChangeListener]
 * for a [ListPreference], where the new value is provided as a [String]. The listener will always return `true`,
 * allowing the preference to persist the new value automatically.
 *
 * If you need to conditionally prevent the preference from persisting the new value, consider using
 * [onNewValueConditional] instead.
 *
 * @param action A lambda function to be invoked when the preference's value changes.
 *               The lambda receives the new value as its parameter.
 * @return The preference instance to allow for chaining calls.
 *
 * Example usage:
 * ```kotlin
 * listPreference.onNewValue { newValue ->
 *     // Handle the new value (Boolean)
 *     // The preference will automatically persist the new value.
 * }
 * ```
 */
inline fun <R : ListPreference> R.onNewValue(crossinline action: (newValue: String) -> Unit): R {
    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
        action(v as String)
        true
    }
    return this
}

/**
 * Registers a callback to be invoked when this [ListPreference] changes its value, allowing you to
 * conditionally control whether the new value should be persisted.
 *
 * This function provides a type-safe way to set an [onPreferenceChangeListener][Preference.OnPreferenceChangeListener]
 * for a [ListPreference], where the new value is supplied as a [String]. The listener's return value determines whether the
 * preference should persist the new value:
 *
 * - Return `true` to allow the preference to persist the new value automatically.
 * - Return `false` to prevent the preference from persisting the new value.
 *
 * Use this function when you need to validate or perform checks before accepting the new value.
 *
 * @param action A lambda function to be invoked when the preference's value changes.
 *               The lambda receives the new value as its parameter and should return `true` to accept
 *               the new value or `false` to reject it.
 * @return The preference instance to allow for chaining calls.
 *
 * Example usage:
 * ```kotlin
 * listPreference.onNewValueConditional { newValue ->
 *     if (isValid(newValue)) {
 *         // Accept and persist the new value
 *         true
 *     } else {
 *         // Reject the new value
 *         false
 *     }
 * }
 * ```
 */
inline fun <R : ListPreference> R.onNewValueConditional(crossinline action: (newValue: String) -> Boolean): R {
    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
        action(v as String)
    }
    return this
}


/**
 * Registers a callback to be invoked when this [HorizontalRadioPreference] changes its value.
 *
 * This function provides a type-safe way to set an [onPreferenceChangeListener][Preference.OnPreferenceChangeListener]
 * for a [HorizontalRadioPreference], where the new value is provided as a [String]. The listener will always return `true`,
 * allowing the preference to persist the new value automatically.
 *
 * If you need to conditionally prevent the preference from persisting the new value, consider using
 * [onNewValueConditional] instead.
 *
 * @param action A lambda function to be invoked when the preference's value changes.
 *               The lambda receives the new value as its parameter.
 * @return The preference instance to allow for chaining calls.
 *
 * Example usage:
 * ```kotlin
 * listPreference.onNewValue { newValue ->
 *     // Handle the new value (Boolean)
 *     // The preference will automatically persist the new value.
 * }
 * ```
 */
inline fun HorizontalRadioPreference.onNewValue(crossinline action: (newValue: String) -> Unit): HorizontalRadioPreference {
    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
        action(v as String)
        true
    }
    return this
}

/**
 * Registers a callback to be invoked when this [HorizontalRadioPreference] changes its value, allowing you to
 * conditionally control whether the new value should be persisted.
 *
 * This function provides a type-safe way to set an [onPreferenceChangeListener][Preference.OnPreferenceChangeListener]
 * for a [HorizontalRadioPreference], where the new value is supplied as a [String]. The listener's return value determines whether the
 * preference should persist the new value:
 *
 * - Return `true` to allow the preference to persist the new value automatically.
 * - Return `false` to prevent the preference from persisting the new value.
 *
 * Use this function when you need to validate or perform checks before accepting the new value.
 *
 * @param action A lambda function to be invoked when the preference's value changes.
 *               The lambda receives the new value as its parameter and should return `true` to accept
 *               the new value or `false` to reject it.
 * @return The preference instance to allow for chaining calls.
 *
 * Example usage:
 * ```kotlin
 * horizontalRadioPreference.onNewValueConditional { newValue ->
 *     if (isValid(newValue)) {
 *         // Accept and persist the new value
 *         true
 *     } else {
 *         // Reject the new value
 *         false
 *     }
 * }
 * ```
 */
inline fun HorizontalRadioPreference.onNewValueConditional(crossinline action: (newValue: String) -> Boolean): HorizontalRadioPreference {
    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
        action(v as String)
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
inline fun <reified R : Preference> R.onClick(crossinline onClickPreference: (preference: R) -> Unit): Preference {
    setOnPreferenceClickListener { v ->
        onClickPreference(v as R)
        true
    }
    return this
}

/**
 * Updates the color of the summary text to visually indicate whether it is user-updatable.
 *
 * @param isUpdatable A boolean flag indicating if the summary text is user-updatable.
 *                    If true, the summary text color will be set to indicate editability.
 */
fun <R : Preference>R.setSummaryUpdatable(isUpdatable: Boolean): R{
    seslSetSummaryColor(if (isUpdatable) userUpdatableSummaryColor else defaultSummaryColor)
    return this
}

@Deprecated(
    message = "Use setSummaryUpdatable()",
    replaceWith = ReplaceWith("setSummaryUpdatable(isUpdatable)")
)
inline fun <R : Preference>R.setUpdatableSummaryColor(isUpdatable: Boolean): R = setSummaryUpdatable(isUpdatable)

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

inline fun <R : Preference>R.showDotBadge(show: Boolean = true): R{
    dotVisibility = show
    return this
}

inline fun <R : Preference>R.clearBadge(): R{
    dotVisibility = false
    return this
}



