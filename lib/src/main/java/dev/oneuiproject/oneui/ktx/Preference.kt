@file:Suppress("MemberVisibilityCanBePrivate", "NOTHING_TO_INLINE", "unused")

package dev.oneuiproject.oneui.ktx


import android.content.res.ColorStateList
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SeekBarPreference
import androidx.preference.TwoStatePreference
import androidx.recyclerview.widget.RecyclerView
import dev.oneuiproject.oneui.preference.ColorPickerPreference
import dev.oneuiproject.oneui.preference.HorizontalRadioPreference

/**
 * Registers a callback to be invoked when the preference's value is changed.
 *
 * @param T The type of the value the specific preference holds.
 * @param action A lambda function to be invoked when the preference's value changes.
 *                 The lambda receives the new value as its parameter. It can optionally
 *                 return a `false` boolean to prevent the preference from persisting
 *                 the new value.
 * @return The preference to allow for chaining calls.
 *
 * @throws ClassCastException If the preference's value type does not match the expected type.
 * This function uses unchecked casts to the returned value.
 *
 * Example usage:
 * ```
 * preference.onNewValueUnsafe<String> { newValue ->
 *     // Handle the new value
 * }
 * ```
 */
@Throws(ClassCastException::class)
inline fun <reified T : Any?> Preference.onNewValueUnsafe(crossinline action: (newValue: T) -> Any): Preference {
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
 * switchPreference.onNewValueConditional { newValue: Boolean ->
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
 *     // Handle the new value (Int)
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
 * seekbarPreference.onNewValueConditional { newValue: Int ->
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
 *     // Handle the new value (String)
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
 * editTextPreference.onNewValueConditional { newValue: String ->
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
 *     // Handle the new value (String)
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
 * listPreference.onNewValueConditional { newValue: String ->
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
 * Registers a callback to be invoked when this [MultiSelectListPreference] changes its value.
 *
 * This function provides a type-safe way to set an [onPreferenceChangeListener][Preference.OnPreferenceChangeListener]
 * for a [MultiSelectListPreference], where the new value is provided as a [Set&lt;String&gt;][Set].
 * The listener will always return `true`, allowing the preference to persist the new value automatically.
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
 * multiSelectListPreference.onNewValue { newValue ->
 *     // Handle the new value (Set<String>)
 *     // The preference will automatically persist the new value.
 * }
 * ```
 */
inline fun <R : MultiSelectListPreference> R.onNewValue(crossinline action: (newValue: String) -> Unit): R {
    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
        action(v as String)
        true
    }
    return this
}

/**
 * Registers a callback to be invoked when this [MultiSelectListPreference] changes its value, allowing you to
 * conditionally control whether the new value should be persisted.
 *
 * This function provides a type-safe way to set an [onPreferenceChangeListener][Preference.OnPreferenceChangeListener]
 * for a [MultiSelectListPreference], where the new value is supplied as a [Set&lt;String&gt;][Set].
 * The listener's return value determines whether the preference should persist the new value:
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
 * multiSelectListPreference.onNewValueConditional { newValue: Set<String> ->
 *     if (process(newValue)) {
 *         // Accept and persist the new value
 *         true
 *     } else {
 *         // Reject the new value
 *         false
 *     }
 * }
 * ```
 */
inline fun <R : MultiSelectListPreference> R.onNewValueConditional(crossinline action: (newValue: String) -> Boolean): R {
    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
        action(v as String)
    }
    return this
}

/**
 * Registers a callback to be invoked when this [ColorPickerPreference] changes its value.
 *
 * This function provides a type-safe way to set an [onPreferenceChangeListener][Preference.OnPreferenceChangeListener]
 * for a [ColorPickerPreference], where the new value is provided as a [Int]. The listener will always return `true`,
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
 * colorPickerPreference.onNewValue { newValue ->
 *     // Handle the new value (Int)
 *     // The preference will automatically persist the new value.
 * }
 * ```
 */
inline fun ColorPickerPreference.onNewValue(crossinline action: (newValue: Int) -> Unit): ColorPickerPreference {
    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
        action(v as Int)
        true
    }
    return this
}

/**
 * Registers a callback to be invoked when this [ColorPickerPreference] changes its value, allowing you to
 * conditionally control whether the new value should be persisted.
 *
 * This function provides a type-safe way to set an [onPreferenceChangeListener][Preference.OnPreferenceChangeListener]
 * for a [ColorPickerPreference], where the new value is supplied as a [Int]. The listener's return value determines whether the
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
 * colorPickerPreference.onNewValueConditional { newValue: Int ->
 *     if (process(newValue)) {
 *         // Accept and persist the new value
 *         true
 *     } else {
 *         // Reject the new value
 *         false
 *     }
 * }
 * ```
 */
inline fun ColorPickerPreference.onNewValueConditional(crossinline action: (newValue: Int) -> Boolean): ColorPickerPreference {
    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
        action(v as Int)
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
 * horizontalRadioPreference.onNewValue { newValue ->
 *     // Handle the new value (String)
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
 * horizontalRadioPreference.onNewValueConditional { newValue: String ->
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
 * Sets a custom `summaryProvider` for this [Preference] using the provided [summary] lambda.
 *
 * Additionally, you can specify whether the summary text is user-updatable.
 *
 * @param isUserUpdatable An optional Boolean indicating if the summary text is user-updatable.
 *                        If `true`, the summary text color will be set to indicate that it is user-updatable.
 * @param summary A lambda function that takes this preference as input and returns a [CharSequence] to be displayed as the summary.
 *
 * ```kotlin
 * // Example usage:
 * preference1.provideSummary {
 *     "Current value: ${it.value}"
 * }

 * preference2.provideSummary(true) {
 *     "Current value: ${it.value}"
 * }
 * ```
 * @see setSummaryUpdatable
 */
inline fun <R : Preference> R.provideSummary(isUserUpdatable: Boolean? = null, crossinline summary: (preference: R) -> CharSequence?) {
    summaryProvider = Preference.SummaryProvider<R> {
        summary.invoke(it)
    }
    isUserUpdatable?.let { setSummaryUpdatable(it) }
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

/**
 * Retrieves a [ColorStateList] suitable for a preference summary that is user-updatable.
 *
 * This color state list uses the `colorPrimaryDark` theme attribute for the enabled state.
 * For the disabled state, it uses the same color but with a reduced alpha (40% of the enabled alpha,
 * or at least 20% alpha, whichever is greater).
 *
 * This provides a visual cue that the summary's content can be changed by the user.
 */
inline val Preference.userUpdatableSummaryColor: ColorStateList get() = context.userUpdatableSummaryColor

/**
 * Retrieves the default summary color for the preference based on the current theme (light or dark).
 * This property returns a [ColorStateList] that represents the default color used for preference summaries.
 *
 * This is typically used as for summaries that are not user-updatable.
 *
 * @return A [ColorStateList] representing the default summary color.
 * @see userUpdatableSummaryColor
 * @see setSummaryUpdatable
 */
inline val Preference.defaultSummaryColor: ColorStateList get() = context.defaultSummaryColor


inline fun <R : Preference>R.showDotBadge(show: Boolean = true): R{
    dotVisibility = show
    return this
}

inline fun <R : Preference>R.clearBadge(): R{
    dotVisibility = false
    return this
}

/**
 * Finds the currently displayed [View] for this [Preference].
 *
 * This must be called only when the preference is actively displayed on the screen,
 * typically within callbacks like [Preference.setOnPreferenceClickListener] or
 * [Preference.setOnPreferenceChangeListener]. It is only safe to call this between
 * `PreferenceFragmentCompat.onViewCreated` and `PreferenceFragmentCompat.onDestroyView`.
 *
 * @receiver The Preference whose view is to be found.
 * @param pfc The [PreferenceFragmentCompat] instance hosting the preference.
 * @return The [View] bound to this preference, or `null` if it is not currently
 *         bound or displayed (e.g., scrolled off-screen).
 *
 * @see PreferenceFragmentCompat.findPreference
 */
context(pfc: PreferenceFragmentCompat)
val Preference.itemView: View?
    get() = (pfc.listView.adapter as? PreferenceGroup.PreferencePositionCallback)
        ?.getPreferenceAdapterPosition(this)
        .takeIf { it != RecyclerView.NO_POSITION }
        ?.let { p -> pfc.listView.findViewHolderForAdapterPosition(p)?.itemView }

