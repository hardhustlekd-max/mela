package com.mela.ussdrunner.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mela.ussdrunner.data.repository.PresetRepository
import com.mela.ussdrunner.data.repository.SettingsRepository
import com.mela.ussdrunner.domain.model.AppSettings
import com.mela.ussdrunner.domain.model.Preset
import com.mela.ussdrunner.domain.model.PresetSort
import com.mela.ussdrunner.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val presets: List<Preset> = emptyList(),
    val favorites: List<Preset> = emptyList(),
    val recent: List<Preset> = emptyList(),
    val others: List<Preset> = emptyList(),
    val categories: List<String> = emptyList(),
    val query: String = "",
    val category: String? = null,
    val favoritesOnly: Boolean = false,
    val recentOnly: Boolean = false,
    val sort: PresetSort = PresetSort.FAVORITES_FIRST,
    val settings: AppSettings = AppSettings(),
    val pendingDelete: Preset? = null,
    val pendingRun: Preset? = null,
)

private data class HomeFilters(
    val query: String,
    val category: String?,
    val favoritesOnly: Boolean,
    val recentOnly: Boolean,
    val sort: PresetSort,
)

class HomeViewModel(
    private val presets: PresetRepository,
    private val settingsRepo: SettingsRepository,
    private val widgets: WidgetUpdater,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<String?>(null)
    private val favoritesOnly = MutableStateFlow(false)
    private val recentOnly = MutableStateFlow(false)
    private val sort = MutableStateFlow(PresetSort.FAVORITES_FIRST)
    private val pendingDelete = MutableStateFlow<Preset?>(null)
    private val pendingRun = MutableStateFlow<Preset?>(null)

    private val filters = combine(query, category, favoritesOnly, recentOnly, sort) {
            q, cat, fav, rec, s ->
        HomeFilters(q, cat, fav, rec, s)
    }

    val state = combine(
        presets.observeAll(),
        settingsRepo.settings,
        filters,
        pendingDelete,
        pendingRun,
    ) { all, appSettings, filter, del, run ->
        val searched = PresetRepository.search(all, filter.query)
        val categorized = PresetRepository.filterByCategory(searched, filter.category)
        val filtered = when {
            filter.favoritesOnly -> categorized.filter { it.isFavorite }
            filter.recentOnly -> categorized.filter { it.lastUsedAt != null }
            else -> categorized
        }
        val sorted = PresetRepository.sort(filtered, filter.sort)
        val favorites = all.filter { it.isFavorite }.sortedBy { it.name.lowercase() }
        val favoriteIds = favorites.map { it.id }.toSet()
        val recent = all.filter { it.lastUsedAt != null && it.id !in favoriteIds }
            .sortedByDescending { it.lastUsedAt }
            .take(8)
        val recentIds = recent.map { it.id }.toSet()
        HomeUiState(
            loading = false,
            presets = sorted,
            favorites = favorites,
            recent = recent,
            others = all.filter { it.id !in favoriteIds && it.id !in recentIds }
                .sortedBy { it.name.lowercase() },
            categories = all.mapNotNull { it.category }.distinct().sorted(),
            query = filter.query,
            category = filter.category,
            favoritesOnly = filter.favoritesOnly,
            recentOnly = filter.recentOnly,
            sort = filter.sort,
            settings = appSettings,
            pendingDelete = del,
            pendingRun = run,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setQuery(value: String) { query.value = value }
    fun setCategory(value: String?) {
        category.value = value
        favoritesOnly.value = false
        recentOnly.value = false
    }
    fun showFavorites() {
        favoritesOnly.value = true
        recentOnly.value = false
        category.value = null
    }
    fun showRecent() {
        recentOnly.value = true
        favoritesOnly.value = false
        category.value = null
    }
    fun showAll() {
        favoritesOnly.value = false
        recentOnly.value = false
        category.value = null
    }
    fun setSort(value: PresetSort) { sort.value = value }
    fun requestDelete(preset: Preset) { pendingDelete.value = preset }
    fun cancelDelete() { pendingDelete.value = null }
    fun requestRun(preset: Preset) { pendingRun.value = preset }
    fun cancelRun() { pendingRun.value = null }

    fun confirmDelete() {
        val target = pendingDelete.value ?: return
        viewModelScope.launch {
            presets.delete(target.id)
            widgets.refreshAll()
            pendingDelete.value = null
        }
    }

    fun toggleFavorite(preset: Preset) {
        viewModelScope.launch {
            presets.setFavorite(preset.id, !preset.isFavorite)
            widgets.refreshAll()
        }
    }

    fun duplicate(preset: Preset) {
        viewModelScope.launch {
            presets.duplicate(preset.id)
            widgets.refreshAll()
        }
    }

    companion object {
        fun factory(
            presets: PresetRepository,
            settings: SettingsRepository,
            widgets: WidgetUpdater,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(presets, settings, widgets) as T
        }
    }
}
