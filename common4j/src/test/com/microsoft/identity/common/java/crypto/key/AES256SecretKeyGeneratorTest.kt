// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.java.crypto.key

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations
import javax.crypto.spec.SecretKeySpec

/**
 * Unit tests for [AES256SecretKeyGenerator].
 */
class AES256SecretKeyGeneratorTest {

    private lateinit var secretKeyGenerator: AES256SecretKeyGenerator

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        secretKeyGenerator = AES256SecretKeyGenerator()
    }

    @Test
    fun testKeySize() {
        // Verify that the key size is 256 bits for AES-256
        Assert.assertEquals(256, secretKeyGenerator.keySize)
    }

    @Test
    fun testKeyAlgorithm() {
        // Verify that the key algorithm is "AES"
        Assert.assertEquals("AES", secretKeyGenerator.keyAlgorithm)
    }

    @Test
    fun testGenerateRandomKey() {
        // Generate a random key
        val secretKey = secretKeyGenerator.generateRandomKey()

        // Verify that the key is not null
        Assert.assertNotNull(secretKey)

        // Verify that the key algorithm is "AES"
        Assert.assertEquals("AES", secretKey.algorithm)

        // Verify that the encoded form of the key has the correct length (32 bytes for 256 bits)
        Assert.assertEquals(32, secretKey.encoded.size)
    }

    @Test
    fun testGenerateKeyFromRawBytes() {
        // Create a byte array of 32 bytes (256 bits) filled with a test value
        val rawBytes = ByteArray(32) { 0x42.toByte() }

        // Generate a key from the raw bytes
        val secretKey = secretKeyGenerator.generateKeyFromRawBytes(rawBytes)

        // Verify that the key is not null
        Assert.assertNotNull(secretKey)

        // Verify that the key algorithm is "AES"
        Assert.assertEquals("AES", secretKey.algorithm)

        // Verify that the encoded form of the key matches the input raw bytes
        Assert.assertArrayEquals(rawBytes, secretKey.encoded)
    }

    @Test
    fun testGenerateKeyFromRawBytes_VerifyInstance() {
        // Create a byte array of 32 bytes (256 bits)
        val rawBytes = ByteArray(32) { 0x37.toByte() }

        // Generate a key from the raw bytes
        val secretKey = secretKeyGenerator.generateKeyFromRawBytes(rawBytes)

        // Verify that the key is an instance of SecretKeySpec
        Assert.assertTrue(secretKey is SecretKeySpec)
    }

    @Test
    fun testGenerateMultipleRandomKeys_AreUnique() {
        // Generate two random keys
        val secretKey1 = secretKeyGenerator.generateRandomKey()
        val secretKey2 = secretKeyGenerator.generateRandomKey()

        // Verify that the two keys are different
        // Note: There is a very small probability that two randomly generated keys could be identical,
        // but this is extremely unlikely and would indicate an issue with the random number generator
        Assert.assertFalse(secretKey1.encoded.contentEquals(secretKey2.encoded))
    }
}
