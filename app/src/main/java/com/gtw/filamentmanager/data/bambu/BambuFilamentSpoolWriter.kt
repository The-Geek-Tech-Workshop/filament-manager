@file:OptIn(ExperimentalUnsignedTypes::class)

package com.gtw.filamentmanager.data.bambu

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.util.Log
import com.gtw.filamentmanager.data.FilamentSpoolWriter
import com.gtw.filamentmanager.model.domain.BambuFilamentSpool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

typealias ByteExtractor = (Int, Int) -> ByteArray
typealias ByteWriter = (Int, Int, ByteArray) -> Unit

enum class Permission {
    KEY_A,
    KEY_B,
    NEVER,
    BOTH
}

fun Byte.getBit(position: Int): Int =
    (toInt() shr position) and 1

data class TrailerAccessPermissions(
    val readKeyA: Permission,
    val writeKeyA: Permission,
    val readKeyB: Permission,
    val writeKeyB: Permission,
    val readDataBlock: Permission,
    val writeDataBlock: Permission
) {
    companion object {

        private val permissions = mapOf(
            "000" to TrailerAccessPermissions(
                Permission.NEVER,
                Permission.KEY_A,
                Permission.KEY_A,
                Permission.NEVER,
                Permission.KEY_A,
                Permission.KEY_A
            ),
            "010" to TrailerAccessPermissions(
                Permission.NEVER,
                Permission.NEVER,
                Permission.KEY_A,
                Permission.NEVER,
                Permission.KEY_A,
                Permission.NEVER,
            ),
            "100" to TrailerAccessPermissions(
                Permission.NEVER,
                Permission.KEY_B,
                Permission.BOTH,
                Permission.NEVER,
                Permission.NEVER,
                Permission.KEY_B
            ),
            "110" to TrailerAccessPermissions(
                Permission.NEVER,
                Permission.NEVER,
                Permission.BOTH,
                Permission.NEVER,
                Permission.NEVER,
                Permission.NEVER
            ),
            "001" to TrailerAccessPermissions(
                Permission.NEVER,
                Permission.KEY_A,
                Permission.KEY_A,
                Permission.KEY_A,
                Permission.KEY_A,
                Permission.KEY_A,
            ),
            "011" to TrailerAccessPermissions(
                Permission.NEVER,
                Permission.KEY_B,
                Permission.BOTH,
                Permission.KEY_B,
                Permission.NEVER,
                Permission.KEY_B,
            ),
            "101" to TrailerAccessPermissions(
                Permission.NEVER,
                Permission.NEVER,
                Permission.BOTH,
                Permission.KEY_B,
                Permission.NEVER,
                Permission.NEVER,
            ),
            "111" to TrailerAccessPermissions(
                Permission.NEVER,
                Permission.NEVER,
                Permission.BOTH,
                Permission.NEVER,
                Permission.NEVER,
                Permission.NEVER,
            )
        )

        fun fromBitString(bitString: String): TrailerAccessPermissions =
            permissions[bitString] ?: throw Exception("Invalid bit string")
    }
}

data class DataBlockAccessPermissions(
    val read: Permission,
    val write: Permission,
    val increment: Permission,
    val decrementTransferOrRestore: Permission
) {
    companion object {

        private val permissions = mapOf(
            "000" to DataBlockAccessPermissions(
                Permission.BOTH,
                Permission.BOTH,
                Permission.BOTH,
                Permission.BOTH,
            ),
            "010" to DataBlockAccessPermissions(
                Permission.BOTH,
                Permission.NEVER,
                Permission.NEVER,
                Permission.NEVER,
            ),
            "100" to DataBlockAccessPermissions(
                Permission.BOTH,
                Permission.KEY_B,
                Permission.NEVER,
                Permission.NEVER,
            ),
            "110" to DataBlockAccessPermissions(
                Permission.BOTH,
                Permission.KEY_B,
                Permission.KEY_B,
                Permission.BOTH,
            ),
            "001" to DataBlockAccessPermissions(
                Permission.BOTH,
                Permission.NEVER,
                Permission.NEVER,
                Permission.BOTH,
            ),
            "011" to DataBlockAccessPermissions(
                Permission.KEY_B,
                Permission.KEY_B,
                Permission.NEVER,
                Permission.NEVER,
            ),
            "101" to DataBlockAccessPermissions(
                Permission.KEY_B,
                Permission.NEVER,
                Permission.NEVER,
                Permission.NEVER,
            ),
            "111" to DataBlockAccessPermissions(
                Permission.NEVER,
                Permission.NEVER,
                Permission.NEVER,
                Permission.NEVER,
            )
        )


        fun fromBitString(bitString: String): DataBlockAccessPermissions =
            permissions[bitString] ?: throw Exception("Invalid bit string")
    }
}

