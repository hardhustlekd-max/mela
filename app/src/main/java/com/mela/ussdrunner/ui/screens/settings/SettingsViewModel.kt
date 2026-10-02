package com.mela.ussdrunner.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mela.ussdrunner.data.repository.SettingsRepository
import com.mela.ussdrunner.domain.model.AppSettings
import com.mela.ussdrunner.domain.model.SimPreference
import com.mela.ussdrunner.domain.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: SettingsRepository,
) : ViewModel() {
    val settings = repo.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )

    fun setSim(value: SimPreference) = viewModelScope.launch { repo.setDefaultSim(value) }
    fun setTheme(value: ThemeMode) = viewModelScope.launch { repo.setThemeMode(value) }
    fun setConfirm(value: Boolean) = viewModelScope.launch { repo.setConfirmBeforeRun(value) }
    fun setShowCode(value: Boolean) = viewModelScope.launch { repo.setShowUssdOnCards(value) }

    companion object {
        fun factory(settings: SettingsRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(settings) as T
        }
    }
}
