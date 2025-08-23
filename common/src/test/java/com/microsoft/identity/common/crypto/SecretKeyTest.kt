package com.microsoft.identity.common.crypto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalSerializationApi::class)
class SecretKeyTest {

    @Test
    fun testSecretKeyConstruction() {
        // Test basic construction
        val secretKey = SecretKey(
            algorithm = "AES",
            transformation = "AES/GCM/NoPadding",
            keySize = 256
        )

        assertEquals("AES", secretKey.algorithm)
        assertEquals("AES/GCM/NoPadding", secretKey.transformation)
        assertEquals(256, secretKey.keySize)
    }

    @Test
    fun testSecretKeyWithDifferentAlgorithms() {
        val aesKey = SecretKey("AES", "AES/CBC/PKCS5Padding", 128)
        val rsaKey = SecretKey("RSA", "RSA/ECB/PKCS1Padding", 2048)
        val ecKey = SecretKey("EC", "ECDSA", 256)

        assertEquals("AES", aesKey.algorithm)
        assertEquals("RSA", rsaKey.algorithm)
        assertEquals("EC", ecKey.algorithm)
    }

    @Test
    fun testSecretKeyWithVariousKeySizes() {
        val small = SecretKey("AES", "AES/GCM/NoPadding", 128)
        val medium = SecretKey("AES", "AES/GCM/NoPadding", 192)
        val large = SecretKey("AES", "AES/GCM/NoPadding", 256)

        assertEquals(128, small.keySize)
        assertEquals(192, medium.keySize)
        assertEquals(256, large.keySize)
    }

    @Test
    fun testSecretKeyEquality() {
        val secretKey1 = SecretKey("AES", "AES/GCM/NoPadding", 256)
        val secretKey2 = SecretKey("AES", "AES/GCM/NoPadding", 256)
        val secretKey3 = SecretKey("RSA", "RSA/ECB/PKCS1Padding", 2048)

        // Test equality
        assertEquals(secretKey1, secretKey2)
        assertNotEquals(secretKey1, secretKey3)

        // Test hash code consistency
        assertEquals(secretKey1.hashCode(), secretKey2.hashCode())
        assertNotEquals(secretKey1.hashCode(), secretKey3.hashCode())
    }

    @Test
    fun testSecretKeyToString() {
        val secretKey = SecretKey("AES", "AES/GCM/NoPadding", 256)
        val toString = secretKey.toString()

        assertTrue("toString should contain algorithm", toString.contains("AES"))
        assertTrue("toString should contain transformation", toString.contains("AES/GCM/NoPadding"))
        assertTrue("toString should contain keySize", toString.contains("256"))
    }

    @Test
    fun testSecretKeyCopy() {
        val original = SecretKey("AES", "AES/GCM/NoPadding", 256)
        
        // Test copy with no changes
        val exactCopy = original.copy()
        assertEquals(original, exactCopy)
        assertNotSame(original, exactCopy)

        // Test copy with algorithm change
        val copyWithNewAlgorithm = original.copy(algorithm = "RSA")
        assertEquals("RSA", copyWithNewAlgorithm.algorithm)
        assertEquals("AES/GCM/NoPadding", copyWithNewAlgorithm.transformation)
        assertEquals(256, copyWithNewAlgorithm.keySize)

        // Test copy with transformation change
        val copyWithNewTransformation = original.copy(transformation = "AES/CBC/PKCS5Padding")
        assertEquals("AES", copyWithNewTransformation.algorithm)
        assertEquals("AES/CBC/PKCS5Padding", copyWithNewTransformation.transformation)
        assertEquals(256, copyWithNewTransformation.keySize)

        // Test copy with keySize change
        val copyWithNewSize = original.copy(keySize = 512)
        assertEquals("AES", copyWithNewSize.algorithm)
        assertEquals("AES/GCM/NoPadding", copyWithNewSize.transformation)
        assertEquals(512, copyWithNewSize.keySize)
    }

    @Test
    fun testProtobufSerialization() {
        val original = SecretKey("AES", "AES/GCM/NoPadding", 256)

        // Serialize to protobuf
        val serialized = ProtoBuf.encodeToByteArray(original)
        assertNotNull("Serialized data should not be null", serialized)
        assertTrue("Serialized data should not be empty", serialized.isNotEmpty())

        // Deserialize from protobuf
        val deserialized = ProtoBuf.decodeFromByteArray<SecretKey>(serialized)
        
        // Verify deserialized object matches original
        assertEquals(original, deserialized)
        assertEquals(original.algorithm, deserialized.algorithm)
        assertEquals(original.transformation, deserialized.transformation)
        assertEquals(original.keySize, deserialized.keySize)
    }