data class SectorAccessPermissions(
    val sector0: DataBlockAccessPermissions,
    val sector1: DataBlockAccessPermissions,
    val sector2: DataBlockAccessPermissions,
    val trailer: TrailerAccessPermissions
)

class BambuFilamentSpoolWriter @Inject constructor(
    private val bambuKeyGenerator: BambuKeyGenerator
) : FilamentSpoolWriter<BambuFilamentSpool> {
    override suspend fun write(
        tag: Tag,
        filamentSpool: BambuFilamentSpool
    ) {
        withContext(Dispatchers.IO) {
            if (!tag.techList.contains("android.nfc.tech.MifareClassic")) {
                throw Exception("Tag does not support Mifare Classic")
            }
            MifareClassic.get(tag).use { mifareClassic ->
                mifareClassic.connect()
                Log.d("BambuFilamentSpoolWriter", "Connected to tag")

                val bambuKeyAs = bambuKeyGenerator.generateKeyAs(tag.id)
                val bambuKeyBs = bambuKeyGenerator.generateKeyBs(tag.id)

                val byteExtractor = mifareClassic.byteExtractor(
                    bambuKeyAs,
                    bambuKeyBs
                )
                val byteWriter = mifareClassic.byteWriter(
                    bambuKeyAs,
                    bambuKeyBs
                )

                mifareClassic.ensureWritePermissions(byteExtractor)
                Log.d("BambuFilamentSpoolWriter", "Ensured write permissions")

                mifareClassic.getBlockBytesForFilamentSpool(filamentSpool)
                    .forEachIndexed { blockNumber, bytes ->
                        bytes?.let { byteWriter(blockNumber / 4, blockNumber % 4, it) }
                        Log.d("BambuFilamentSpoolWriter", "Wrote block $blockNumber")
                    }
                Log.d("BambuFilamentSpoolWriter", "Wrote all blocks")

                (0..NUMBER_OF_SECTORS - 1).toList().forEach { sector ->
                    byteWriter(
                        sector,
                        TRAILER_BLOCK,
                        mifareClassic.getBlockBytesForTrailer(
                            currentBytes = byteExtractor(sector, TRAILER_BLOCK),
                            keyA = bambuKeyAs[sector],
                            keyB = bambuKeyBs[sector]
                        )
                    )
                    Log.d("BambuFilamentSpoolWriter", "Wrote trailer block to sector $sector")
                }
                Log.d("BambuFilamentSpoolWriter", "Wrote all trailer blocks")

            }
        }
    }

    private fun MifareClassic.ensureWritePermissions(byteExtractor: ByteExtractor) {
        (0..NUMBER_OF_SECTORS - 1).toList().forEach { sector ->
            getAccessPermissionsForSector(sector, byteExtractor).run {
                listOf(
                    sector0.write,
                    sector1.write,
                    sector2.write,
                    trailer.writeKeyA,
                    trailer.writeKeyB
                ).all { it != Permission.NEVER }
            } || throw Exception("Sector $sector not writable")
        }
    }

    private fun MifareClassic.byteExtractor(
        keyAs: List<ByteArray>,
        keyBs: List<ByteArray>
    ): ByteExtractor =
        { sector, block ->
            val firstBlockIndexForSector = sectorToBlock(sector)
            processBytes(keyAs, keyBs, sector) {
                readBlock(firstBlockIndexForSector + block)
            } ?: throw Exception("Failed to read sector $sector")
        }

    private fun MifareClassic.byteWriter(
        keyAs: List<ByteArray>,
        keyBs: List<ByteArray>
    ): ByteWriter =
        { sector, block, data ->
            val firstBlockIndexForSector = sectorToBlock(sector)
            processBytes(keyAs, keyBs, sector) {
                writeBlock(
                    firstBlockIndexForSector + block, data
                )
            } ?: throw Exception("Failed to write sector $sector")
        }

    private fun <T> MifareClassic.processBytes(
        keyAs: List<ByteArray>,
        keyBs: List<ByteArray>,
        sector: Int,
        process: Function0<T>
    ): T? =
        (knownKeys + keyAs[sector]).fold(null as T?) { data, key ->
            data
                ?: try {
                    authenticateSectorWithKeyA(sector, key)
                    process()
                } catch (_: Exception) {
                    null
                }
        } ?: let {
            (knownKeys + keyBs[sector]).fold(null as T?) { data, key ->
                data
                    ?: try {
                        authenticateSectorWithKeyB(sector, key)
                        process()
                    } catch (_: Exception) {
                        null
                    }
            }
        }


    fun getAccessPermissionsForSector(
        sector: Int,
        extractBlockBytes: Function2<Int, Int, ByteArray>
    ): SectorAccessPermissions {
        val trailerBytes = extractBlockBytes(sector, TRAILER_BLOCK)
        return SectorAccessPermissions(
            sector0 = DataBlockAccessPermissions.fromBitString(
                sector0BitMapping.map { (byte, bit) -> trailerBytes[byte].getBit(bit) }
                    .joinToString("")
            ),
            sector1 = DataBlockAccessPermissions.fromBitString(
                sector1BitMapping.map { (byte, bit) -> trailerBytes[byte].getBit(bit) }
                    .joinToString("")
            ),
            sector2 = DataBlockAccessPermissions.fromBitString(
                sector2BitMapping.map { (byte, bit) -> trailerBytes[byte].getBit(bit) }
                    .joinToString("")
            ),
            trailer = TrailerAccessPermissions.fromBitString(
                trailerBitMapping.map { (byte, bit) -> trailerBytes[byte].getBit(bit) }
                    .joinToString("")
            )
        )
    }

    suspend fun MifareClassic.getBlockBytesForFilamentSpool(
        spool: BambuFilamentSpool
    ): List<ByteArray?> = withContext(Dispatchers.IO) {
        fun shortLE(value: Short) =
            ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()

        fun floatLE(value: Float) =
            ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array()

        val block1 = ByteArray(16)
            .apply {
                spool.trayInfoIndex.materialVariantId.toByteArray(Charsets.US_ASCII).copyOf(8)
                    .copyInto(this, 0, endIndex = 8)
                spool.trayInfoIndex.uniqueMaterialId.toByteArray(Charsets.US_ASCII).copyOf(8)
                    .copyInto(this, 8, endIndex = 8)
            }

        val block2 = spool.filamentType.toByteArray(Charsets.US_ASCII).copyOf(16)

        val block4 = spool.detailedFilamentType.bambuName.toByteArray(Charsets.US_ASCII).copyOf(16)

        val block5 = ubyteArrayOf(
            spool.filamentColour.red.toInt().toUByte(),
            spool.filamentColour.green.toInt().toUByte(),
            spool.filamentColour.blue.toInt().toUByte(),
            // Alpha is represented as 0-1 in Color, needs to be 0-255 on tag
            spool.filamentColour.alpha.toInt().let { it * 255 }.toUByte()
        ).toByteArray() + shortLE(
            spool.weightInGrams.toInt().toShort()
        ) + ByteArray(2) + floatLE(spool.filamentDiameterInMillimeters)
        val paddedBlock5 = block5.copyOf(16)

        val block6 = shortLE(spool.dryingTemperatureInCelsius.toInt().toShort()) +
                shortLE(spool.dryingTime.inWholeHours.toShort()) +
                ByteArray(2) + // TODO(): Bed temp type
                shortLE(spool.bedTemperatureInCelsius.toInt().toShort()) +
                shortLE(spool.maxTemperatureForHotendInCelsius.toInt().toShort()) +
                shortLE(spool.minTemperatureForHotendInCelsius.toInt().toShort())
        val paddedBlock6 = block6.copyOf(16)

        val block10 = ByteArray(4) +
                shortLE(spool.spoolWidthInMicroMeters.toInt().toShort()) +
                ByteArray(10)

        List(16) { index ->
            when (index) {
                1 -> block1
                2 -> block2
                4 -> block4
                5 -> paddedBlock5
                6 -> paddedBlock6
                10 -> block10
                else -> null
            }
        }
    }

    suspend fun MifareClassic.getBlockBytesForTrailer(
        currentBytes: ByteArray,
        keyA: ByteArray? = null,
        keyB: ByteArray? = null,
        userByte: Byte? = null,
//        access: SectorAccessPermissions? = null
    ): ByteArray = withContext(Dispatchers.IO) {
        val keyA = keyA ?: currentBytes.sliceArray(0..5)
        // TODO(): Allow changing access permissions
        val accessBits = currentBytes.sliceArray(6..8)
        val userByte = userByte ?: currentBytes[9]
        val keyB = keyB ?: currentBytes.sliceArray(10..15)
        keyA + accessBits + byteArrayOf(userByte) + keyB
    }

    companion object {
        private val knownKeys = listOf(
            MifareClassic.KEY_DEFAULT,
            MifareClassic.KEY_NFC_FORUM,
            MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY,
        )

        const val NUMBER_OF_SECTORS = 16

        const val TRAILER_BLOCK = 3

        val sector0BitMapping = listOf(
            7 to 4,
            8 to 0,
            8 to 4
        )
        val sector1BitMapping = listOf(
            7 to 5,
            8 to 1,
            8 to 5
        )
        val sector2BitMapping = listOf(
            7 to 6,
            8 to 2,
            8 to 6
        )
        val trailerBitMapping = listOf(
            7 to 7,
            8 to 3,
            8 to 7
        )

    }
}