package com.gtw.filamentmanager.data.bambu

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import androidx.compose.ui.graphics.Color
import com.gtw.filamentmanager.data.FilamentSpoolParser
import com.gtw.filamentmanager.data.Hkdf
import com.gtw.filamentmanager.model.domain.BambuFilamentSpool
import com.gtw.filamentmanager.model.domain.DetailedFilamentType
import com.gtw.filamentmanager.model.domain.TrayInfoIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalUnsignedTypes::class)
private val salt = ubyteArrayOf(
    0x9au,
    0x75u,
    0x9cu,
    0xf2u,
    0xc4u,
    0xf7u,
    0xcau,
    0xffu,
    0x22u,
    0x2cu,
    0xb9u,
    0x76u,
    0x9bu,
    0x41u,
    0xbcu,
    0x96u
).toByteArray()

private fun ByteArray.takeUntilNull(): ByteArray =
    this.takeWhile { it.toInt() != '\u0000'.code }.toByteArray()

private fun ByteArray.encodeToAsciiString() = String(
    takeUntilNull(),
    Charsets.US_ASCII
)

private val keyAContext = "RFID-A\u0000".toByteArray(Charsets.US_ASCII)
private val keyBContext = "RFID-B\u0000".toByteArray(Charsets.US_ASCII)

private fun ByteArray.bytesToShort(): Short {
    val byteBuffer = ByteBuffer.wrap(this)
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    return byteBuffer.getShort()
}

private fun ByteArray.bytesToFloat(): Float {
    val byteBuffer = ByteBuffer.wrap(this)
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    return byteBuffer.getFloat()
}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

private const val sectorKeyByteLength = 6

private val HKDF = Hkdf.getInstance("HmacSHA256")

class BambuFilamentSpoolParser() : FilamentSpoolParser<BambuFilamentSpool> {

    override suspend fun canParseTag(tag: Tag): Boolean =
        if (tag.techList.contains("android.nfc.tech.MifareClassic")) {
            withContext(Dispatchers.IO) {
                MifareClassic.get(tag)?.let { mifare ->
                    try {
                        mifare.connect()
                        val keyAs = HKDF.deriveKey(
                            masterKey = tag.id,
                            salt = salt,
                            info = keyAContext,
                            sectorKeyByteLength,
                            16
                        )
                        mifare.authenticateSectorWithKeyA(0, keyAs[0])
                    } catch (_: Exception) {
                        false
                    } finally {
                        mifare.close()
                    }
                } == true
            }
        } else {
            false
        }

    override suspend fun parse(tag: Tag): BambuFilamentSpool {

        return withContext(Dispatchers.IO) {

            val tagUniqueId = tag.id

            MifareClassic.get(tag)?.let { mifare ->
                try {

                    mifare.connect()
                    val keyAs = HKDF.deriveKey(
                        masterKey = tagUniqueId,
                        salt = salt,
                        info = keyAContext,
                        sectorKeyByteLength,
                        16
                    )

                    fun extractBlockBytes(sector: Int, block: Int): ByteArray {
                        if (mifare.authenticateSectorWithKeyA(sector, keyAs[sector])) {
                            val firstBlockIndexForSector = mifare.sectorToBlock(sector)
                            return mifare.readBlock(firstBlockIndexForSector + block)
                        } else {
                            throw Exception("Authentication failed for sector $sector")
                        }
                    }

                    BambuFilamentSpool(
                        tagUID = tagUniqueId.toHex(),
                        trayInfoIndex = extractBlockBytes(0, 1).let { bytes ->
                            TrayInfoIndex(
                                materialVariantId = bytes.slice(0..7).toByteArray()
                                    .encodeToAsciiString(),
                                uniqueMaterialId = bytes.slice(8..15).toByteArray()
                                    .encodeToAsciiString()
                            )
                        },
                        filamentType = extractBlockBytes(0, 2).encodeToAsciiString(),
                        detailedFilamentType = extractBlockBytes(1, 0)
                            .let { bytes ->
                                DetailedFilamentType.fromBambuName(
                                    bytes.encodeToAsciiString()
                                )
                            },
                        filamentColour = extractBlockBytes(1, 1).slice(0..3).let {
                            val red = it[0].toUByte().toInt()
                            val green = it[1].toUByte().toInt()
                            val blue = it[2].toUByte().toInt()
                            val alpha = it[3].toUByte().toInt()
                            Color(red, green, blue, alpha)
                        },
                        weightInGrams = extractBlockBytes(1, 1).slice(4..5).toByteArray()
                            .bytesToShort()
                            .toFloat(),
                        filamentDiameterInMillimeters = extractBlockBytes(1, 1).slice(8..15)
                            .toByteArray().bytesToFloat(),
                        dryingTemperatureInCelsius = extractBlockBytes(1, 2).slice(0..1)
                            .toByteArray()
                            .bytesToShort()
                            .toFloat(),
                        dryingTime = extractBlockBytes(1, 2).slice(2..3).toByteArray()
                            .bytesToShort().toInt().hours,
                        bedTemperatureInCelsius = extractBlockBytes(1, 2).slice(6..7)
                            .toByteArray().bytesToShort().toFloat(),
                        maxTemperatureForHotendInCelsius = extractBlockBytes(1, 2).slice(8..9)
                            .toByteArray().bytesToShort().toFloat(),
                        minTemperatureForHotendInCelsius = extractBlockBytes(1, 2).slice(10..11)
                            .toByteArray().bytesToShort().toFloat(),
                        spoolWidthInMicroMeters = extractBlockBytes(2, 2).slice(4..5)
                            .toByteArray()
                            .bytesToShort().toFloat(),
//                        producedAt = extractBlockBytes(2, 2).slice(0..3).toByteArray().let {

                    )
                } finally {
                    mifare.close()
                }
            } ?: throw Exception("Unable to connect to tag")
        }
    }
}