    @Test
    fun testProtobufSerializationRoundTrip() {
        val testCases = listOf(
            SecretKey("AES", "AES/GCM/NoPadding", 128),
            SecretKey("AES", "AES/CBC/PKCS5Padding", 192),
            SecretKey("AES", "AES/CTR/NoPadding", 256),
            SecretKey("RSA", "RSA/ECB/PKCS1Padding", 1024),
            SecretKey("RSA", "RSA/ECB/OAEPPadding", 2048),
            SecretKey("RSA", "RSA/ECB/OAEPWithSHA-256AndMGF1Padding", 4096),
            SecretKey("EC", "ECDSA", 256),
            SecretKey("EC", "ECDH", 384),
            SecretKey("DES", "DES/CBC/PKCS5Padding", 56),
            SecretKey("3DES", "DESede/CBC/PKCS5Padding", 168)
        )

        testCases.forEach { original ->
            val serialized = ProtoBuf.encodeToByteArray(original)
            val deserialized = ProtoBuf.decodeFromByteArray<SecretKey>(serialized)
            assertEquals("Round-trip serialization failed for $original", original, deserialized)
        }
    }

    @Test
    fun testProtobufSerializationWithEmptyStrings() {
        val secretKey = SecretKey("", "", 0)

        assertEquals("", secretKey.algorithm)
        assertEquals("", secretKey.transformation)
        assertEquals(0, secretKey.keySize)

        // Test that it can still be serialized/deserialized
        val serialized = ProtoBuf.encodeToByteArray(secretKey)
        val deserialized = ProtoBuf.decodeFromByteArray<SecretKey>(serialized)
        assertEquals(secretKey, deserialized)
    }

    @Test
    fun testProtobufSerializationWithSpecialCharacters() {
        val secretKey = SecretKey(
            algorithm = "AES-with-special-chars-@#$%^&*()",
            transformation = "AES/GCM/NoPadding-with-special-chars-!@#$%",
            keySize = 256
        )

        assertEquals("AES-with-special-chars-@#$%^&*()", secretKey.algorithm)
        assertEquals("AES/GCM/NoPadding-with-special-chars-!@#$%", secretKey.transformation)

        // Test serialization with special characters
        val serialized = ProtoBuf.encodeToByteArray(secretKey)
        val deserialized = ProtoBuf.decodeFromByteArray<SecretKey>(serialized)
        assertEquals(secretKey, deserialized)
    }

    @Test
    fun testProtobufSerializationWithUnicodeCharacters() {
        val secretKey = SecretKey(
            algorithm = "AES-алгоритм-🔐-암호화",
            transformation = "AES/GCM/NoPadding-трансформация-🔒-변환",
            keySize = 256
        )

        assertEquals("AES-алгоритм-🔐-암호화", secretKey.algorithm)
        assertEquals("AES/GCM/NoPadding-трансформация-🔒-변환", secretKey.transformation)

        // Test serialization with Unicode characters
        val serialized = ProtoBuf.encodeToByteArray(secretKey)
        val deserialized = ProtoBuf.decodeFromByteArray<SecretKey>(serialized)
        assertEquals(secretKey, deserialized)
    }

    @Test
    fun testProtobufSerializationWithLongStrings() {
        val longAlgorithm = "A".repeat(1000)
        val longTransformation = "T".repeat(1000)

        val secretKey = SecretKey(longAlgorithm, longTransformation, 256)

        // Test serialization with very long strings
        val serialized = ProtoBuf.encodeToByteArray(secretKey)
        val deserialized = ProtoBuf.decodeFromByteArray<SecretKey>(serialized)
        assertEquals(secretKey, deserialized)
        assertEquals(longAlgorithm, deserialized.algorithm)
        assertEquals(longTransformation, deserialized.transformation)
    }

    @Test
    fun testSecretKeyComponentAccess() {
        val secretKey = SecretKey("AES", "AES/GCM/NoPadding", 256)

        // Test component access (destructuring)
        val (algorithm, transformation, keySize) = secretKey

        assertEquals("AES", algorithm)
        assertEquals("AES/GCM/NoPadding", transformation)
        assertEquals(256, keySize)
    }

