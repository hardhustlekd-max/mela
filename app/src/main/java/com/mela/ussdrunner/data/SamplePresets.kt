package com.mela.ussdrunner.data

import com.mela.ussdrunner.data.repository.PresetRepository
import com.mela.ussdrunner.data.repository.SettingsRepository
import com.mela.ussdrunner.domain.model.SimPreference
import kotlinx.coroutines.flow.first

object SamplePresets {
    suspend fun seedIfNeeded(
        presets: PresetRepository,
        settings: SettingsRepository,
    ) {
        if (settings.samplesSeeded.first()) return
        if (presets.count() > 0) {
            settings.markSamplesSeeded()
            return
        }
        samples.forEach { sample ->
            presets.create(
                name = sample.name,
                ussdCode = sample.code,
                description = sample.description,
                category = sample.category,
                simPreference = SimPreference.DEFAULT,
                isFavorite = sample.favorite,
                isSample = true,
            )
        }
        settings.markSamplesSeeded()
    }

    data class Sample(
        val name: String,
        val code: String,
        val description: String,
        val category: String,
        val favorite: Boolean = false,
    )

    val samples = listOf(
        Sample(
            name = "Check Balance",
            code = "*100#",
            description = "Example — verify with your carrier",
            category = "Balance",
            favorite = true,
        ),
        Sample(
            name = "Buy Data",
            code = "*141*1#",
            description = "Example — verify with your carrier",
            category = "Data",
            favorite = true,
        ),
        Sample(
            name = "Mini Statement",
            code = "*222#",
            description = "Example — verify with your carrier",
            category = "Balance",
        ),
        Sample(
            name = "Airtime Top-up",
            code = "*101#",
            description = "Example — verify with your carrier",
            category = "Airtime",
        ),
        Sample(
            name = "Data Balance",
            code = "*100*2#",
            description = "Example — verify with your carrier",
            category = "Data",
        ),
        Sample(
            name = "Call Me Back",
            code = "*140#",
            description = "Example — verify with your carrier",
            category = "Voice",
        ),
    )
}
