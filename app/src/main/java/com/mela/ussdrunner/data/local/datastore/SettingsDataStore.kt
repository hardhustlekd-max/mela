package com.mela.ussdrunner.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mela.ussdrunner.domain.model.AppSettings
import com.mela.ussdrunner.domain.model.SimPreference
import com.mela.ussdrunner.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "mela_settings")

class SettingsDataStore(private val context: Context) {
    private val defaultSimKey = stringPreferencesKey("default_sim")
    private val themeKey = stringPreferencesKey("theme_mode")
    private val confirmKey = booleanPreferencesKey("confirm_before_run")
    private val showCodeKey = booleanPreferencesKey("show_ussd_on_cards")
    private val seededKey = booleanPreferencesKey("samples_seeded")

    val settings: Flow<AppSettings> = context.settingsStore.data.map { prefs ->
        AppSettings(
            defaultSim = prefs[defaultSimKey]?.let {
                runCatching { SimPreference.valueOf(it) }.getOrNull()
            } ?: SimPreference.DEFAULT,
            themeMode = prefs[themeKey]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrNull()
            } ?: ThemeMode.SYSTEM,
            confirmBeforeRun = prefs[confirmKey] ?: true,
            showUssdOnCards = prefs[showCodeKey] ?: true,
        )
    }

    val samplesSeeded: Flow<Boolean> = context.settingsStore.data.map { it[seededKey] ?: false }

    suspend fun setDefaultSim(value: SimPreference) {
        context.settingsStore.edit { it[defaultSimKey] = value.name }
    }

    suspend fun setThemeMode(value: ThemeMode) {
        context.settingsStore.edit { it[themeKey] = value.name }
    }

    suspend fun setConfirmBeforeRun(value: Boolean) {
        context.settingsStore.edit { it[confirmKey] = value }
    }

    suspend fun setShowUssdOnCards(value: Boolean) {
        context.settingsStore.edit { it[showCodeKey] = value }
    }

    suspend fun markSamplesSeeded() {
        context.settingsStore.edit { it[seededKey] = true }
    }
}
