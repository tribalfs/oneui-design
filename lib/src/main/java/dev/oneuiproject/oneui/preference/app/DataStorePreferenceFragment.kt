package dev.oneuiproject.oneui.preference.app

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.TwoStatePreference
import dev.oneuiproject.oneui.preference.core.BooleanSetter
import dev.oneuiproject.oneui.preference.core.FloatSetter
import dev.oneuiproject.oneui.preference.core.IntegerSetter
import dev.oneuiproject.oneui.preference.core.LongSetter
import dev.oneuiproject.oneui.preference.core.StringSetSetter
import dev.oneuiproject.oneui.preference.core.StringSetter
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArraySet

/**
 * A base [PreferenceFragmentCompat] implementation that automatically integrates
 * Jetpack’s [DataStore] with the AndroidX Preferences UI system, providing a
 * modern and efficient alternative to the legacy file-based [android.content.SharedPreferences].
 *
 * This fragment connects a [PreferenceDataStore] (specifically, an [ObservablePreferencesDataStore])
 * to the [androidx.preference.PreferenceManager], enabling two-way synchronization between
 * user interface preferences and a persistent [DataStore] backend.
 *
 * ## Key Features
 * - **Automatic DataStore Binding:** Subclasses only need to provide a [DataStore]
 *   instance via [DataStoreProvider.getDataStore]; this fragment handles the wiring
 *   between the [androidx.preference.PreferenceManager] and the DataStore automatically.
 * - **Lifecycle-Aware Listeners:** Automatically registers and unregisters itself
 *   as a listener to the associated [ObservablePreferencesDataStore] during
 *   `onCreate()` and `onDestroy()` to prevent leaks and dangling observers.
 *
 * ## Usage
 * Subclass this fragment and implement [DataStoreProvider] to supply your
 * specific [ObservablePreferencesDataStore] instance:
 *
 * ```kotlin
 * class UserSettingsFragment : DataStorePreferenceFragment() {
 *     override fun getDataStore(): ObservablePreferencesDataStore = userSettingsDataStore
 * }
 * ```
 *
 * ## Threading
 * - All write and read operations in [ObservablePreferencesDataStore]
 *   are performed synchronously.
 *
 * ## Custom value preferences
 * To support custom value preference types, have your custom [Preference] class
 * implement one of the following "Setter" interfaces:
 * - [IntegerSetter]
 * - [StringSetter]
 * - [BooleanSetter]
 * - [StringSetSetter]
 * - [FloatSetter]
 * - [LongSetter]
 *
 * ## Notes
 * - This class assumes the use of the **Preferences DataStore** variant (not Proto).
 *
 * @see ObservablePreferencesDataStore
 * @see DataStoreProvider
 */
abstract class DataStorePreferenceFragment : PreferenceFragmentCompat(), DataStoreProvider {
    @CallSuper
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Set the custom data store from the concrete implementation.
        preferenceManager.preferenceDataStore = getDataStore()
    }

    /**
     * This listener receives all preference change events, both from UI interactions and
     * from external DataStore updates. It filters out immediate duplicate events.
     */
    private val onPreferencesChangeListener =
        object : ObservablePreferencesDataStore.OnPreferencesChangeListener {
            @Volatile private var lastKeyInvoked: String? = null
            @Volatile private var lastValueInvoked: Any? = null
            private var pendingUpdates: MutableMap<String, Any>? = null
            var skipChange = false

            override fun onPreferenceChanged(key: String, newValue: Any?, oldValue: Any?) {
                if (skipChange) return
                if (lastKeyInvoked == key && lastValueInvoked == newValue) {
                    // It's a duplicate echo. Clear the state and stop processing.
                    lastKeyInvoked = null
                    lastValueInvoked = null
                    return
                }
                lastKeyInvoked = key
                lastValueInvoked = newValue

                findPreference<Preference>(key)?.apply {
                    // Ensure ui is update in case triggered from outside
                    if (isResumed) {
                        updatePreference(this, newValue)
                    } else {
                        if (pendingUpdates == null) {
                            pendingUpdates = mutableMapOf()
                        }
                        pendingUpdates?.set(key, newValue!!)
                    }
                }
            }

            fun applyPendingUpdates() {
                pendingUpdates?.let { updates ->
                    if (updates.isEmpty()) return@let

                    skipChange = true
                    updates.forEach { (key, value) ->
                        findPreference<Preference>(key)?.apply {
                            updatePreference(this, value)
                        }
                    }
                    skipChange = false
                    updates.clear()
                }
                pendingUpdates = null
            }
        }

    private fun updatePreference(pref: Preference, value: Any?) {
        @Suppress("UNCHECKED_CAST")
        when (pref) {
            // -- androidx provided prefs --
            is TwoStatePreference -> pref.isChecked = value as? Boolean ?: false
            is SeekBarPreference -> (value as? Int)?.let { pref.value = it }
            is EditTextPreference -> pref.text = value as? String
            is ListPreference -> pref.value = value as? String
            is MultiSelectListPreference -> pref.values = value as? Set<String> ?: emptySet()

            // -- custom prefs --
            is IntegerSetter -> (value as? Int)?.let { pref.value = it }
            is StringSetter -> (value as? String)?.let { pref.value = it }
            is BooleanSetter -> (value as? Boolean)?.let { pref.value = it }
            is StringSetSetter -> (value as? Set<String>)?.let { pref.value = it }
            is FloatSetter -> (value as? Float)?.let { pref.value = it }
            is LongSetter -> (value as? Long)?.let { pref.value = it }
        }
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getDataStore().addOnPreferencesChangeListener(onPreferencesChangeListener)
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        onPreferencesChangeListener.applyPendingUpdates()
    }


    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        getDataStore().removeOnPreferencesChangeListener(onPreferencesChangeListener)
    }
}

