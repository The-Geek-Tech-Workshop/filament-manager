@file:OptIn(ExperimentalUuidApi::class)

package com.gtw.filamentmanager.data.bambu

import android.util.Log
import com.gtw.filamentmanager.model.domain.DiscoveredPrinter
import com.gtw.filamentmanager.model.domain.PrinterModel
import com.gtw.filamentmanager.model.repos.PrinterFound
import com.gtw.filamentmanager.model.repos.PrinterRepo
import com.gtw.filamentmanager.model.repos.PrinterSearchCompleted
import com.gtw.filamentmanager.model.repos.PrinterSearchEvent
import com.gtw.filamentmanager.model.repos.PrinterSearchStarted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.SocketException
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


class BambuPrinterRepoSsdp @Inject constructor() : PrinterRepo {
    private val _printerFlow = MutableSharedFlow<PrinterSearchEvent>(0)
    override val events: Flow<PrinterSearchEvent> = _printerFlow

    companion object {
        private const val SSDP_PORT = 2021
        private val SEARCH_MAX_TIME = 10.seconds

        private const val BAMBU_PRINTER_URN = "urn:bambulab-com:device:3dprinter:1"

        private const val BAMBU_PRINTER_NAME_KEY = "DevName.bambu.com"
        private const val BAMBU_PRINTER_MODEL_KEY = "DevModel.bambu.com"
        private const val BAMBU_PRINTER_SERIAL_NUMBER_KEY = "USN"
        private const val BAMBU_PRINTER_IP_ADDRESS_KEY = "Location"

    }

    override suspend fun findPrinters() {
        withContext(Dispatchers.IO) {
            val searchId = Uuid.random()

            lateinit var socket: DatagramSocket
            try {
                _printerFlow.emit(PrinterSearchStarted(searchId))
                socket = DatagramSocket(SSDP_PORT).apply {
                    broadcast = true
                }

                launch {
                    delay(SEARCH_MAX_TIME)
                    socket.close()
                }

                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)

                while (true) {
                    socket.receive(packet)
                    val receivedData = String(packet.data, 0, packet.length)
                    val data = receivedData.lines()
                        .fold(emptyList<Pair<String, String>>()) { list, line ->
                            line.split(':', limit = 2)
                                .let { vals ->
                                    if (vals.size == 2) Pair(
                                        vals[0].trim(),
                                        vals[1].trim()
                                    ) else null
                                }
                                ?.let { list + it } ?: list
                        }
                        .toMap()

                    if (data["NT"] == BAMBU_PRINTER_URN) {
                        val serialNumber = data[BAMBU_PRINTER_SERIAL_NUMBER_KEY]
                        val ipAddress = data[BAMBU_PRINTER_IP_ADDRESS_KEY]
                        val name = data[BAMBU_PRINTER_NAME_KEY]
                        val model = data[BAMBU_PRINTER_MODEL_KEY]

                        if (serialNumber != null && ipAddress != null && name != null && model != null) {
                            val printer = DiscoveredPrinter(
                                name = name,
                                model = model.let {
                                    PrinterModel.Companion.fromBambuCode(
                                        it
                                    )
                                },
                                serialNumber = serialNumber,
                                ipAddress = Inet4Address.getByName(ipAddress),
                            )
                            _printerFlow.emit(PrinterFound(printer))
                        } else {
                            Log.w("SsdpListenerService", "Invalid printer SSDP data: $data")
                        }

                    }
                }
            } catch (_: SocketException) {

            } catch (e: Exception) {
                Log.e("SsdpListenerService", "Error listening for printers", e)
            } finally {
                if (!socket.isClosed) socket.close()
                _printerFlow.emit(PrinterSearchCompleted(searchId))
            }
        }
    }
}