package com.mela.ussdrunner.data

import com.mela.ussdrunner.data.repository.PresetRepository
import com.mela.ussdrunner.domain.model.Preset
import com.mela.ussdrunner.domain.model.PresetSort
import com.mela.ussdrunner.domain.model.SimPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetLogicTest {
    private fun preset(
        id: String,
        name: String,
        code: String,
        favorite: Boolean = false,
        category: String? = "Data",
        created: Long = 1,
        used: Long? = null,
    ) = Preset(
        id = id,
        name = name,
        ussdCode = code,
        description = "Example — verify with your carrier",
        category = category,
        simPreference = SimPreference.DEFAULT,
        isFavorite = favorite,
        isSample = true,
        createdAt = created,
        lastUsedAt = used,
    )

    private val data = listOf(
        preset("1", "Ethio Telecom Data", "*999*1*2#", favorite = true, created = 10, used = 50),
        preset("2", "Balance", "*804#", category = "Balance", created = 20, used = 80),
        preset("3", "Voice Package", "*999*2*1#", favorite = false, category = "Voice", created = 30),
        preset("4", "Mobile Money", "*127#", category = "Mobile Money", created = 40, used = 10),
    )

    @Test
    fun searchMatchesNameAndCode() {
        assertEquals(1, PresetRepository.search(data, "balance").size)
        assertEquals(2, PresetRepository.search(data, "*999").size)
        assertEquals(1, PresetRepository.search(data, "*804#").size)
        assertTrue(PresetRepository.search(data, "nope").isEmpty())
    }

    @Test
    fun categoryFilterIsCaseInsensitive() {
        assertEquals(1, PresetRepository.filterByCategory(data, "voice").size)
        assertEquals(data.size, PresetRepository.filterByCategory(data, null).size)
    }

    @Test
    fun sortFavoritesFirstThenName() {
        val sorted = PresetRepository.sort(data, PresetSort.FAVORITES_FIRST)
        assertEquals("Ethio Telecom Data", sorted.first().name)
    }

    @Test
    fun sortRecentlyUsedPutsLatestFirst() {
        val sorted = PresetRepository.sort(data, PresetSort.RECENTLY_USED)
        assertEquals("Balance", sorted.first().name)
    }

    @Test
    fun sortNewestUsesCreatedDate() {
        val sorted = PresetRepository.sort(data, PresetSort.NEWEST)
        assertEquals("Mobile Money", sorted.first().name)
    }

    @Test
    fun widgetSelectionKeepsChosenIdsInOrder() {
        val chosen = listOf("2", "1", "4")
        val selected = chosen.mapNotNull { id -> data.find { it.id == id } }
        assertEquals(listOf("Balance", "Ethio Telecom Data", "Mobile Money"), selected.map { it.name })
        assertEquals(3, selected.size)
    }
}
