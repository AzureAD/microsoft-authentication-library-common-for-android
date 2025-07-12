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
import android.os.Build
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager.getFlightsProvider
import com.microsoft.identity.common.java.flighting.IFlightsProvider
import com.microsoft.identity.common.logging.Logger
import java.security.spec.MGF1ParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

/**
 * A factory for creating cryptographic parameter specifications for key generation and cipher operations.
 *
 * This class encapsulates the logic for determining the appropriate key generation and cipher
 * specifications based on the Android API level and configurable feature flags. It provides a
 * fallback mechanism to ensure compatibility across different Android versions and device-specific
 * hardware implementations.
 *
 * Key features:
 * - Creates appropriate key generation specifications based on Android API level
 * - Supports both modern (API 23+) and legacy key specifications
 * - Provides options for different padding schemes (OAEP, PKCS1)
 * - Configurable through feature flags for testing different implementations
 *
 * The Android Keystore (especially on older devices or some hardware-backed implementations) has limited support for MGF1 digests.
 *
 * Specifically:
 *
 * It supports OAEP with:
 * - Main Digest: SHA-256 ✅
 * - MGF1 Digest: SHA-1 ✅
 *
 * But not:
 * - MGF1 Digest: SHA-256 ❌ (on many devices)
 *
 * This factory helps navigate these limitations by providing appropriate fallback mechanisms.
 */
