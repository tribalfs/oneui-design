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
import dev.oneuiproject.oneui.preference.ColorPickerPreference
import dev.oneuiproject.oneui.preference.HorizontalRadioPreference
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
 * - **Cached Reads:** Uses an in-memory snapshot of DataStore preferences to serve
 *   read requests instantly without blocking the main thread. This allows the Preferences
 *   UI to load synchronously while still reflecting the latest DataStore values.
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
 * - **Writes:** All write operations (`put*` methods) in [ObservablePreferencesDataStore]
 *   are performed asynchronously on `Dispatchers.IO`, ensuring that disk I/O never blocks
 *   the main thread. After each successful write, preference change notifications are
 *   dispatched on the main thread to keep the UI in sync.
 *
 * - **Reads:** All reads are served from a **cached in-memory snapshot** of the
 *   [Preferences] object, updated automatically via a background coroutine.
 *
 * - **Memory Impact:** The snapshot cache holds a single immutable [Preferences]
 *   instance in memory. This overhead is minimal for typical key–value pairs and
 *   does not grow with time. Large data payloads should be avoided in Preferences
 *   DataStore and stored in files or Proto DataStore instead.
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
        when (pref) {
            is TwoStatePreference -> pref.isChecked = value as? Boolean ?: false
            is SeekBarPreference -> (value as? Int)?.let { pref.value = it }
            is EditTextPreference -> pref.text = value as? String
            is ListPreference -> pref.value = value as? String
            is ColorPickerPreference -> (value as? Int)?.let { pref.onColorSet(it) }
            is MultiSelectListPreference -> pref.values = value as? Set<String> ?: emptySet()
            is HorizontalRadioPreference -> pref.value = value as? String
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
        CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("ObservablePrefDataStore"))

    @Volatile
    private var snapshot: Preferences = emptyPreferences()

    fun interface OnPreferencesChangeListener {
        fun onPreferenceChanged(key: String, newValue: Any?, oldValue: Any?)
    }

    private val listeners = CopyOnWriteArraySet<OnPreferencesChangeListener>()

    init {
        // Keep an up-to-date snapshot and notify listeners only for changed keys.
        @OptIn(ExperimentalCoroutinesApi::class)
        scope.launch {
            var previous: Preferences = emptyPreferences()
            store.data
                .distinctUntilChanged()
                .collect { current ->
                    // compute changed keys
                    val changedKeys = mutableListOf<Preferences.Key<*>>()
                    val prevKeys = previous.asMap().keys
                    val currKeys = current.asMap().keys
                    val allKeys = HashSet<Preferences.Key<*>>(prevKeys.size + currKeys.size)
                    allKeys.addAll(prevKeys)
                    allKeys.addAll(currKeys)

                    for (k in allKeys) {
                        val prevVal = previous[k]
                        val currVal = current[k]
                        if (prevVal != currVal) {
                            changedKeys.add(k)
                        }
                    }

                    // keep a reference to the old snapshot before overwriting
                    val oldSnapshot = previous

                    // swap snapshot
                    snapshot = current
                    previous = current

                    if (changedKeys.isNotEmpty()) {
                        val notifications = changedKeys.map { key ->
                            Triple(key.name, current[key], oldSnapshot[key])
                        }
                        val listenersSnapshot = listeners.toList()

                        // switch once to Main and notify all listeners
                        withContext(Dispatchers.Main) {
                            for ((name, newValue, oldValue) in notifications) {
                                for (listener in listenersSnapshot) {
                                    listener.onPreferenceChanged(name, newValue, oldValue)
                                }
                            }
                        }
                    }
                }
        }
    }

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
            store.edit { it[k] = value ?: "" }
        }
    }

    override fun getString(key: String, defValue: String?): String {
        val k = stringPreferencesKey(key)
        return snapshot[k] ?: defValue ?: ""
    }

    override fun putInt(key: String, value: Int) {
        val k = intPreferencesKey(key)
        scope.launch { store.edit { it[k] = value } }
    }

    override fun getInt(key: String, defValue: Int): Int {
        val k = intPreferencesKey(key)
        return snapshot[k] ?: defValue
    }

    override fun putBoolean(key: String, value: Boolean) {
        val k = booleanPreferencesKey(key)
        scope.launch { store.edit { it[k] = value } }
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        val k = booleanPreferencesKey(key)
        return snapshot[k] ?: defValue
    }

    override fun putFloat(key: String, value: Float) {
        val k = floatPreferencesKey(key)
        scope.launch { store.edit { it[k] = value } }
    }

    override fun getFloat(key: String, defValue: Float): Float {
        val k = floatPreferencesKey(key)
        return snapshot[k] ?: defValue
    }

    override fun putLong(key: String, value: Long) {
        val k = longPreferencesKey(key)
        scope.launch { store.edit { it[k] = value } }
    }

    override fun getLong(key: String, defValue: Long): Long {
        val k = longPreferencesKey(key)
        return snapshot[k] ?: defValue
    }

    override fun putStringSet(key: String, values: Set<String>?) {
        val k = stringSetPreferencesKey(key)
        scope.launch { store.edit { it[k] = values ?: emptySet() } }
    }

    override fun getStringSet(key: String, defValues: MutableSet<String>?): Set<String> {
        val k = stringSetPreferencesKey(key)
        @Suppress("UNCHECKED_CAST")
        return snapshot[k] as? Set<String> ?: (defValues ?: emptySet())
    }
}