/**
 * A functional interface for fragments to provide their specific [ObservablePreferencesDataStore] instance.
 */
fun interface DataStoreProvider {
    fun getDataStore(): ObservablePreferencesDataStore
}


/**
 * An observable [PreferenceDataStore] implementation backed by Jetpack's
 * Preferences DataStore. It notifies listeners of changes to preference values.
 */
abstract class ObservablePreferencesDataStore(
    private val store: DataStore<Preferences>
) : PreferenceDataStore() {

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main + CoroutineName("ObservablePrefDataStore"))

    @Volatile
    private var snapshot: Preferences = emptyPreferences()

    fun interface OnPreferencesChangeListener {
        fun onPreferenceChanged(key: String, newValue: Any?, oldValue: Any?)
    }

    private val listeners = CopyOnWriteArraySet<OnPreferencesChangeListener>()


    /**
     * Registers a callback to be invoked when a preference is changed.
     * Ensure to call [removeOnPreferencesChangeListener] to release the reference.
     */
    fun addOnPreferencesChangeListener(listener: OnPreferencesChangeListener) {
        listeners.add(listener)
    }

    /**
     * Un-registers a previously registered callback.
     */
    fun removeOnPreferencesChangeListener(listener: OnPreferencesChangeListener) {
        listeners.remove(listener)
    }

    override fun putString(key: String, value: String?) {
        val k = stringPreferencesKey(key)
        scope.launch {
            val oldValue = store.data.map { it[k] }.firstOrNull()
            store.edit { it[k] = value ?: "" }
            for (listener in listeners) {
                listener.onPreferenceChanged(key, value, oldValue)
            }
        }
    }

    override fun getString(key: String, defValue: String?): String? {
        val k = stringPreferencesKey(key)
        return runBlocking { (store.data.map { it[k] }.firstOrNull() ?: defValue) }
    }

    override fun putInt(key: String, value: Int) {
        val k = intPreferencesKey(key)
        scope.launch {
            val oldValue = store.data.map { it[k] }.firstOrNull()
            store.edit { it[k] = value }
            for (listener in listeners) {
                listener.onPreferenceChanged(key, value, oldValue)
            }
        }
    }

    override fun getInt(key: String, defValue: Int): Int {
        val k = intPreferencesKey(key)
        return runBlocking { store.data.map { it[k] }.firstOrNull() ?: defValue }
    }

    override fun putBoolean(key: String, value: Boolean) {
        val k = booleanPreferencesKey(key)
        scope.launch {
            val oldValue = store.data.map { it[k] }.firstOrNull()
            store.edit { it[k] = value }
            for (listener in listeners) {
                listener.onPreferenceChanged(key, value, oldValue)
            }
        }
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        val k = booleanPreferencesKey(key)
        return runBlocking { store.data.map { it[k] }.firstOrNull() ?: defValue }
    }

    override fun putFloat(key: String, value: Float) {
        val k = floatPreferencesKey(key)
        scope.launch {
            val oldValue = store.data.map { it[k] }.firstOrNull()
            store.edit { it[k] = value }
            for (listener in listeners) {
                listener.onPreferenceChanged(key, value, oldValue)
            }
        }
    }

    override fun getFloat(key: String, defValue: Float): Float {
        val k = floatPreferencesKey(key)
        return runBlocking { store.data.map { it[k] }.firstOrNull() ?: defValue }
    }

    override fun putLong(key: String, value: Long) {
        val k = longPreferencesKey(key)
        scope.launch {
            val oldValue = store.data.map { it[k] }.firstOrNull()
            store.edit { it[k] = value }
            for (listener in listeners) {
                listener.onPreferenceChanged(key, value, oldValue)
            }
        }
    }

    override fun getLong(key: String, defValue: Long): Long {
        val k = longPreferencesKey(key)
        return runBlocking { store.data.map { it[k] }.firstOrNull() ?: defValue }
    }

    override fun putStringSet(key: String, values: Set<String>?) {
        val k = stringSetPreferencesKey(key)
        scope.launch {
            val oldValue = store.data.map { it[k] }.firstOrNull()
            store.edit { it[k] = values ?: emptySet()}
            for (listener in listeners) {
                listener.onPreferenceChanged(key, values, oldValue)
            }
        }
    }

    override fun getStringSet(key: String, defValues: MutableSet<String>?): Set<String> {
        val k = stringSetPreferencesKey(key)
        @Suppress("UNCHECKED_CAST")
        return runBlocking { store.data.map { it[k] }.firstOrNull() ?: defValues } ?: emptySet()
    }
}