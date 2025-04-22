@file:OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3Api::class)

package com.gtw.filamentmanager.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.gtw.filamentmanager.ui.components.AccessCodeInputDialogue
import com.gtw.filamentmanager.ui.components.ReadyToWrite
import com.gtw.filamentmanager.ui.navigation.AppNavHost
import com.gtw.filamentmanager.ui.navigation.AppTopNavBar
import com.gtw.filamentmanager.ui.navigation.Destination
import com.gtw.filamentmanager.ui.navigation.Destination.Companion.AppSettings
import com.gtw.filamentmanager.ui.navigation.Destination.Companion.FilamentDetails
import com.gtw.filamentmanager.ui.navigation.Destination.Companion.FilamentScanner
import com.gtw.filamentmanager.ui.navigation.Destination.Companion.PrinterDetails
import com.gtw.filamentmanager.ui.navigation.Destination.Companion.PrinterList
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi

fun isNfcSupported(context: Context): Boolean {
    val packageManager = context.packageManager
    return packageManager.hasSystemFeature(PackageManager.FEATURE_NFC) &&
            packageManager.hasSystemFeature("com.nxp.mifare")
}

fun isNfcEnabled(context: Context): Boolean {
    return NfcAdapter.getDefaultAdapter(context)?.isEnabled == true
}

fun NavBackStackEntry.getRoute(): Destination? =
    when {
        destination.hasRoute(PrinterDetails::class) == true ->
            toRoute<PrinterDetails>()

        destination.hasRoute(PrinterList::class) == true ->
            PrinterList

        destination.hasRoute(FilamentDetails::class) == true ->
            toRoute<FilamentDetails>()

        destination.hasRoute(AppSettings::class) == true ->
            AppSettings

        destination.hasRoute(FilamentScanner::class) == true ->
            FilamentScanner

        else -> null
    }


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: PrinterViewModel by viewModels()

    @Inject
    lateinit var nfcAdapter: NfcAdapter

    private lateinit var pendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isNfcSupported(this)) {
            Log.e("NFC", "NFC is not supported on this device")
        }
        if (!isNfcEnabled(this)) {
            Log.e("NFC", "NFC is not enabled on this device")
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // NFC Setup
        pendingIntent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        enableEdgeToEdge()
        setContent {
            val printerState = viewModel.printerState.collectAsStateWithLifecycle().value
            val navController = rememberNavController()
            val currentBackStackEntry = navController.currentBackStackEntryAsState()
            val destination: Destination =
                currentBackStackEntry.value?.getRoute() ?: PrinterList

            MaterialTheme {

                LaunchedEffect(destination) {
                    // When the destination changes, set where scanned spool data will be sent
                    when (destination) {
                        is FilamentDetails -> printerState.discoveredPrinter(destination.printerSerialNumber)
                            ?.let { printer ->
                                printerState.filamentTray(printer, destination.filamentTrayLocation)
                                    ?.let { tray ->
                                        viewModel.setScanDestination(
                                            ScanToFilamentTray(
                                                printer,
                                                tray
                                            )
                                        )
                                    }
                            } ?: viewModel.setScanDestination(null)

                        is FilamentScanner -> viewModel.setScanDestination(ScanToApp)

                        else -> viewModel.setScanDestination(null)
                    }
                }

                LaunchedEffect(destination) {
                    //  Ensure that printer data is refreshed when arriving
                    // on a printer details screen
                    when (destination) {
                        is PrinterDetails -> printerState.discoveredPrinter(destination.printerSerialNumber)
                            ?.let { printer ->
                                viewModel.refreshPrinterData(printer)
                            }

                        else -> Unit
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(viewModel.snackbarHostState) },
                    topBar = {
                        Column(
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            AppTopNavBar(
                                destination = destination,
                                navController = navController,
                                printerState = printerState
                            )
                        }
                    },
                    floatingActionButton = {
                        when (destination) {
                            is FilamentScanner -> {
                                printerState.scannedFilament?.let { filament ->
                                    IconButton(
                                        onClick = {
                                            viewModel.setScanDestination(
                                                WriteFilamentSpoolToTag(filament)
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Write to Tag")
                                    }
                                }
                            }

                            else -> Unit
                        }

                    }
                ) { contentPadding ->
                    AppNavHost(
                        navHostController = navController,
                        modifier = Modifier.padding(contentPadding),
                        printerState = printerState,
                        viewModel = viewModel
                    )
                }
                printerState.accessCodeIsRequired?.let {
                    val printer = it.forPrinter
                    AccessCodeInputDialogue(
                        printer = printer,
                        onDismiss = {
                            viewModel.accessCodeRequestDismissed()
                            viewModel.setSelectedPrinter(null)
                            navController.popBackStack(PrinterList, inclusive = false)
                        },
                        onConfirm = { accessCode ->
                            viewModel.updateAccessCodeForPrinter(
                                printer,
                                accessCode
                            )
                        }
                    )
                }
                if (printerState.tagAction is WriteFilamentSpoolToTag) {
                    ReadyToWrite(
                        onDismiss = {
                            viewModel.setScanDestination(ScanToApp)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        nfcAdapter.disableForegroundDispatch(this)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        lifecycleScope.launch(Dispatchers.IO) {
            when (intent.action) {
//                NfcAdapter.ACTION_TECH_DISCOVERED -> {
                NfcAdapter.ACTION_TAG_DISCOVERED -> {
                    getTagFromIntent(intent)?.let { viewModel.nfcTagDetected(it) }
                }
            }
        }
    }

    private fun getTagFromIntent(intent: Intent): Tag? {
        return intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
    }

}