class CryptoParameterSpecFactory(
    context: Context,
    keyAlias: String,
    flightsProvider: IFlightsProvider = getFlightsProvider()
) {

    private companion object {
        private val TAG = CryptoParameterSpecFactory::class.java.simpleName

        // Algorithm used for RSA key generation and encryption
        private const val RSA_ALGORITHM = "RSA"

        // Default key size for RSA keys
        private const val KEY_SIZE: Int = 2048

        // Descriptive identifiers for different key generation specifications
        private const val MODERN_SPEC_WITH_PURPOSE_WRAP_KEY = "modern_spec_with_wrap_key"
        private const val MODERN_SPEC_WITHOUT_PURPOSE_WRAP_KEY = "modern_spec_without_wrap_key"
        private const val LEGACY_SPEC = "legacy_key_gen_spec"

        // Padding schemes and modes used in cipher operations
        private const val PKCS1_PADDING = "PKCS1Padding"
        private const val OAEP_PADDING_WITH_256MGF1 = "OAEPwithSHA-256andMGF1Padding"
        private const val MODE_ECB = "ECB"
        private const val MODE_NONE = "NONE"

        // OAEP parameter specification for RSA encryption
        private val OAEP_SPECS = OAEPParameterSpec(
            "SHA-256",  // main digest
            "MGF1",  // mask generation function
            MGF1ParameterSpec.SHA1,  // MGF1 digest
            PSource.PSpecified.DEFAULT // label (usually default)
        )
    }

    // Feature flags to control which key generation specs to use
    private val keySpecWithPurposeKey =
        flightsProvider.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY)
    private val keySpecWithoutPurposeKey =
        flightsProvider.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY)
    private val supportsKeyGenEncryptionPaddingRsaOaep =
        flightsProvider.isFlightEnabled(CommonFlight.ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING)


    // Cipher parameter specifications
    val pkcs1CipherSpec = CipherSpec(
        algorithmParameterSpec = null,
        algorithm = RSA_ALGORITHM,
        mode = MODE_ECB,
        padding = PKCS1_PADDING
    )

    private val oaepCipherSpec = CipherSpec(
        algorithmParameterSpec = OAEP_SPECS,
        algorithm = RSA_ALGORITHM,
        mode = MODE_NONE,
        padding = OAEP_PADDING_WITH_256MGF1,
    )


    @RequiresApi(Build.VERSION_CODES.P)
    private val keyGenParamSpecWithPurposeWrapKey =
        KeyGenSpec(
            keyAlias = keyAlias,
            purposes = KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT or
                    KeyProperties.PURPOSE_WRAP_KEY,
            keySize = KEY_SIZE,
            digestAlgorithms = listOf(
                KeyProperties.DIGEST_SHA256,
                KeyProperties.DIGEST_SHA512
            ),
            description = MODERN_SPEC_WITH_PURPOSE_WRAP_KEY,
            encryptionPaddings = getEncryptionPaddings(),
            algorithm = RSA_ALGORITHM
        )

    @RequiresApi(Build.VERSION_CODES.M)
    private val keyGenParamSpecWithoutPurposeWrapKey =
        KeyGenSpec(
            keyAlias = keyAlias,
            purposes = KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT,
            keySize = KEY_SIZE,
            digestAlgorithms = listOf(
                KeyProperties.DIGEST_SHA256,
                KeyProperties.DIGEST_SHA512
            ),
            description = MODERN_SPEC_WITHOUT_PURPOSE_WRAP_KEY,
            encryptionPaddings = getEncryptionPaddings(),
            algorithm = RSA_ALGORITHM
        )


    private val keyGenParamSpecLegacy = LegacyKeyGenSpec(
        context = context,
        keyAlias = keyAlias,
        keySize = KEY_SIZE,
        description = LEGACY_SPEC,
        encryptionPaddings = listOf(PKCS1_PADDING),
        algorithm = RSA_ALGORITHM
    )


    init {
        val methodTag = "$TAG:init"
        Logger.info(
            methodTag,
            "Initialized with keyAlias: $keyAlias, API level: ${Build.VERSION.SDK_INT}, " +
                    "With flight flags - PurposeWrapKey: $keySpecWithPurposeKey, " +
                    "WithoutPurposeKey: $keySpecWithoutPurposeKey, " +
                    "supportsKeyGenEncryptionPaddingRsaOaep: $supportsKeyGenEncryptionPaddingRsaOaep"
        )
    }


    private fun getEncryptionPaddings(): List<String> {
        val paddings = mutableListOf(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
        if (supportsKeyGenEncryptionPaddingRsaOaep) {
            paddings.add(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
        }
        return paddings.toList()
    }

    /**
     * Returns a prioritized list of cipher parameter specifications to try in sequence.
     *
     * This allows the calling code to attempt operations with the most secure/preferred
     * specification first, then fall back to more compatible options if needed.
     *
     * The list is ordered with the most preferred specification first (OAEP if enabled, then PKCS1).
     *
     * @return List of [CipherSpec] objects ordered by preference (highest priority first)
     */
    fun getPrioritizedCipherParameterSpecs(): List<CipherSpec> {
        val methodTag = "$TAG:getPrioritizedCipherParameterSpecs"
        val specs = listOf(oaepCipherSpec, pkcs1CipherSpec)
        Logger.info(methodTag, "Ciphers: $specs")
        return specs
    }

    /**
     * Returns a prioritized list of key generation parameter specifications to try in sequence.
     *
     * This helps handle fallback scenarios where the preferred spec might not work
     * on all devices or with all existing keys. Each specification has a descriptive
     * identifier for logging and debugging purposes.
     *
     * The method considers:
     * 1. Android API level (supporting modern APIs from Android M/23 and P/28)
     * 2. Feature flags that enable/disable specific key generation approaches
     * 3. Backward compatibility with existing keys
     *
     * The list always includes a legacy specification as a last resort fallback option.
     *
     * @return List of [KeyGenSpec] objects ordered by priority (highest first)
     */
    fun getPrioritizedKeyGenParameterSpecs(): List<IKeyGenSpec> {
        val methodTag = "$TAG:getPrioritizedKeyGenParameterSpecs"

        val specs = mutableListOf<IKeyGenSpec>()

        // Add specs in order of preference
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && keySpecWithPurposeKey) {
            // First priority: API 28+ with PURPOSE_WRAP_KEY if enabled
            specs.add(keyGenParamSpecWithPurposeWrapKey)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && keySpecWithoutPurposeKey) {
            // Second priority: API 23+ without PURPOSE_WRAP_KEY
            specs.add(keyGenParamSpecWithoutPurposeWrapKey)
        }

        // Always include legacy spec as last resort fallback
        specs.add(keyGenParamSpecLegacy)

        Logger.info(methodTag, "Options: ${specs.joinToString { it.description }}")
        return specs.toList()
    }
}
