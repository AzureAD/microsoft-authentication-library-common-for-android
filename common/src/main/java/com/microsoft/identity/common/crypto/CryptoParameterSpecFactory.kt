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
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.logging.Logger

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
    val flightsProvider: IFlightsProvider = getFlightsProvider()
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
        private const val CONSERVATIVE_SPEC = "conservative_spec_for_api_30_and_below"
        private const val LEGACY_SPEC = "legacy_key_gen_spec"
    }

    // Feature flags to control which key generation specs to use. (evaluated every time accessed)
    private val keySpecWithWrapPurposeKey get() =
        flightsProvider.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY)
    private val keySpecWithoutWrapPurposeKey get() =
        flightsProvider.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY)
    private val enableKeyGenEncryptionPaddingRsaOaep get() =
        flightsProvider.isFlightEnabled(CommonFlight.ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING)
    // When enabled, advanced key gen specs are skipped on API <= 30 in favour of a conservative
    // RSA/PKCS1/SHA-256 spec that legacy keymasters reliably support. Killable via ECS.
    private val conservativeKeyGenForLegacyDevices get() =
        flightsProvider.isFlightEnabled(CommonFlight.ENABLE_CONSERVATIVE_KEY_GEN_SPEC_FOR_LEGACY_DEVICES)

    init {
        val methodTag = "$TAG:init"
        Logger.verbose(
            methodTag,
            "Initialized CryptoParameterSpecFactory - " +
                    "API: ${Build.VERSION.SDK_INT}, " +
                    "flags: [keySpecWithWrapPurposeKey=$keySpecWithWrapPurposeKey, " +
                    "keySpecWithoutWrapPurposeKey=$keySpecWithoutWrapPurposeKey, " +
                    "oaepSupported=$enableKeyGenEncryptionPaddingRsaOaep, " +
                    "conservativeKeyGenForLegacyDevices=$conservativeKeyGenForLegacyDevices]"
        )
    }

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

    /**
     * Conservative, hardware-friendly key generation spec for API 23..30 (pre-Keystore 2.0).
     *
     * Requests only RSA with ENCRYPT/DECRYPT purposes, a single SHA-256 digest and PKCS1
     * padding. It deliberately avoids PURPOSE_WRAP_KEY, SHA-512 and OAEP/MGF1 - features that
     * many hardware-backed keymasters on API <= 30 (notably rugged/enterprise devices) reject,
     * causing key generation to fail. A key produced with PKCS1 padding is also reliably
     * introspectable via [android.security.keystore.KeyInfo], so a compatible cipher spec is
     * always found when wrapping the secret key.
     */
    private val keyGenParamSpecConservative by lazy {
        KeyGenSpec(
            keyAlias = keyAlias,
            purposes = KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT,
            keySize = KEY_SIZE,
            digestAlgorithms = listOf(
                KeyProperties.DIGEST_SHA256
            ),
            description = CONSERVATIVE_SPEC,
            encryptionPaddings = listOf(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1),
            algorithm = RSA_ALGORITHM
        )
    }

    private val keyGenParamSpecLegacy = LegacyKeyGenSpec(
        context = context,
        keyAlias = keyAlias,
        keySize = KEY_SIZE,
        description = LEGACY_SPEC,
        encryptionPaddings = listOf("PKCS1Padding"),
        algorithm = RSA_ALGORITHM
    )

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
        val specs = listOf(
            CipherSpec.oaepCipherSpec,
            CipherSpec.pkcs1CipherSpec
        )
        Logger.info(methodTag, "Ciphers: $specs")
        return specs
    }

    /**
     * Returns key generation specifications ordered by API level compatibility.
     *
     * Prioritizes modern Android KeyStore features (API 23+) when enabled by feature flags,
     * with legacy fallback always included for maximum compatibility.
     *
     * When [CommonFlight.ENABLE_CONSERVATIVE_KEY_GEN_SPEC_FOR_LEGACY_DEVICES] is enabled, the
     * advanced specs (PURPOSE_WRAP_KEY / SHA-512 / OAEP-MGF1) are skipped on API &lt;= 30 - where
     * pre-Keystore 2.0 keymasters frequently reject them - in favour of a conservative
     * RSA/PKCS1/SHA-256 spec. When the flight is disabled the previous behaviour is restored so the
     * change can be turned off via ECS.
     *
     * @return List of [IKeyGenSpec] objects ordered by preference (most modern first)
     */
    fun getPrioritizedKeyGenParameterSpecs(): List<IKeyGenSpec> {
        val methodTag = "$TAG:getPrioritizedKeyGenParameterSpecs"
        val conservativeFixEnabled = conservativeKeyGenForLegacyDevices

        // Record whether the fix flight was on for this attempt so we can correlate, post-deploy,
        // against the existing DeviceInfo_OsVersion and key_pair_gen_description telemetry.
        SpanExtension.current().setAttribute(
            AttributeName.key_gen_conservative_spec_flight_enabled.name,
            conservativeFixEnabled
        )

        // The fix is an ECS kill switch. When the flight is OFF we run the exact original spec
        // selection; when it is ON we run the legacy-device-safe variant. Keeping the two paths
        // separate makes the change trivial to reason about and to revert via ECS.
        val specs = if (conservativeFixEnabled) {
            getLegacyDeviceSafeKeyGenSpecs()
        } else {
            getDefaultKeyGenSpecs()
        }

        Logger.info(methodTag, "Key generation specs: ${specs.joinToString { it.description }}")
        return specs
    }

    /**
     * Original key generation spec selection (fix flight OFF) - unchanged behaviour.
     *
     * The advanced specs (PURPOSE_WRAP_KEY / SHA-512 / OAEP-MGF1) are attempted only on API levels
     * that support them (e.g., wrap-key spec on API 28+; non-wrap spec on API 23+) when their
     * corresponding feature flags are enabled. See [getLegacyDeviceSafeKeyGenSpecs] for the fixed path.
     */
    private fun getDefaultKeyGenSpecs(): List<IKeyGenSpec> {
        val specs = mutableListOf<IKeyGenSpec>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && keySpecWithWrapPurposeKey) {
            // First priority: API 28+ with PURPOSE_WRAP_KEY if enabled
            specs.add(keyGenParamSpecWithPurposeWrapKey)
        }

        if (keySpecWithoutWrapPurposeKey) {
            // Second priority: API 23+ without PURPOSE_WRAP_KEY
            specs.add(keyGenParamSpecWithoutPurposeWrapKey)
        }

        // Always include legacy spec as last resort fallback (API < 23).
        specs.add(keyGenParamSpecLegacy)
        return specs
    }

    /**
     * Legacy-device-safe key generation spec selection (fix flight ON).
     *
     * On API &lt;= 30 the advanced specs are skipped entirely - pre-Keystore 2.0 keymasters
     * (especially on rugged/enterprise devices) reject PURPOSE_WRAP_KEY / SHA-512 / OAEP-MGF1, which
     * previously made every attempt fail with "All key generation attempts failed". Instead a
     * conservative RSA/PKCS1/SHA-256 spec - which those keymasters reliably support - is used. On
     * API 31+ (Keystore 2.0) the advanced specs are still attempted first, with the conservative
     * spec added as an extra fallback before the deprecated legacy spec.
     */
    private fun getLegacyDeviceSafeKeyGenSpecs(): List<IKeyGenSpec> {
        val specs = mutableListOf<IKeyGenSpec>()

        // Advanced specs are only reliable from Keystore 2.0 (API 31) onward.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (keySpecWithWrapPurposeKey) {
                specs.add(keyGenParamSpecWithPurposeWrapKey)
            }
            if (keySpecWithoutWrapPurposeKey) {
                specs.add(keyGenParamSpecWithoutPurposeWrapKey)
            }
        }

        // Conservative, hardware-friendly spec (RSA, ENCRYPT|DECRYPT, SHA-256, PKCS1 only).
        // Primary viable spec on API 23..30 keymasters; a safe fallback on API 31+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            specs.add(keyGenParamSpecConservative)
        // Always include legacy spec as last resort fallback.
        specs.add(keyGenParamSpecLegacy)
        return specs
    }
}
