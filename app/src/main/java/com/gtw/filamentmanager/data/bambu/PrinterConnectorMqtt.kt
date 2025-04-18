@file:OptIn(
    ExperimentalUuidApi::class, ExperimentalSerializationApi::class,
    ExperimentalStdlibApi::class
)

package com.gtw.filamentmanager.data.bambu

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.gtw.filamentmanager.di.DataCoroutineScope
import com.gtw.filamentmanager.model.domain.AMS
import com.gtw.filamentmanager.model.domain.BambuFilamentSpool
import com.gtw.filamentmanager.model.domain.DiscoveredPrinter
import com.gtw.filamentmanager.model.domain.EmptyFilamentTray
import com.gtw.filamentmanager.model.domain.ExternalSpool
import com.gtw.filamentmanager.model.domain.FilamentSpool
import com.gtw.filamentmanager.model.domain.FilamentTray
import com.gtw.filamentmanager.model.domain.PrinterAuthenticationDetails
import com.gtw.filamentmanager.model.domain.SpooledFilamentTray
import com.gtw.filamentmanager.model.domain.TrayLocation
import com.gtw.filamentmanager.model.repos.BambuPrinterEvent
import com.gtw.filamentmanager.model.repos.ConnectedPrinter
import com.gtw.filamentmanager.model.repos.ConnectionException
import com.gtw.filamentmanager.model.repos.FilamentTraysUpdate
import com.gtw.filamentmanager.model.repos.FilamentTraysUpdateError
import com.gtw.filamentmanager.model.repos.NotAuthorizedException
import com.gtw.filamentmanager.model.repos.PrinterConnector
import com.gtw.filamentmanager.model.repos.PrinterDisconnected
import com.hivemq.client.mqtt.MqttClientSslConfig
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder
import com.hivemq.client.mqtt.mqtt3.exceptions.Mqtt3ConnAckException
import com.hivemq.client.mqtt.mqtt3.message.auth.Mqtt3SimpleAuth
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAckReturnCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.net.ssl.TrustManagerFactory
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi

fun String.bambuRGBAHexToColor(): Color = Color("#${takeLast(2)}${take(6)}".toColorInt())

fun trayLocation(trayId: Int, amsUnitId: Int?): TrayLocation =
    if (trayId > 16 || amsUnitId == null) ExternalSpool else AMS(amsUnitId, trayId)


@Serializable
data class RawFilamentTray(
    val id: Int,
    val tagUid: String,
    val trayType: String,
    val trayColor: String,
    val trayWeight: Float,
    val trayDiameter: Float,
    val trayTemp: Float,
    val trayTime: Float,
    val bedTemp: Float,
    val nozzleTempMax: Float,
    val nozzleTempMin: Float,
) {

    fun toFilamentTray(amsUnitId: Int? = null): FilamentTray {
        return if (trayType.trim()
                .isEmpty()
        ) EmptyFilamentTray(trayLocation(id, amsUnitId)) else SpooledFilamentTray(
            location = trayLocation(id, amsUnitId),
            tagUID = tagUid,
            type = trayType,
            color = trayColor.bambuRGBAHexToColor(),
            weight = trayWeight,
            diameter = trayDiameter,
            temperature = trayTemp,
            time = trayTime,
            bedTemperature = bedTemp,
            nozzleTemperatureMaximumInCelsius = nozzleTempMax,
            nozzleTemperatureMinimumInCelsius = nozzleTempMin,
        )
    }

}

fun Json.parseFilamentTray(json: JsonElement, amsUnitId: Int? = null): FilamentTray =
    if (json.jsonObject.keys == setOf("id")) EmptyFilamentTray(
        trayLocation(
            json.jsonObject["id"]!!.jsonPrimitive.int,
            amsUnitId
        )
    )
    else
        decodeFromJsonElement(
            serializersModule.serializer<RawFilamentTray>(),
            json
        ).toFilamentTray(amsUnitId)

