package com.gtw.filamentmanager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.gtw.filamentmanager.ui.PrinterState
import com.gtw.filamentmanager.ui.navigation.Destination.Companion.FilamentScanner
import com.gtw.filamentmanager.ui.navigation.Destination.Companion.PrinterList

@ExperimentalMaterial3Api
@Composable
fun AppTopNavBar(
    destination: Destination,
    navController: NavHostController,
    printerState: PrinterState
) {
    TopAppBar(
        title = {
            Text(
                text = destination.title(printerState),
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            if (navController.previousBackStackEntry != null) {
                IconButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back to previous screen"
                    )
                }
            }
        }
    )
    PrimaryTabRow(
        selectedTabIndex = destination.tab().tabIndex
    ) {
        AppTab(
            label = "Printers",
            icon = Icons.Filled.Print,
            appTab = AppTabIndex.PRINTERS,
            currentDestination = destination,
            onClick = {
                navController.navigate(PrinterList) {
                    popUpTo(PrinterList) {
                        inclusive = true
                    }
                }
            }
        )
        AppTab(
            label = "Scan",
            icon = Icons.Filled.Scanner,
            appTab = AppTabIndex.SCAN,
            currentDestination = destination,
            onClick = {
                navController.navigate(FilamentScanner)
            }
        )
    }
}