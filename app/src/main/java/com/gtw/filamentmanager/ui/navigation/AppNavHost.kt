@file:OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3Api::class)

package com.gtw.filamentmanager.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.gtw.filamentmanager.model.domain.TrayLocation
import com.gtw.filamentmanager.ui.PrinterState
import com.gtw.filamentmanager.ui.PrinterViewModel
import com.gtw.filamentmanager.ui.components.FilamentDetailsScreen
import com.gtw.filamentmanager.ui.components.FilamentScannerScreen
import com.gtw.filamentmanager.ui.components.PrinterDetailsScreen
import com.gtw.filamentmanager.ui.components.PrinterListScreen
import com.gtw.filamentmanager.ui.navigation.Destination.Companion.FilamentDetails
import com.gtw.filamentmanager.ui.navigation.Destination.Companion.FilamentScanner
import com.gtw.filamentmanager.ui.navigation.Destination.Companion.PrinterDetails
import com.gtw.filamentmanager.ui.navigation.Destination.Companion.PrinterList
import kotlin.reflect.typeOf
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun AppNavHost(
    navHostController: NavHostController,
    modifier: Modifier = Modifier,
    printerState: PrinterState,
    viewModel: PrinterViewModel,
) {
    NavHost(
        navController = navHostController,
        startDestination = PrinterList
    ) {
        composable<PrinterList> {
            PrinterListScreen(
                modifier = modifier,
                discoveredPrinters = printerState.discoveredPrinters,
                onCardClick = { printer ->
                    navHostController.navigate(PrinterDetails(printer.serialNumber))
                },
                isRefreshing = printerState.printerSearchesInProgress.isNotEmpty(),
                onRefresh = viewModel::findPrinters,
            )
        }

        composable<PrinterDetails> { backStackEntry ->
            val printerDetails: PrinterDetails =
                backStackEntry.toRoute()
            printerState.discoveredPrinters.firstOrNull { it.serialNumber == printerDetails.printerSerialNumber }
                ?.let { printer ->
                    PrinterDetailsScreen(
                        modifier = modifier,
                        printer = printer,
                        filamentTrays = printerState.filamentTrays[printer],
                        onFilamentTrayCardClick = { filamentTray ->
                            navHostController.navigate(
                                FilamentDetails(
                                    printer.serialNumber,
                                    filamentTray.location
                                )
                            )
                        },
                        isRefreshing = printerState.isPrinterAwaitingRefresh(printer),
                        onRefresh = {
                            viewModel.refreshPrinterData(printer)
                        }
                    )
                } ?: navHostController.popBackStack()
        }

        composable<FilamentDetails>(
            typeMap = mapOf(
                typeOf<TrayLocation>() to NavTypes.trayLocation,
            )
        ) {
            val filamentDetails: FilamentDetails = it.toRoute()
            printerState.discoveredPrinter(filamentDetails.printerSerialNumber)
                ?.let { printer ->
                    printerState.filamentTray(
                        printer,
                        filamentDetails.filamentTrayLocation
                    )?.let { tray ->
                        FilamentDetailsScreen(
                            modifier = modifier,
                            filamentTray = tray
                        )
                    }
                } ?: navHostController.popBackStack()
        }

        composable<FilamentScanner> {
            FilamentScannerScreen(modifier = modifier, filament = printerState.scannedFilament)
        }
    }

}
