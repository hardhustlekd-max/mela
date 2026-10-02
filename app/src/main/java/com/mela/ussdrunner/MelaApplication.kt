package com.mela.ussdrunner

import android.app.Application
import com.mela.ussdrunner.data.SamplePresets
import com.mela.ussdrunner.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MelaApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applicationScope.launch {
            SamplePresets.seedIfNeeded(container.presetRepository, container.settingsRepository)
        }
    }
}
