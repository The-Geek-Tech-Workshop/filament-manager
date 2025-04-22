package com.gtw.filamentmanager.model.domain

import androidx.compose.ui.graphics.Color
import kotlin.time.Duration

sealed interface FilamentSpool {
    val tagUID: String
    val tagFormat: TagFormat<out FilamentSpool>
}


sealed interface TagFormat<F : FilamentSpool> {
    val name: String
}

object TagFormats {
    object BAMBU : TagFormat<BambuFilamentSpool> {
        override val name: String = "Bambu"
    }
}


data class TrayInfoIndex(val materialVariantId: String, val uniqueMaterialId: String)

enum class DetailedFilamentType(val bambuName: String) {
    PLA_BASIC("PLA Basic"),
    PLA_MATTE("PLA Matte"),
    PLA_SILK("PLA Silk"),
    PLA_GALAXY("PLA Galaxy"),
    PLA_SPARKLE("PLA Sparkle"),
    SUPPORT_FOR_PLA("Support for PLA"),
    PLA_CF("PLA-CF"),
    PETG_BASIC("PETG Basic");

    companion object {
        fun fromBambuName(bambuName: String): DetailedFilamentType {
            return DetailedFilamentType.entries.find { it.bambuName == bambuName }
                ?: throw IllegalArgumentException("Unknown bambu name: $bambuName")
        }
    }
}

data class BambuFilamentSpool(
    override val tagUID: String,
    override val tagFormat: TagFormats.BAMBU = TagFormats.BAMBU,
    val trayInfoIndex: TrayInfoIndex,
    val filamentType: String,
    val detailedFilamentType: DetailedFilamentType,
    val filamentColour: Color,
    val weightInGrams: Float,
    val filamentDiameterInMillimeters: Float,
    val dryingTemperatureInCelsius: Float,
    val dryingTime: Duration,
    val bedTemperatureInCelsius: Float,
    val maxTemperatureForHotendInCelsius: Float,
    val minTemperatureForHotendInCelsius: Float,
    val spoolWidthInMicroMeters: Float,
//    val producedAt: Instant

) : FilamentSpool

