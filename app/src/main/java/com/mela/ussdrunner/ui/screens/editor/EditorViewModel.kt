package com.mela.ussdrunner.ui.screens.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mela.ussdrunner.data.repository.PresetRepository
import com.mela.ussdrunner.domain.UssdValidator
import com.mela.ussdrunner.domain.model.SimPreference
import com.mela.ussdrunner.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val existingId: String? = null,
    val name: String = "",
    val ussdCode: String = "",
    val description: String = "",
    val category: String = "",
    val sim: SimPreference = SimPreference.DEFAULT,
    val favorite: Boolean = false,
    val nameError: String? = null,
    val codeError: String? = null,
    val loaded: Boolean = false,
    val savedId: String? = null,
)

class EditorViewModel(
    private val presetId: String?,
    private val presets: PresetRepository,
    private val widgets: WidgetUpdater,
) : ViewModel() {
    private val _state = MutableStateFlow(EditorUiState(existingId = presetId))
    val state: StateFlow<EditorUiState> = _state

    init {
        if (presetId != null) {
            viewModelScope.launch {
                val preset = presets.getById(presetId)
                if (preset != null) {
                    _state.value = EditorUiState(
                        existingId = preset.id,
                        name = preset.name,
                        ussdCode = preset.ussdCode,
                        description = preset.description.orEmpty(),
                        category = preset.category.orEmpty(),
                        sim = preset.simPreference,
                        favorite = preset.isFavorite,
                        loaded = true,
                    )
                } else {
                    _state.update { it.copy(loaded = true) }
                }
            }
        } else {
            _state.update { it.copy(loaded = true) }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value, nameError = null) }
    fun setCode(value: String) = _state.update { it.copy(ussdCode = value, codeError = null) }
    fun setDescription(value: String) = _state.update { it.copy(description = value) }
    fun setCategory(value: String) = _state.update { it.copy(category = value) }
    fun setSim(value: SimPreference) = _state.update { it.copy(sim = value) }
    fun setFavorite(value: Boolean) = _state.update { it.copy(favorite = value) }

    fun save() {
        val current = _state.value
        val name = current.name.trim()
        val nameError = if (name.isBlank()) "Give this preset a name" else null
        val codeResult = UssdValidator.validate(current.ussdCode)
        val codeError = (codeResult as? UssdValidator.ValidationResult.Invalid)?.message
        if (nameError != null || codeError != null) {
            _state.update { it.copy(nameError = nameError, codeError = codeError) }
            return
        }
        val normalized = (codeResult as UssdValidator.ValidationResult.Valid).normalized
        viewModelScope.launch {
            val id = if (current.existingId != null) {
                val existing = presets.getById(current.existingId) ?: return@launch
                presets.update(
                    existing.copy(
                        name = name,
                        ussdCode = normalized,
                        description = current.description,
                        category = current.category,
                        simPreference = current.sim,
                        isFavorite = current.favorite,
                    ),
                )
                current.existingId
            } else {
                presets.create(
                    name = name,
                    ussdCode = normalized,
                    description = current.description,
                    category = current.category,
                    simPreference = current.sim,
                    isFavorite = current.favorite,
                ).id
            }
            widgets.refreshAll()
            _state.update { it.copy(savedId = id) }
        }
    }

    companion object {
        fun factory(
            presetId: String?,
            presets: PresetRepository,
            widgets: WidgetUpdater,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                EditorViewModel(presetId, presets, widgets) as T
        }
    }
}
