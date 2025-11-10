package dev.oneuiproject.oneuiexample.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.oneuiproject.oneuiexample.data.util.DarkMode
import dev.oneuiproject.oneuiexample.data.util.determineDarkMode
import dev.oneuiproject.oneuiexample.ui.preference.PreferencesFragment.Companion.PREF_AUTO_DARK_MODE
import dev.oneuiproject.oneuiexample.ui.preference.PreferencesFragment.Companion.PREF_DARK_MODE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepo @Inject constructor(@ApplicationContext appCtx: Context) {
    val dataStore = appCtx.sampleAppPreferences

    companion object {
        private  val darkModePrefKey = stringPreferencesKey(PREF_DARK_MODE)
        private  val autoDarkModePrefKey = booleanPreferencesKey(PREF_AUTO_DARK_MODE)
    }

    fun getDarkModeFlow(): Flow<DarkMode> {
        return dataStore.data.map {
            determineDarkMode(
                it[darkModePrefKey] ?: "0",
                it[autoDarkModePrefKey] == true
            )
        }
    }
}