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
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager.getFlightsProvider
import com.microsoft.identity.common.logging.Logger
import java.math.BigInteger
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.MGF1ParameterSpec
import java.util.Calendar
import java.util.Locale
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.security.auth.x500.X500Principal

/**
 * Factory class to create various cryptographic parameter specifications
 * for key generation and cipher operations.
 *
 * This class encapsulates the logic to determine which key generation and cipher specs
 * to use based on the Android API level and feature flags. It implements a fallback mechanism
 * to ensure compatibility across different Android versions and device implementations.
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
class CryptoParameterSpecFactory(private val context: Context, private val keyAlias: String) {


    data class KeyGenSpec(
        val keyGenParameterSpec: AlgorithmParameterSpec,
        val description: String
    )

    data class CipherSpec(
        val algorithmParameterSpecs: AlgorithmParameterSpec?,
        val transformation: String
    )

    companion object {
        private val TAG = CryptoParameterSpecFactory::class.java.simpleName
        private const val KEY_SIZE: Int = 2048
        private const val MODERN_SPEC_WITH_PURPOSE_WRAP_KEY = "modern_spec_with_wrap_key"
        private const val MODERN_SPEC_WITHOUT_PURPOSE_WRAP_KEY = "modern_spec_without_wrap_key"
        private const val LEGACY_SPEC = "legacy_key_gen_spec"
        private const val OAEP_TRANSFORMATION = "RSA/NONE/OAEPwithSHA-256andMGF1Padding"
        private const val PKCS1_TRANSFORMATION = "RSA/ECB/PKCS1Padding"

        private val oaepSpec  = OAEPParameterSpec(
            "SHA-256",  // main digest
            "MGF1",  // mask generation function
            MGF1ParameterSpec.SHA1,  // MGF1 digest
            PSource.PSpecified.DEFAULT // label (usually default)
        )
    }

    private val legacyCipherSpec = CipherSpec(
        algorithmParameterSpecs = null,
        transformation = PKCS1_TRANSFORMATION
    )

    private val oaepCipherSpec = CipherSpec(
        algorithmParameterSpecs = oaepSpec,
        transformation = OAEP_TRANSFORMATION
    )

    @RequiresApi(Build.VERSION_CODES.P)
    private val keyGenParamSpecWithPurposeWrapKey = KeyGenSpec(
        keyGenParameterSpec = getAlgorithmParameterSpec(
            KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT or
                    KeyProperties.PURPOSE_WRAP_KEY
        ),
        description = MODERN_SPEC_WITH_PURPOSE_WRAP_KEY
    )

    @RequiresApi(Build.VERSION_CODES.M)
    private val keyGenParamSpecWithoutPurposeWrapKey = KeyGenSpec(
        keyGenParameterSpec = getAlgorithmParameterSpec(
            KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
        ),
        description = MODERN_SPEC_WITHOUT_PURPOSE_WRAP_KEY
    )

    private val keyGenParamSpecLegacy = KeyGenSpec(
        keyGenParameterSpec = getLegacyKeyGenParamSpec(),
        description = LEGACY_SPEC
    )

    private val keySpecWithPurposeKey =
        getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY)
    private val keySpecWithoutPurposeKey =
        getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY)
    private val keySpecWithOAEP =
        getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING)

    init {
        val methodTag = "$TAG:init"
        Logger.info(
            methodTag,
            "Initialized with keyAlias: $keyAlias, API level: ${Build.VERSION.SDK_INT}, " +
                    "With flight flags - PurposeWrapKey: $keySpecWithPurposeKey, " +
                    "WithoutPurposeKey: $keySpecWithoutPurposeKey, " +
                    "WithOAEP: $keySpecWithOAEP"
        )
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
        val specs = mutableListOf<CipherSpec>()

        // Add OAEP padding spec first (if enabled) as it provides stronger security
        if (keySpecWithOAEP) {
            specs.add(oaepCipherSpec)
        }

        // Always include legacy PKCS1 padding as a fallback for compatibility
        specs.add(legacyCipherSpec)

        Logger.info(methodTag, "Options: ${specs.joinToString { it.transformation }}")
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
    fun getPrioritizedKeyGenParameterSpecs(): List<KeyGenSpec> {
        val methodTag = "$TAG:getPrioritizedKeyGenParameterSpecs"

        val specs = mutableListOf<KeyGenSpec>()

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
        return specs
    }

    /**
     * Generates a legacy algorithm parameter specification using KeyPairGeneratorSpec.
     *
     * This approach is used for API levels below 23 (Android M) or as a fallback
     * when more modern specifications fail. It creates a self-signed certificate
     * with a 100-year validity period.
     *
     * @return A [KeyPairGeneratorSpec] configured for the key alias and application context
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

    /**
     * Helper method to create an appropriate key generation parameter specification.
     *
     * This method configures the specification with the appropriate padding and digest
     * algorithms based on feature flags. It supports both OAEP (stronger) and PKCS1
     * (more compatible) padding schemes.
     *
     * @param purposes The key usage purposes (combinations of KeyProperties.PURPOSE_* constants)
     * @return A [KeyGenParameterSpec] configured according to current settings
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun getAlgorithmParameterSpec(purposes: Int): AlgorithmParameterSpec {
        val methodTag = "$TAG:getSpecForWrappingKey"
        return if (keySpecWithOAEP) {
            Logger.info(methodTag, "Using OAEP padding with SHA-256 digest")
            KeyGenParameterSpec.Builder(keyAlias, purposes)
                .setKeySize(KEY_SIZE)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .build()
        } else {
            // Fallback to legacy spec if OAEP is not enabled.
            Logger.info(methodTag, "Using PKCS1 padding with SHA-256 and SHA-512 digests")
            KeyGenParameterSpec.Builder(keyAlias, purposes)
                .setKeySize(KEY_SIZE)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .build()
        }
    }
}
