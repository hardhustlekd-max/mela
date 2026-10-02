package com.mela.ussdrunner.data.repository

import com.mela.ussdrunner.data.local.db.PresetDao
import com.mela.ussdrunner.data.local.db.PresetEntity
import com.mela.ussdrunner.domain.UssdValidator
import com.mela.ussdrunner.domain.model.Preset
import com.mela.ussdrunner.domain.model.PresetSort
import com.mela.ussdrunner.domain.model.SimPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class PresetRepository(private val dao: PresetDao) {

    fun observeAll(): Flow<List<Preset>> = dao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    fun observeById(id: String): Flow<Preset?> = dao.observeById(id).map { it?.toDomain() }

    suspend fun getById(id: String): Preset? = dao.getById(id)?.toDomain()

    suspend fun save(preset: Preset) {
        dao.upsert(PresetEntity.fromDomain(preset.copy(ussdCode = UssdValidator.normalize(preset.ussdCode))))
    }

    suspend fun create(
        name: String,
        ussdCode: String,
        description: String?,
        category: String?,
        simPreference: SimPreference,
        isFavorite: Boolean,
        isSample: Boolean = false,
        now: Long = System.currentTimeMillis(),
    ): Preset {
        val preset = Preset(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            ussdCode = UssdValidator.normalize(ussdCode),
            description = description?.trim()?.ifBlank { null },
            category = category?.trim()?.ifBlank { null },
            simPreference = simPreference,
            isFavorite = isFavorite,
            isSample = isSample,
            createdAt = now,
            lastUsedAt = null,
        )
        save(preset)
        return preset
    }

    suspend fun update(preset: Preset) {
        save(
            preset.copy(
                name = preset.name.trim(),
                ussdCode = UssdValidator.normalize(preset.ussdCode),
                description = preset.description?.trim()?.ifBlank { null },
                category = preset.category?.trim()?.ifBlank { null },
            ),
        )
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    suspend fun duplicate(id: String): Preset? {
        val original = getById(id) ?: return null
        return create(
            name = "${original.name} (copy)",
            ussdCode = original.ussdCode,
            description = original.description,
            category = original.category,
            simPreference = original.simPreference,
            isFavorite = false,
            isSample = false,
        )
    }

    suspend fun setFavorite(id: String, favorite: Boolean) {
        val current = getById(id) ?: return
        save(current.copy(isFavorite = favorite))
    }

    suspend fun markUsed(id: String, at: Long = System.currentTimeMillis()) {
        val current = getById(id) ?: return
        save(current.copy(lastUsedAt = at))
    }

    suspend fun count(): Int = dao.count()

    companion object {
        fun search(presets: List<Preset>, query: String): List<Preset> {
            val q = query.trim()
            if (q.isEmpty()) return presets
            return presets.filter { preset ->
                preset.name.contains(q, ignoreCase = true) ||
                    preset.ussdCode.contains(q, ignoreCase = true) ||
                    (preset.description?.contains(q, ignoreCase = true) == true) ||
                    (preset.category?.contains(q, ignoreCase = true) == true)
            }
        }

        fun filterByCategory(presets: List<Preset>, category: String?): List<Preset> {
            if (category.isNullOrBlank()) return presets
            return presets.filter { it.category.equals(category, ignoreCase = true) }
        }

        fun sort(presets: List<Preset>, sort: PresetSort): List<Preset> = when (sort) {
            PresetSort.NAME -> presets.sortedBy { it.name.lowercase() }
            PresetSort.RECENTLY_USED -> presets.sortedWith(
                compareByDescending<Preset> { it.lastUsedAt ?: 0L }.thenBy { it.name.lowercase() },
            )
            PresetSort.NEWEST -> presets.sortedByDescending { it.createdAt }
            PresetSort.FAVORITES_FIRST -> presets.sortedWith(
                compareByDescending<Preset> { it.isFavorite }.thenBy { it.name.lowercase() },
            )
        }
    }
}
