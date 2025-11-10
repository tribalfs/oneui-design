package dev.oneuiproject.oneuiexample

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.oneuiproject.oneuiexample.data.SettingsRepo
import dev.oneuiproject.oneuiexample.ui.main.core.util.applyDarkMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class OneUIApp : Application() {

    @Inject
    lateinit var settingsRepo: SettingsRepo

    override fun onCreate() {
        super.onCreate()
        runBlocking { applyDarkModePrefs() }
    }

    suspend fun applyDarkModePrefs() {
        applyDarkMode(settingsRepo.getDarkModeFlow().first())
    }
}