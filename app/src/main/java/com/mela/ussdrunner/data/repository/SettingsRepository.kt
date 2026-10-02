package com.mela.ussdrunner.data.repository

import com.mela.ussdrunner.data.local.datastore.SettingsDataStore
import com.mela.ussdrunner.domain.model.AppSettings
import com.mela.ussdrunner.domain.model.SimPreference
import com.mela.ussdrunner.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SettingsRepository(private val store: SettingsDataStore) {
    val settings: Flow<AppSettings> = store.settings
    val samplesSeeded: Flow<Boolean> = store.samplesSeeded

    suspend fun current(): AppSettings = store.settings.first()
    suspend fun setDefaultSim(value: SimPreference) = store.setDefaultSim(value)
    suspend fun setThemeMode(value: ThemeMode) = store.setThemeMode(value)
    suspend fun setConfirmBeforeRun(value: Boolean) = store.setConfirmBeforeRun(value)
    suspend fun setShowUssdOnCards(value: Boolean) = store.setShowUssdOnCards(value)
    suspend fun markSamplesSeeded() = store.markSamplesSeeded()
}
