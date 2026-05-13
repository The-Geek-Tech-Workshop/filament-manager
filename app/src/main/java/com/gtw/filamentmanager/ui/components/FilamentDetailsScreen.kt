package com.gtw.filamentmanager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gtw.filamentmanager.model.domain.EmptyFilamentTray
import com.gtw.filamentmanager.model.domain.ExternalSpool
import com.gtw.filamentmanager.model.domain.FilamentTray
import com.gtw.filamentmanager.model.domain.SpooledFilamentTray

@Composable
fun QuickTextField(label: String, value: String?, modifier: Modifier = Modifier) {
    TextField(
        value = TextFieldValue(value ?: ""),
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp)
    )
}

@Composable
fun FilamentDetailsScreen(
    modifier: Modifier = Modifier,
    filamentTray: FilamentTray
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val spooledTray = when (filamentTray) {
            is EmptyFilamentTray -> null
            is SpooledFilamentTray -> filamentTray
        }
        Row(modifier = Modifier.padding(0.dp)) {
            Column(modifier = Modifier.weight(3f)) {
                QuickTextField("Material", spooledTray?.type)
                QuickTextField("Weight", spooledTray?.weight?.let { "${it}g" })
            }
            FilamentColor(
                spooledTray?.color ?: Color.Unspecified,
                modifier = Modifier
                    .weight(1f)
                    .size(110.dp)
            )
        }
        QuickTextField("Diameter", spooledTray?.diameter?.let { "${it}mm" })
        QuickTextField("Temperature", spooledTray?.temperature?.let { "${it}°C" })
        QuickTextField("Time", spooledTray?.time?.let { "${it}s" })
        QuickTextField("Bed Temperature", spooledTray?.bedTemperature?.let { "${it}°C" })
        QuickTextField(
            "Nozzle Temperature Min",
            spooledTray?.nozzleTemperatureMinimumInCelsius?.let { "${it}°C" })
        QuickTextField(
            "Nozzle Temperature Max",
            spooledTray?.nozzleTemperatureMaximumInCelsius?.let { "${it}°C" })

    }
}

@Composable
@Preview
fun PreviewFilamentDetailsScreen() {
    FilamentDetailsScreen(
        filamentTray = SpooledFilamentTray(
            location = ExternalSpool,
            tagUID = "1234567890",
            type = "PLA",
            color = Color.Red,
            weight = 100f,
            diameter = 10f,
            temperature = 20f,
            time = 30f,
            bedTemperature = 30f,
            nozzleTemperatureMaximumInCelsius = 30f,
            nozzleTemperatureMinimumInCelsius = 30f
        )
    )
}