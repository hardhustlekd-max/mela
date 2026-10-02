package com.mela.ussdrunner.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mela.ussdrunner.domain.model.Preset
import com.mela.ussdrunner.domain.model.SimPreference

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ussdCode: String,
    val description: String?,
    val category: String?,
    val simPreference: String,
    val isFavorite: Boolean,
    val isSample: Boolean,
    val createdAt: Long,
    val lastUsedAt: Long?,
) {
    fun toDomain(): Preset = Preset(
        id = id,
        name = name,
        ussdCode = ussdCode,
        description = description,
        category = category,
        simPreference = runCatching { SimPreference.valueOf(simPreference) }
            .getOrDefault(SimPreference.DEFAULT),
        isFavorite = isFavorite,
        isSample = isSample,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
    )

    companion object {
        fun fromDomain(preset: Preset): PresetEntity = PresetEntity(
            id = preset.id,
            name = preset.name,
            ussdCode = preset.ussdCode,
            description = preset.description,
            category = preset.category,
            simPreference = preset.simPreference.name,
            isFavorite = preset.isFavorite,
            isSample = preset.isSample,
            createdAt = preset.createdAt,
            lastUsedAt = preset.lastUsedAt,
        )
    }
}
