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
 * Data class to hold parameter specifications for cryptographic key generation.
 *
 * This class encapsulates all the necessary information to generate a new cryptographic key pair,
 * including a description for logging, the algorithm, the padding scheme to be associated with the key,
 * and the detailed algorithm parameter specification.
 *
 * @property description A descriptive string for the specification, useful for logging and debugging.
 * @property algorithm The key generation algorithm, typically "RSA".
 * @property encryptionPaddings The encryption paddings supported for the key generation,
 * (e.g., [android.security.keystore.KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1]).
 * @property algorithmParameterSpec The detailed key generation parameter specification, such as
 * [android.security.keystore.KeyGenParameterSpec] or [android.security.KeyPairGeneratorSpec].
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

    private fun getDigestAlgorithms(): Array<String> {
        return digestAlgorithms.toTypedArray()
    }

    private fun getEncryptionPaddings(): Array<String> {
        return encryptionPaddings.toTypedArray()
    }

    /**
     * Helper method to create an appropriate key generation parameter specification.
     *
     * This method configures the specification with the appropriate padding and digest
     * algorithms based on feature flags. It supports both OAEP (stronger) and PKCS1
     * (more compatible) padding schemes.
     *
     * @return A [KeyGenParameterSpec] configured according to current settings
     */
    override val algorithmParameterSpec: AlgorithmParameterSpec =
        KeyGenParameterSpec.Builder(keyAlias, purposes)
            .setKeySize(keySize)
            .setDigests(*getDigestAlgorithms())
            .setEncryptionPaddings(*getEncryptionPaddings())
            .build()

}

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

