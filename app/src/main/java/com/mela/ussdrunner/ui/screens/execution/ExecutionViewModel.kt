package com.mela.ussdrunner.ui.screens.execution

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mela.ussdrunner.data.repository.PresetRepository
import com.mela.ussdrunner.data.repository.SettingsRepository
import com.mela.ussdrunner.domain.model.Preset
import com.mela.ussdrunner.telephony.PermissionHelper
import com.mela.ussdrunner.telephony.UssdExecutor
import com.mela.ussdrunner.telephony.UssdResult
import com.mela.ussdrunner.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface RunPhase {
    data object Loading : RunPhase
    data object NeedPermission : RunPhase
    data object Connecting : RunPhase
    data class Success(val body: String) : RunPhase
    data class Dialer(val reason: String) : RunPhase
    data class Error(val title: String, val hints: List<String>, val canFallback: Boolean) : RunPhase
    data object Missing : RunPhase
}

data class ExecutionUiState(
    val preset: Preset? = null,
    val phase: RunPhase = RunPhase.Loading,
    val missingPermissions: List<String> = emptyList(),
)

class ExecutionViewModel(
    application: Application,
    private val presetId: String,
    private val presets: PresetRepository,
    private val settings: SettingsRepository,
    private val executor: UssdExecutor,
    private val widgets: WidgetUpdater,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ExecutionUiState())
    val state: StateFlow<ExecutionUiState> = _state

    init {
        viewModelScope.launch {
            val preset = presets.getById(presetId)
            if (preset == null) {
                _state.value = ExecutionUiState(phase = RunPhase.Missing)
                return@launch
            }
            _state.value = ExecutionUiState(preset = preset)
            start()
        }
    }

    fun onPermissionsUpdated() = start()

    fun start() {
        val app = getApplication<Application>()
        val missing = PermissionHelper.missing(app)
        if (missing.contains(android.Manifest.permission.CALL_PHONE)) {
            _state.update {
                it.copy(
                    phase = RunPhase.NeedPermission,
                    missingPermissions = missing,
                )
            }
            return
        }
        val preset = _state.value.preset ?: return
        viewModelScope.launch {
            _state.update { it.copy(phase = RunPhase.Connecting, missingPermissions = missing) }
            val prefs = settings.current()
            val result = executor.execute(
                code = preset.ussdCode,
                preference = preset.simPreference,
                settingsDefault = prefs.defaultSim,
                preferNative = true,
            )
            applyResult(result)
        }
    }

    fun fallback() {
        val preset = _state.value.preset ?: return
        viewModelScope.launch {
            _state.update { it.copy(phase = RunPhase.Connecting) }
            val prefs = settings.current()
            val result = executor.execute(
                code = preset.ussdCode,
                preference = preset.simPreference,
                settingsDefault = prefs.defaultSim,
                preferNative = false,
            )
            applyResult(result)
        }
    }

    private suspend fun applyResult(result: UssdResult) {
        when (result) {
            is UssdResult.Response -> {
                presets.markUsed(presetId)
                widgets.refreshAll()
                _state.update { it.copy(phase = RunPhase.Success(result.body)) }
            }
            is UssdResult.DialerLaunched -> {
                presets.markUsed(presetId)
                widgets.refreshAll()
                _state.update { it.copy(phase = RunPhase.Dialer(result.reason)) }
            }
            is UssdResult.Failure -> {
                _state.update {
                    it.copy(phase = RunPhase.Error(result.title, result.hints, result.canFallback))
                }
            }
        }
    }

    companion object {
        fun factory(
            application: Application,
            presetId: String,
            presets: PresetRepository,
            settings: SettingsRepository,
            executor: UssdExecutor,
            widgets: WidgetUpdater,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ExecutionViewModel(application, presetId, presets, settings, executor, widgets) as T
        }
    }
}
