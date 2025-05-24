package dev.oneuiproject.oneuiexample.ui.main.core.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import dev.oneuiproject.oneuiexample.data.util.DarkMode

/**
 * Applies night mode preference to the app components
 */
fun applyDarkMode(darkModeOption: DarkMode) {
    when (darkModeOption) {
        DarkMode.AUTO -> AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
        DarkMode.DISABLED -> AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
        DarkMode.ENABLED ->  AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
    }
}