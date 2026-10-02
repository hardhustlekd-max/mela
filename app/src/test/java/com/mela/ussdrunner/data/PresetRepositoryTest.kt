package com.mela.ussdrunner.data

import com.mela.ussdrunner.data.local.db.PresetDao
import com.mela.ussdrunner.data.local.db.PresetEntity
import com.mela.ussdrunner.data.repository.PresetRepository
import com.mela.ussdrunner.domain.model.SimPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetRepositoryTest {
    private fun repo() = PresetRepository(InMemoryPresetDao())

    @Test
    fun createPersistsNormalizedCode() = runTest {
        val repo = repo()
        val preset = repo.create(
            name = "  Data  ",
            ussdCode = " *999*1*2# ",
            description = "  Example — verify with your carrier  ",
            category = " Data ",
            simPreference = SimPreference.SLOT_0,
            isFavorite = true,
        )
        val stored = repo.getById(preset.id)!!
        assertEquals("Data", stored.name)
        assertEquals("*999*1*2#", stored.ussdCode)
        assertEquals("Example — verify with your carrier", stored.description)
        assertEquals("Data", stored.category)
        assertTrue(stored.isFavorite)
        assertEquals(SimPreference.SLOT_0, stored.simPreference)
        assertEquals(1, repo.count())
    }

    @Test
    fun editUpdatesFields() = runTest {
        val repo = repo()
        val preset = repo.create(
            name = "Balance",
            ussdCode = "*804#",
            description = null,
            category = "Balance",
            simPreference = SimPreference.DEFAULT,
            isFavorite = false,
        )
        repo.update(
            preset.copy(
                name = "Airtime balance",
                ussdCode = "*805#",
                description = "Check remaining credit",
                isFavorite = true,
            ),
        )
        val stored = repo.getById(preset.id)!!
        assertEquals("Airtime balance", stored.name)
        assertEquals("*805#", stored.ussdCode)
        assertEquals("Check remaining credit", stored.description)
        assertTrue(stored.isFavorite)
    }

    @Test
    fun deleteRemovesPreset() = runTest {
        val repo = repo()
        val preset = repo.create(
            name = "Voice",
            ussdCode = "*999*2*1#",
            description = null,
            category = "Voice",
            simPreference = SimPreference.DEFAULT,
            isFavorite = false,
        )
        repo.delete(preset.id)
        assertNull(repo.getById(preset.id))
        assertEquals(0, repo.count())
        assertTrue(repo.observeAll().first().isEmpty())
    }

    @Test
    fun favoriteToggleAndDuplicate() = runTest {
        val repo = repo()
        val preset = repo.create(
            name = "Mobile Money",
            ussdCode = "*127#",
            description = null,
            category = "Mobile Money",
            simPreference = SimPreference.SLOT_1,
            isFavorite = false,
        )
        repo.setFavorite(preset.id, true)
        assertTrue(repo.getById(preset.id)!!.isFavorite)
        repo.setFavorite(preset.id, false)
        assertFalse(repo.getById(preset.id)!!.isFavorite)

        val copy = repo.duplicate(preset.id)!!
        assertNotEquals(preset.id, copy.id)
        assertEquals("Mobile Money (copy)", copy.name)
        assertEquals("*127#", copy.ussdCode)
        assertEquals(SimPreference.SLOT_1, copy.simPreference)
        assertFalse(copy.isFavorite)
        assertFalse(copy.isSample)
        assertEquals(2, repo.count())
    }

    @Test
    fun markUsedSetsLastUsedDate() = runTest {
        val repo = repo()
        val preset = repo.create(
            name = "Data",
            ussdCode = "*999*1*2#",
            description = null,
            category = "Data",
            simPreference = SimPreference.DEFAULT,
            isFavorite = false,
        )
        assertNull(repo.getById(preset.id)!!.lastUsedAt)
        repo.markUsed(preset.id, at = 1_700_000_000_000L)
        assertEquals(1_700_000_000_000L, repo.getById(preset.id)!!.lastUsedAt)
    }
}

private class InMemoryPresetDao : PresetDao {
    private val items = MutableStateFlow<Map<String, PresetEntity>>(emptyMap())

    override fun observeAll(): Flow<List<PresetEntity>> =
        items.map { it.values.sortedBy { entity -> entity.name.lowercase() } }

    override suspend fun getById(id: String): PresetEntity? = items.value[id]

    override fun observeById(id: String): Flow<PresetEntity?> = items.map { it[id] }

    override suspend fun upsert(entity: PresetEntity) {
        items.value = items.value + (entity.id to entity)
    }

    override suspend fun upsertAll(entities: List<PresetEntity>) {
        items.value = items.value + entities.associateBy { it.id }
    }

    override suspend fun update(entity: PresetEntity) = upsert(entity)

    override suspend fun delete(entity: PresetEntity) {
        items.value = items.value - entity.id
    }

    override suspend fun deleteById(id: String) {
        items.value = items.value - id
    }

    override suspend fun count(): Int = items.value.size
}
