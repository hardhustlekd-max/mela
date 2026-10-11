package com.mela.ussdrunner.widget

import com.mela.ussdrunner.domain.model.Preset
import com.mela.ussdrunner.domain.model.SimPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetGridLogicTest {
    private fun preset(id: String, name: String) = Preset(
        id = id,
        name = name,
        ussdCode = "*100#",
        description = null,
        category = "Balance",
        simPreference = SimPreference.DEFAULT,
        isFavorite = false,
        isSample = true,
        createdAt = 1,
        lastUsedAt = null,
    )

    @Test
    fun emptySavedSlotsFallBackToFirstSixPresets() {
        val all = (1..8).map { preset("$it", "P$it") }
        val filled = fillGridCells(List(6) { null }, all)
        assertEquals(listOf("P1", "P2", "P3", "P4", "P5", "P6"), filled.map { it?.name })
    }

    @Test
    fun configuredSlotsAreKeptEvenWithGaps() {
        val a = preset("a", "A")
        val c = preset("c", "C")
        val saved = listOf(a, null, c, null, null, null)
        val filled = fillGridCells(saved, listOf(a, c, preset("z", "Z")))
        assertEquals("A", filled[0]?.name)
        assertNull(filled[1])
        assertEquals("C", filled[2]?.name)
    }

    @Test
    fun resolveGridMapsSavedIdsAndFallsBackWhenEmpty() {
        val all = (1..8).map { preset("$it", "P$it") }
        val empty = WidgetKeys.resolveGrid(List(6) { null }, all)
        assertEquals(listOf("P1", "P2", "P3", "P4", "P5", "P6"), empty.map { it?.name })

        val saved = WidgetKeys.resolveGrid(listOf("3", null, "1", null, null, null), all)
        assertEquals("P3", saved[0]?.name)
        assertNull(saved[1])
        assertEquals("P1", saved[2]?.name)
    }

    @Test
    fun toggleKeepsTapOrderAndStopsAtSix() {
        var ids = emptyList<String>()
        ids = toggleWidgetSelection(ids, "b", max = 6)
        ids = toggleWidgetSelection(ids, "a", max = 6)
        ids = toggleWidgetSelection(ids, "c", max = 6)
        assertEquals(listOf("b", "a", "c"), ids)

        ids = toggleWidgetSelection(ids, "a", max = 6)
        assertEquals(listOf("b", "c"), ids)

        ids = listOf("1", "2", "3", "4", "5", "6")
        assertEquals(ids, toggleWidgetSelection(ids, "7", max = 6))
    }

    @Test
    fun orderedWidgetPresetsFollowsChosenIds() {
        val all = listOf(
            preset("1", "Data"),
            preset("2", "Balance"),
            preset("4", "Money"),
        )
        val ordered = orderedWidgetPresets(listOf("2", "1", "4", "9"), all, max = 6)
        assertEquals(listOf("Balance", "Data", "Money"), ordered.map { it.name })
    }
}
