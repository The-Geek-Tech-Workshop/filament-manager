import HkdfTestData.example1Algorithm
import HkdfTestData.example1Context
import HkdfTestData.example1KeyLength
import HkdfTestData.example1MasterKey
import HkdfTestData.example1NumKeys
import HkdfTestData.example1Salt
import HkdfTestData.expectedHexKeys1
import com.gtw.filamentmanager.data.Hkdf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import java.security.NoSuchAlgorithmException
import java.util.Base64
import javax.crypto.Mac

fun ByteArray.toBase64(): String =
    String(Base64.getEncoder().encode(this))

fun String.hexStringToByteArray(): ByteArray =
    this.chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

class HkdfTest {

    private val testMasterKey = "master_key".toByteArray(Charsets.US_ASCII)
    private val testSalt = "salt".toByteArray(Charsets.US_ASCII)
    private val testInfo = "info".toByteArray(Charsets.US_ASCII)

   @Test
   fun `deriveKey with HmacSHA256 and single key`() {
       val hkdf = Hkdf.getInstance("HmacSHA256")
       val expectedKeyLength = 16
       val key = hkdf.deriveKey(testMasterKey, testSalt, testInfo, expectedKeyLength)
       assertEquals(expectedKeyLength, key[0].size)
   }

   @Test
   fun `deriveKey with HmacSHA512 and single key`() {
       val hkdf = Hkdf.getInstance("HmacSHA512")
       val expectedKeyLength = 16
       val key = hkdf.deriveKey(testMasterKey, testSalt, testInfo, expectedKeyLength)
       assertEquals(expectedKeyLength, key[0].size)
   }

   @Test
   fun `deriveKey with HmacSHA256 and multiple keys`() {
       val hkdf = Hkdf.getInstance("HmacSHA256")
       val expectedKeyLength = 16
       val numKeys = 3
       val keys = hkdf.deriveKey(testMasterKey, testSalt, testInfo, expectedKeyLength, numKeys)
       assertEquals(numKeys, keys.size)
       keys.forEach { assertEquals(expectedKeyLength, it.size) }
   }

   @Test
   fun `deriveKey with HmacSHA512 and multiple keys`() {
       val hkdf = Hkdf.getInstance("HmacSHA512")
       val expectedKeyLength = 16
       val numKeys = 3
       val keys = hkdf.deriveKey(testMasterKey, testSalt, testInfo, expectedKeyLength, numKeys)
       assertEquals(numKeys, keys.size)
       keys.forEach { assertEquals(expectedKeyLength, it.size) }
   }

   @Test
   fun `deriveKey with invalid key length`() {
       val hkdf = Hkdf.getInstance("HmacSHA256")
       val digestLength = getDigestLength("HmacSHA256")
       val invalidKeyLength = 255 * digestLength + 1
       assertThrows(IllegalArgumentException::class.java) {
           hkdf.deriveKey(testMasterKey, testSalt, testInfo, invalidKeyLength)
       }
   }

   @Test
   fun `deriveKey with empty master key`() {
       val hkdf = Hkdf.getInstance("HmacSHA256")
       val expectedKeyLength = 16
       val emptyMasterKey = ByteArray(0)
       val key = hkdf.deriveKey(emptyMasterKey, testSalt, testInfo, expectedKeyLength)
       assertEquals(expectedKeyLength, key[0].size)
   }

   @Test
   fun `deriveKey with large offset`() {
       val hkdf = Hkdf.getInstance("HmacSHA256")
       val expectedKeyLength = 16
       val digestLength = getDigestLength("HmacSHA256")
       val largeOffset = 255 * digestLength + 1
       assertThrows(IllegalArgumentException::class.java) {
           hkdf.deriveKey(testMasterKey, testSalt, testInfo, expectedKeyLength, largeOffset)
       }
   }

    @Throws(NoSuchAlgorithmException::class)
    private fun getDigestLength(macAlgorithm: String): Int {
        val mac = Mac.getInstance(macAlgorithm)
        return mac.macLength
    }


    @Test
    fun `Full example 1`() {
        val hkdf = Hkdf.getInstance(example1Algorithm)
        val keys =
            hkdf.deriveKey(
                example1MasterKey,
                example1Salt,
                example1Context,
                example1KeyLength,
                example1NumKeys
            )
        println(example1MasterKey.toHex())
        assertEquals(example1NumKeys, keys.size)
        keys.forEach { assertEquals(example1KeyLength, it.size) }
        assertEquals(expectedHexKeys1, keys.map { it.toHex().uppercase() })
    }
}

object HkdfTestData {
    // Example 1: HMAC-SHA256 (Single Key)
    val example1Algorithm = "HmacSHA256"

    val example1MasterKey = "babe97b4".hexStringToByteArray()

    @OptIn(ExperimentalUnsignedTypes::class)
    val example1Salt = ubyteArrayOf(
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

    val example1Context = "RFID-A\u0000".toByteArray(Charsets.US_ASCII)
    val example1KeyLength = 6
    val example1NumKeys = 16
    val expectedHexKeys1 = listOf(
        "0846904161A6",
        "F5E61676ACD0",
        "7418CACC06B9",
        "88A9EA11E50E",
        "3943F93EAFFE",
        "F0FDAC0ACB80",
        "2E2DA345C5C7",
        "C5ACE4EE9811",
        "2B7029FBA363",
        "18228ACE18FD",
        "30E5799B08D4",
        "7D4AE1393B70",
        "DB9A39F43D6F",
        "BCFC9434002F",
        "FB6AA9CF4B21",
        "DF845EE9DFD8",
    )

    // Example 2: HMAC-SHA256 (Multiple Keys)
    val example2Algorithm = "HmacSHA256"
    val example2MasterKey = byteArrayOf(
        0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b,
        0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b,
        0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b
    ) // 22 bytes
    val example2Salt = byteArrayOf(
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08, 0x09, 0x0a, 0x0b, 0x0c
    ) // 13 bytes
    val example2Context = byteArrayOf(
        0xf0.toByte(), 0xf1.toByte(), 0xf2.toByte(), 0xf3.toByte(),
        0xf4.toByte(), 0xf5.toByte(), 0xf6.toByte(), 0xf7.toByte(),
        0xf8.toByte(), 0xf9.toByte()
    ) // 10 bytes
    val example2KeyLength = 16
    val example2NumKeys = 3
    val example2ExpectedOutputKeys = listOf(
        byteArrayOf(
            0x77, 0x7a.toByte(), 0x85.toByte(), 0xb3.toByte(),
            0x8a.toByte(), 0x9b.toByte(), 0x2a, 0x4a,
            0x86.toByte(), 0x6a.toByte(), 0x74, 0x6e,
            0x45, 0x79, 0x6f, 0xf3.toByte()
        ), // 16 bytes
        byteArrayOf(
            0x31, 0x19, 0xb1.toByte(), 0x12,
            0x05, 0xc7.toByte(), 0x53, 0x13,
            0xd3.toByte(), 0x14, 0xa8.toByte(), 0x39,
            0x1b, 0x48, 0x14, 0x6b
        ), // 16 bytes
        byteArrayOf(
            0x94.toByte(), 0xf9.toByte(), 0x13, 0x21,
            0x24, 0x96.toByte(), 0x07, 0xe1.toByte(),
            0x5b, 0xb4.toByte(), 0x07, 0xe1.toByte(),
            0x98.toByte(), 0x78, 0x56, 0x8e.toByte()
        )  // 16 bytes
    )
}