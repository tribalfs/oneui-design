package dev.oneuiproject.oneuiexample.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.sampleAppPreferences: DataStore<Preferences> by preferencesDataStore(name = "sample_app_prefs")