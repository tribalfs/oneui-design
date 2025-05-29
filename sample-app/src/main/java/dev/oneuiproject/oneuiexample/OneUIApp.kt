package dev.oneuiproject.oneuiexample

import android.app.Application
import androidx.preference.PreferenceManager
import dagger.hilt.android.HiltAndroidApp
import dev.oneuiproject.oneuiexample.data.util.determineDarkMode
import dev.oneuiproject.oneuiexample.ui.main.core.util.applyDarkMode
import dev.oneuiproject.oneuiexample.ui.preference.PreferencesFragment.Companion.PREF_AUTO_DARK_MODE
import dev.oneuiproject.oneuiexample.ui.preference.PreferencesFragment.Companion.PREF_DARK_MODE

@HiltAndroidApp
class OneUIApp : Application() {

    override fun onCreate() {
        super.onCreate()
        applyDarkModePrefs()
    }

    fun applyDarkModePrefs() {
        val darkMode = with(PreferenceManager.getDefaultSharedPreferences(this)) {
            determineDarkMode(getString(PREF_DARK_MODE, "0")!!,
                getBoolean(PREF_AUTO_DARK_MODE, true))
        }
        applyDarkMode(darkMode)
    }
}