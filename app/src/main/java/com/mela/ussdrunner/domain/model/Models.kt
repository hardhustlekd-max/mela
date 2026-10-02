package com.mela.ussdrunner.domain.model

enum class SimPreference {
    DEFAULT,
    SLOT_0,
    SLOT_1,
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class PresetSort {
    NAME,
    RECENTLY_USED,
    NEWEST,
    FAVORITES_FIRST,
}

data class Preset(
    val id: String,
    val name: String,
    val ussdCode: String,
    val description: String?,
    val category: String?,
    val simPreference: SimPreference,
    val isFavorite: Boolean,
    val isSample: Boolean,
    val createdAt: Long,
    val lastUsedAt: Long?,
)

data class AppSettings(
    val defaultSim: SimPreference = SimPreference.DEFAULT,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val confirmBeforeRun: Boolean = true,
    val showUssdOnCards: Boolean = true,
)

data class SimSlot(
    val slotIndex: Int,
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String?,
    val number: String?,
)
