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
    private val enableKeyGenEncryptionPaddingRsaOaep =
        flightsProvider.isFlightEnabled(CommonFlight.ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING)

    init {
        val methodTag = "$TAG:init"
        Logger.verbose(
            methodTag,
            "Initialized CryptoParameterSpecFactory - " +
                    "API: ${Build.VERSION.SDK_INT}, " +
                    "flags: [purposeKey=$keySpecWithPurposeKey, " +
                    "withoutPurposeKey=$keySpecWithoutPurposeKey, " +
                    "oaepSupported=$enableKeyGenEncryptionPaddingRsaOaep]"
        )
    }

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

    // Key generation parameter specifications
    @delegate:RequiresApi(Build.VERSION_CODES.P)
    private val keyGenParamSpecWithPurposeWrapKey by lazy {
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
            encryptionPaddings = getEncryptionPaddingsForKeyGen(),
            algorithm = RSA_ALGORITHM
        )
    }

    @delegate:RequiresApi(Build.VERSION_CODES.M)
    private val keyGenParamSpecWithoutPurposeWrapKey by lazy {
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
            encryptionPaddings = getEncryptionPaddingsForKeyGen(),
            algorithm = RSA_ALGORITHM
        )
        }

    private val keyGenParamSpecLegacy = LegacyKeyGenSpec(
        context = context,
        keyAlias = keyAlias,
        keySize = KEY_SIZE,
        description = LEGACY_SPEC,
        encryptionPaddings = listOf(PKCS1_PADDING),
        algorithm = RSA_ALGORITHM
    )

    //TODO: check if additional flag is needed to enable OAEP padding
    private fun getEncryptionPaddingsForKeyGen(): List<String> {
        val paddings = mutableListOf(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
        if (enableKeyGenEncryptionPaddingRsaOaep) {
            paddings.add(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
        }
        return paddings
    }


    /**
     * Returns cipher specifications ordered by security preference.
     *
     * The list prioritizes OAEP padding (when enabled by feature flag) over PKCS1
     * for better security, with PKCS1 always included for compatibility.
     *
     * @return List of [CipherSpec] objects ordered by preference (most secure first)
     */
    fun getPrioritizedCipherParameterSpecs(): List<CipherSpec> {
        val methodTag = "$TAG:getPrioritizedCipherParameterSpecs"
        val specs = listOf(oaepCipherSpec, pkcs1CipherSpec)
        Logger.info(methodTag, "Ciphers: $specs")
        return specs
    }


    /**
     * Returns key generation specifications ordered by API level compatibility.
     *
     * Prioritizes modern Android KeyStore features (API 23+) when enabled by feature flags,
     * with legacy fallback always included for maximum compatibility.
     *
     * @return List of [IKeyGenSpec] objects ordered by preference (most modern first)
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

        Logger.info(methodTag, "Key generation specs: ${specs.joinToString { it.description }}")
        return specs
    }
}
