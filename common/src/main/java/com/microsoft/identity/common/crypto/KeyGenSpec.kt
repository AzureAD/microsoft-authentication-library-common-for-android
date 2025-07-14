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
package com.microsoft.identity.common.crypto

import android.content.Context
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import java.math.BigInteger
import java.security.spec.AlgorithmParameterSpec
import java.util.Calendar
import java.util.Locale
import javax.security.auth.x500.X500Principal

/**
 * Interface for key generation specifications.
 */
interface IKeyGenSpec {
    val keyAlias: String
    val description: String
    val algorithm: String
    val keySize: Int
    val encryptionPaddings: List<String>
    val algorithmParameterSpec: AlgorithmParameterSpec
    fun print(): String {
        return "KeyGenSpec(description='$description', algorithm='$algorithm', encryptionPaddings='$encryptionPaddings')"
    }
}

/**
 * Modern key generation specification for Android API 23+.
 *
 * Uses [KeyGenParameterSpec] with configurable purposes, digest algorithms, and encryption paddings.
 *
 * @property purposes Key usage purposes (e.g., ENCRYPT, DECRYPT, WRAP_KEY)
 * @property digestAlgorithms Supported digest algorithms (e.g., SHA-256, SHA-512)
 * @property keyAlias Unique identifier for the key in KeyStore
 * @property keySize RSA key size in bits (typically 2048)
 * @property description Human-readable identifier for logging
 * @property algorithm Key generation algorithm (typically "RSA")
 * @property encryptionPaddings Supported padding schemes (e.g., PKCS1, OAEP)
 */
data class KeyGenSpec(
    private val purposes: Int,
    private val digestAlgorithms: List<String>,
    override val keyAlias: String,
    override val keySize: Int,
    override val description: String,
    override val algorithm: String,
    override val encryptionPaddings: List<String>,
) : IKeyGenSpec {
    override fun toString() = print()

    /**
     * Converts digest algorithms list to array format required by KeyGenParameterSpec.
     */
    private fun getDigestAlgorithms(): Array<String> {
        return digestAlgorithms.toTypedArray()
    }

    /**
     * Converts encryption paddings list to array format required by KeyGenParameterSpec.
     */
    private fun getEncryptionPaddings(): Array<String> {
        return encryptionPaddings.toTypedArray()
    }

    override val algorithmParameterSpec: AlgorithmParameterSpec =
        KeyGenParameterSpec.Builder(keyAlias, purposes)
            .setKeySize(keySize)
            .setDigests(*getDigestAlgorithms())
            .setEncryptionPaddings(*getEncryptionPaddings())
            .build()

}

/**
 * Legacy key generation specification for Android API < 23.
 *
 * Uses [KeyPairGeneratorSpec] to generate self-signed certificates with 100-year validity.
 * Provides fallback compatibility for devices that don't support modern KeyStore APIs.
 *
 * @property context Android context for KeyStore access
 * @property keyAlias Unique identifier for the key in KeyStore
 * @property keySize RSA key size in bits (typically 2048)
 * @property description Human-readable identifier for logging
 * @property algorithm Key generation algorithm (typically "RSA")
 * @property encryptionPaddings Supported padding schemes (typically PKCS1 only)
 */
data class LegacyKeyGenSpec(
    private val context: Context,
    override val keyAlias: String,
    override val keySize: Int,
    override val description: String,
    override val algorithm: String,
    override val encryptionPaddings: List<String>,
) : IKeyGenSpec {

    override val algorithmParameterSpec = getLegacyKeyGenParamSpec()

    override fun toString() = print()

    /**
     * Creates legacy KeyPairGeneratorSpec with self-signed certificate.
     *
     * Generates certificate with 100-year validity using app package name as issuer.
     */
    private fun getLegacyKeyGenParamSpec(): AlgorithmParameterSpec {
        // Generate a self-signed cert.
        val certInfo = String.format(
            Locale.ROOT, "CN=%s, OU=%s",
            keyAlias,
            context.packageName
        )
        val start = Calendar.getInstance()
        val end = Calendar.getInstance()
        val certValidYears = 100
        end.add(Calendar.YEAR, certValidYears)

        return KeyPairGeneratorSpec.Builder(context)
            .setAlias(keyAlias)
            .setSubject(X500Principal(certInfo))
            .setSerialNumber(BigInteger.ONE)
            .setStartDate(start.time)
            .setEndDate(end.time)
            .build()
    }
}
