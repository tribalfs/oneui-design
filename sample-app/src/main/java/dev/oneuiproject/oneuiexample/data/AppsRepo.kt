package dev.oneuiproject.oneuiexample.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.apppickerview.widget.AppPickerView
import androidx.apppickerview.widget.AppPickerView.AppPickerType
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import dev.oneuiproject.oneuiexample.data.util.getInstalledPackagesCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class App(
    val packageName: String,
    val name: String,
    val isSystemApp: Boolean
)

data class AppsPreference(
    val showSystem: Boolean,
    @AppPickerType val appPickerType: Int
)

class AppsRepo (
    private val context: Context
) {
    private val dataStore: DataStore<Preferences> = context.sampleAppPreferences

    val appsFlow: Flow<List<App>> get() = flow { emit(getInstalledPackageNames()) }.flowOn(
        Dispatchers.IO)

    /**
     * Opted to source installed apps here instead of using list from AppPickerView itself.
     * [AppPickerView] can automatically provide list of installed apps
     * but no ability to filter out system apps
    */
    private suspend fun getInstalledPackageNames(): List<App> = withContext(Dispatchers.IO) {
        return@withContext context.packageManager
            .getInstalledPackagesCompat(PackageManager.GET_META_DATA)
            .map {
                App(
                    packageName = it.packageName,
                    name = it.applicationInfo?.loadLabel(context.packageManager).toString(),
                    isSystemApp = (it.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) ?: 1) != 0
                )
            }
    }

    suspend fun setShowSystemApps(show: Boolean){
        dataStore.edit {
            it[PREF_SHOW_SYSTEM] = show
        }
    }
    suspend fun setPickerType(@AppPickerType appPickerType: Int){
        dataStore.edit {
            it[PREF_PICKER_TYPE] = appPickerType
        }
    }

    val appPreferenceFlow: Flow<AppsPreference> get() = dataStore.data.map {
        AppsPreference(
            it[PREF_SHOW_SYSTEM] ?: false,
            it[PREF_PICKER_TYPE] ?: AppPickerView.TYPE_LIST
        )
    }

    private companion object{
        val PREF_SHOW_SYSTEM = booleanPreferencesKey("showSystem")
        val PREF_PICKER_TYPE = intPreferencesKey("pickerType")
    }
}