    @Test
    fun testSecretKeyWithNegativeKeySize() {
        // Test edge case with negative key size
        val secretKey = SecretKey("AES", "AES/GCM/NoPadding", -1)

        assertEquals(-1, secretKey.keySize)

        // Should still be serializable
        val serialized = ProtoBuf.encodeToByteArray(secretKey)
        val deserialized = ProtoBuf.decodeFromByteArray<SecretKey>(serialized)
        assertEquals(secretKey, deserialized)
    }

    @Test
    fun testSecretKeyWithLargeKeySize() {
        // Test with very large key size
        val secretKey = SecretKey("RSA", "RSA/ECB/PKCS1Padding", Int.MAX_VALUE)

        assertEquals(Int.MAX_VALUE, secretKey.keySize)

        // Should still be serializable
        val serialized = ProtoBuf.encodeToByteArray(secretKey)
        val deserialized = ProtoBuf.decodeFromByteArray<SecretKey>(serialized)
        assertEquals(secretKey, deserialized)
    }

    @Test
    fun testProtobufSerializationCompatibility() {
        // Test that serialization format is consistent and compatible
        val secretKey = SecretKey("AES", "AES/GCM/NoPadding", 256)

        // Serialize multiple times and ensure bytes are identical
        val serialized1 = ProtoBuf.encodeToByteArray(secretKey)
        val serialized2 = ProtoBuf.encodeToByteArray(secretKey)

        assertArrayEquals("Multiple serializations should produce identical bytes", serialized1, serialized2)

        // Deserialize both and ensure they're equal
        val deserialized1 = ProtoBuf.decodeFromByteArray<SecretKey>(serialized1)
        val deserialized2 = ProtoBuf.decodeFromByteArray<SecretKey>(serialized2)

        assertEquals(deserialized1, deserialized2)
    }

    @Test
    fun testProtobufSerializationSize() {
        // Test that protobuf serialization is reasonably compact
        val secretKey = SecretKey("AES", "AES/GCM/NoPadding", 256)
        val serialized = ProtoBuf.encodeToByteArray(secretKey)

        // Protobuf should be relatively compact
        assertTrue("Serialized size should be reasonable", serialized.size < 100)

        // Test with longer strings
        val longSecretKey = SecretKey("A".repeat(100), "T".repeat(100), 256)
        val longSerialized = ProtoBuf.encodeToByteArray(longSecretKey)

        // Should still be reasonable but larger
        assertTrue("Long serialized size should be larger", longSerialized.size > serialized.size)
        assertTrue("Long serialized size should still be reasonable", longSerialized.size < 500)
    }

    @Test
    fun testProtobufProtoNumberMapping() {
        // Test that ProtoNumber annotations work correctly
        val secretKey = SecretKey("AES", "AES/GCM/NoPadding", 256)
        val serialized = ProtoBuf.encodeToByteArray(secretKey)

        // Verify that we can deserialize successfully (which means ProtoNumber mapping is correct)
        val deserialized = ProtoBuf.decodeFromByteArray<SecretKey>(serialized)

        assertEquals(secretKey.algorithm, deserialized.algorithm)      // ProtoNumber(1)
        assertEquals(secretKey.transformation, deserialized.transformation) // ProtoNumber(2)
        assertEquals(secretKey.keySize, deserialized.keySize)         // ProtoNumber(3)
    }

    @Test
    fun testMultipleSecretKeysSerializationIndependence() {
        val key1 = SecretKey("AES", "AES/GCM/NoPadding", 128)
        val key2 = SecretKey("RSA", "RSA/ECB/PKCS1Padding", 2048)
        val key3 = SecretKey("EC", "ECDSA", 256)

        val serialized1 = ProtoBuf.encodeToByteArray(key1)
        val serialized2 = ProtoBuf.encodeToByteArray(key2)
        val serialized3 = ProtoBuf.encodeToByteArray(key3)

        // Each serialization should be independent
        val deserialized1 = ProtoBuf.decodeFromByteArray<SecretKey>(serialized1)
        val deserialized2 = ProtoBuf.decodeFromByteArray<SecretKey>(serialized2)
        val deserialized3 = ProtoBuf.decodeFromByteArray<SecretKey>(serialized3)

        assertEquals(key1, deserialized1)
        assertEquals(key2, deserialized2)
        assertEquals(key3, deserialized3)

        // Cross-deserialization should not work (each is independent)
        assertNotEquals(key1, deserialized2)
        assertNotEquals(key2, deserialized3)
        assertNotEquals(key3, deserialized1)
    }
}
