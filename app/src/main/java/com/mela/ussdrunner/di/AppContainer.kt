package com.mela.ussdrunner.di

import android.content.Context
import com.mela.ussdrunner.data.local.datastore.SettingsDataStore
import com.mela.ussdrunner.data.local.db.AppDatabase
import com.mela.ussdrunner.data.repository.PresetRepository
import com.mela.ussdrunner.data.repository.SettingsRepository
import com.mela.ussdrunner.telephony.SimManager
import com.mela.ussdrunner.telephony.UssdExecutor
import com.mela.ussdrunner.widget.WidgetUpdater

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.create(appContext)
    val presetRepository = PresetRepository(db.presetDao())
    val settingsRepository = SettingsRepository(SettingsDataStore(appContext))
    val simManager = SimManager(appContext)
    val ussdExecutor = UssdExecutor(appContext, simManager)
    val widgetUpdater = WidgetUpdater(appContext)
}