class ConnectedPrinterMqtt(
    mqttClientBuilder: Mqtt3ClientBuilder,
    private val maximumConnectionDuration: Duration,
    private val requestTopic: String,
    private val reportTopic: String,
    private val coroutineScope: CoroutineScope
) : ConnectedPrinter {

    private val mqttClient = mqttClientBuilder.addDisconnectedListener {
        runBlocking {
            _eventFlow.emit(PrinterDisconnected)
        }
    }.buildAsync()

    private val _state = MutableStateFlow<JsonElement?>(null)
    private val forcedUpdateCounter = MutableStateFlow<Int>(0)

    private val _eventFlow = MutableSharedFlow<BambuPrinterEvent>(0)
    override val events: Flow<BambuPrinterEvent> = _eventFlow

    private fun processFullPrinterDataMessage(json: JsonObject): List<FilamentTray> {
        val externalSpool = json["external_spool"]?.let {
            listOf(it).map {
                jsonSerializer.parseFilamentTray(json = it, amsUnitId = null)
            }
        } ?: emptyList<FilamentTray>()
        val ams = json["ams"]?.jsonArray?.toList()?.flatMap { ams ->
            ams.jsonObject["id"]?.jsonPrimitive?.int?.let { amsUnitId ->
                ams.jsonObject["tray"]?.jsonArray?.toList()?.map {
                    jsonSerializer.parseFilamentTray(json = it, amsUnitId = amsUnitId)
                }
            } ?: emptyList<FilamentTray>()
        } ?: emptyList<FilamentTray>()
        return externalSpool + ams
    }

    private fun merge(oldJsonState: JsonElement, newJsonUpdate: JsonElement): JsonElement {
        val dataUpdateJson = newJsonUpdate.jsonObject
        val currentJson = oldJsonState.jsonObject
        val newExternalSpoolJson =
            dataUpdateJson["vt_tray"]?.jsonObject?.let { newEs ->
                currentJson.jsonObject["external_spool"]?.jsonObject?.let { oldEs ->
                    JsonObject(oldEs + newEs)
                }
            }
        val newAmsSpoolJson =
            dataUpdateJson["ams"]?.jsonObject["ams"]?.jsonArray?.let { newAms ->
                currentJson.jsonObject["ams"]?.let { oldAms ->
                    oldAms.jsonArray.map { amsModule ->
                        amsModule.jsonObject["id"]?.jsonPrimitive?.int?.let { amsModuleId ->
                            newAms.firstOrNull { it.jsonObject["id"]?.jsonPrimitive?.int == amsModuleId }
                                ?.let { matchingNewAmsModule ->
                                    val updatedTrays =
                                        amsModule.jsonObject["tray"]?.jsonArray?.map { tray ->
                                            tray.jsonObject["id"]?.jsonPrimitive?.int?.let { trayId ->
                                                matchingNewAmsModule.jsonObject["tray"]?.jsonArray?.firstOrNull { it.jsonObject["id"]?.jsonPrimitive?.int == trayId }
                                                    ?.let { newTray ->
                                                        JsonObject(
                                                            tray.jsonObject + newTray.jsonObject
                                                        )
                                                    }

                                            } ?: tray
                                        } ?: emptyList()
                                    JsonObject(
                                        amsModule.jsonObject + mapOf(
                                            "tray" to JsonArray(
                                                updatedTrays
                                            )
                                        )
                                    )


                                }
                        } ?: amsModule.jsonObject
                    }
                }
            }
        return JsonObject(
            mapOf<String, JsonElement>(
                "external_spool" to (newExternalSpoolJson
                    ?: currentJson.jsonObject["external_spool"]
                    ?: JsonObject(emptyMap())),
                "ams" to (newAmsSpoolJson?.let {
                    JsonArray(
                        it
                    )
                }
                    ?: currentJson.jsonObject["ams"]?.jsonArray
                    ?: JsonArray(emptyList()))
            )
        )

    }

    private suspend fun connect() {
        coroutineScope.launch {
            _state.collect { latestJson ->
                try {
                    if (latestJson != null) _eventFlow.emit(
                        FilamentTraysUpdate(
                            processFullPrinterDataMessage(
                                latestJson.jsonObject
                            )
                        )
                    )
                } catch (e: Exception) {
                    Log.e("ConnectedPrinterMqtt", "Error processing filament tray data", e)
                    Log.e("ConnectedPrinterMqtt", "Json that caused error: $latestJson")
                    _eventFlow.emit(FilamentTraysUpdateError)
                }
            }
        }

        try {
            mqttClient.connect().await()
        } catch (e: Mqtt3ConnAckException) {
            when (e.mqttMessage.returnCode) {
                Mqtt3ConnAckReturnCode.BAD_USER_NAME_OR_PASSWORD, Mqtt3ConnAckReturnCode.NOT_AUTHORIZED ->
                    throw NotAuthorizedException

                else -> {
                    throw ConnectionException(e)
                }
            }
        }

        coroutineScope.launch {
            delay(maximumConnectionDuration)
            disconnect()
        }

        mqttClient.subscribeWith()
            .topicFilter(reportTopic)
            .qos(MqttQos.AT_LEAST_ONCE)
            .callback { message ->
                runBlocking {
                    message.payload.getOrNull()
                        ?.let { StandardCharsets.UTF_8.decode(it)?.toString() }
                        ?.also { payload ->
                            Log.d(
                                "MQTT",
                                "Received message: $payload"
                            )
                            val json = Json.parseToJsonElement(payload)
                            if (json is JsonObject) {
                                json["print"]?.let { dataUpdateJson ->
                                    val currentJson = _state.value
                                    if (dataUpdateJson is JsonObject) {
                                        val isMessage =
                                            dataUpdateJson["msg"]?.jsonPrimitive?.int

                                        if (isMessage == 0) {
                                            _state.update {
                                                dataUpdateJson["vt_tray"]?.let { externalSpoolJson ->
                                                    dataUpdateJson["ams"]?.jsonObject["ams"]?.let { amsJson ->
                                                        JsonObject(
                                                            mapOf(
                                                                "external_spool" to externalSpoolJson,
                                                                "ams" to amsJson,
                                                                "forced_updates" to JsonPrimitive(
                                                                    forcedUpdateCounter.value
                                                                )
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        } else if (currentJson != null && isMessage == 1 && dataUpdateJson.jsonObject.keys.any { it == "vt_tray" || it == "ams" }) {
                                            withContext(Dispatchers.Default) {
                                                _state.update {
                                                    merge(
                                                        currentJson,
                                                        dataUpdateJson
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }
            }.send().await()
    }

    suspend fun disconnect() {
        if (mqttClient.state.isConnected) mqttClient.disconnect().await()
    }

    private fun publish(requestBody: JsonElement) =
        mqttClient.publishWith()
            .topic(requestTopic)
            .qos(MqttQos.AT_LEAST_ONCE)
            .payload(Json.encodeToString(requestBody).encodeToByteArray())
            .send()

    override suspend fun requestAllPrinterData() {
        withContext(Dispatchers.IO) {
            forcedUpdateCounter.update { it + 1 }
            publish(requestAllPrinterDataBody)
        }
    }

    override suspend fun setFilamentTraySpool(
        filamentTray: FilamentTray,
        filamentSpool: FilamentSpool
    ) {
        withContext(Dispatchers.IO) {
            publish(setAmsFilamentSettingRequestJson(filamentTray, filamentSpool))
        }
    }

    companion object {
        const val EXTERNAL_SPOOL_AMS_UNIT_ID = 255
        const val EXTERNAL_SPOOL_AMS_SLOT_ID = 254

        private val jsonSerializer = Json {
            ignoreUnknownKeys = true
            namingStrategy = JsonNamingStrategy.SnakeCase
        }

        private val requestAllPrinterDataBody = Json.parseToJsonElement(
            """
            {
                "pushing": {
                    "sequence_id": "0",
                    "command": "pushall",
                    "version": 1,
                    "push_target": 1
                }
            }
        """.trimIndent()
        )

        private fun setAmsFilamentSettingRequestJson(
            filamentTray: FilamentTray,
            filamentSpool: FilamentSpool
        ): JsonElement {
            when (filamentSpool) {
                is BambuFilamentSpool -> {
                    val location = filamentTray.location
                    val rgbaHex =
                        filamentSpool.filamentColour.value.toHexString().let { originalHex ->
                            originalHex.substring(2, 8) + originalHex.take(2)
                        }.uppercase()
                    return Json.parseToJsonElement(
//                "tray_info_idx":"GFL99",
                        """
            {
                "print": {
                  "command":"ams_filament_setting",
                  "sequence_id":"0",
                  "ams_id":${if (location is AMS) location.unit else EXTERNAL_SPOOL_AMS_UNIT_ID},
                  "nozzle_temp_max":${filamentSpool.maxTemperatureForHotendInCelsius},
                  "nozzle_temp_min":${filamentSpool.minTemperatureForHotendInCelsius},
                  "tray_color":"$rgbaHex",
                  "tray_id":${if (location is AMS) location.slot else EXTERNAL_SPOOL_AMS_SLOT_ID},
                  "tray_info_idx": "${filamentSpool.trayInfoIndex.uniqueMaterialId}",
                  "tray_type":"${filamentSpool.filamentType}"
                }
            }
            """.trimIndent()
                    )

                }
            }
        }

        suspend fun connect(
            mqttClientBuilder: Mqtt3ClientBuilder,
            reportTopic: String,
            requestTopic: String,
            maximumConnectionDuration: Duration,
            coroutineScope: CoroutineScope
        ): ConnectedPrinterMqtt =
            ConnectedPrinterMqtt(
                mqttClientBuilder,
                maximumConnectionDuration,
                requestTopic,
                reportTopic,
                coroutineScope
            ).also { printer ->
                printer.connect()
            }

    }
}

class PrinterConnectorMqtt @Inject constructor(
    private val trustManagerFactory: TrustManagerFactory,
    private val coroutineScope: DataCoroutineScope
) : PrinterConnector {

    override suspend fun connectPrinter(
        printer: DiscoveredPrinter,
        authenticationDetails: PrinterAuthenticationDetails
    ): ConnectedPrinterMqtt = ConnectedPrinterMqtt.connect(
        mqttClientBuilder = Mqtt3Client.builder()
            .identifier("FilamentLoader-${printer.serialNumber}")
            .serverHost(printer.ipAddress)
            .serverPort(BAMBU_LOCAL_MQTT_PORT)
            .sslConfig(
                MqttClientSslConfig.builder()
                    .trustManagerFactory(trustManagerFactory)
                    // TODO: Implement SNI correctly (printer serial)
                    .hostnameVerifier { hostname, session -> true }
                    .build()
            )
            .simpleAuth(
                Mqtt3SimpleAuth.builder()
                    .username(authenticationDetails.username)
                    .password(authenticationDetails.accessCode.encodeToByteArray())
                    .build()
            ),
        maximumConnectionDuration = MAXIMUM_CONNECTION_TIME,
        requestTopic = "device/${printer.serialNumber}/request",
        reportTopic = "device/${printer.serialNumber}/report",
        coroutineScope = coroutineScope
    )

    companion object {
        const val BAMBU_LOCAL_MQTT_PORT = 8883
        val MAXIMUM_CONNECTION_TIME = 20.seconds
    }
}
