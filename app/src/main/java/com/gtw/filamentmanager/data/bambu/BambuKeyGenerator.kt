@file:OptIn(ExperimentalUnsignedTypes::class)

package com.gtw.filamentmanager.data.bambu

import com.gtw.filamentmanager.data.Hkdf
import javax.inject.Inject

class BambuKeyGenerator @Inject constructor(
    private val hkdf: Hkdf
) {

    fun generateKeyAs(tagId: ByteArray): List<ByteArray> =
        generateKeys(tagId, keyAContext)

    fun generateKeyBs(tagId: ByteArray): List<ByteArray> =
        generateKeys(tagId, keyBContext)

    private fun generateKeys(tagId: ByteArray, keyContext: ByteArray): List<ByteArray> =
        hkdf.deriveKey(
            masterKey = tagId,
            salt = bambuSalt,
            info = keyContext,
            sectorKeyByteLength,
            numberOfKeys
        )

    companion object {
        private val bambuSalt = ubyteArrayOf(
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
        private val keyAContext = "RFID-A\u0000".toByteArray(Charsets.US_ASCII)
        private val keyBContext = "RFID-B\u0000".toByteArray(Charsets.US_ASCII)
        private const val sectorKeyByteLength = 6
        private const val numberOfKeys = 16
    }
}