package com.gtw.filamentmanager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gtw.filamentmanager.model.domain.BambuFilamentSpool
import com.gtw.filamentmanager.model.domain.FilamentSpool

@Composable
fun FilamentScannerScreen(modifier: Modifier = Modifier, filament: FilamentSpool?) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (filament == null) {
            Text(
                text = "Ready to scan",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        } else {
            QuickTextField("RFID Format", filament.tagFormat.name)
            when (filament) {
                is BambuFilamentSpool -> {
                    Row(modifier = Modifier.padding(0.dp)) {
                        Column(modifier = Modifier.weight(3f)) {
                            QuickTextField("Filament type", filament.filamentType)
                            QuickTextField(
                                "Detailed filament type",
                                filament.detailedFilamentType.bambuName
                            )
                        }
                        FilamentColor(
                            filament.filamentColour,
                            modifier = Modifier
                                .weight(1f)
                                .size(110.dp)
                        )
                    }
                    QuickTextField("Material variant ID", filament.trayInfoIndex.materialVariantId)
                    QuickTextField("Unique material ID", filament.trayInfoIndex.uniqueMaterialId)
                    QuickTextField("Weight", "${filament.weightInGrams}g")
                    QuickTextField("Diameter", "${filament.filamentDiameterInMillimeters}mm")
                    QuickTextField(
                        "Drying temperature",
                        "${filament.dryingTemperatureInCelsius}°C"
                    )
                    QuickTextField("Drying time", filament.dryingTime.toString())
                    QuickTextField(
                        "Bed temperature",
                        "${filament.bedTemperatureInCelsius}°C"
                    )
                    QuickTextField(
                        "Max temperature for hotend",
                        "${filament.maxTemperatureForHotendInCelsius}°C"
                    )
                    QuickTextField(
                        "Min temperature for hotend",
                        "${filament.minTemperatureForHotendInCelsius}°C"
                    )
                    QuickTextField("Spool width", "${filament.spoolWidthInMicroMeters}μm")

                }

            }
        }
    }
}