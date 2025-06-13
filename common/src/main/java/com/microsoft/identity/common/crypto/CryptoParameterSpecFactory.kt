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
 * to use based on the Android API level and flight flags.
 *
 *
 * The Android Keystore (especially on older devices or some hardware-backed implementations) has limited support for MGF1 digests.
 *
 * Specifically:
 *
 * It supports OAEP with:
 *
 * Main Digest: SHA-256 ✅
 *
 * MGF1 Digest: SHA-1 ✅
 *
 * But not:
 *
 * MGF1 Digest: SHA-256 ❌ (on many devices)
 *
 */
class CryptoParameterSpecFactory(private val context: Context, private val keyAlias: String) {

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

        private val legacyCipherSpec = CipherSpec(
            algorithmParameterSpecs = null,
            transformation = PKCS1_TRANSFORMATION
        )

        private val oaepCipherSpec = CipherSpec(
            algorithmParameterSpecs = oaepSpec,
            transformation = OAEP_TRANSFORMATION
        )
    }

    data class KeyGenSpec(
        val keyGenParameterSpec: AlgorithmParameterSpec,
        val description: String
    )

    data class CipherSpec(
        val algorithmParameterSpecs: AlgorithmParameterSpec?,
        val transformation: String
    )

    private val keySpecWithPurposeKey =
        getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY)
    private val keySpecWithoutPurposeKey =
        getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY)
    private val keySpecWithOAEP = true
        //getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING)

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

    fun getPrimaryCipherParameterSpec(): CipherSpec {
        val methodTag = "$TAG:getCipherTransformation"
        val spec = if (keySpecWithOAEP) {
            oaepCipherSpec
        } else {
            legacyCipherSpec
        }
        Logger.info(methodTag, "Using cipher transformation: ${spec.transformation}")
        return spec
    }

    fun getPrioritizedCipherParameterSpecs(): List<CipherSpec> {
        val specs = mutableListOf<CipherSpec>()
        if (keySpecWithOAEP) {
            specs.add(oaepCipherSpec)
        }
        specs.add(legacyCipherSpec)
        return specs
    }

    /**
     * Returns a prioritized list of AlgorithmParameterSpec objects to try in sequence.
     * This helps handle fallback scenarios where the preferred spec might not work
     * on all devices or with all existing keys.
     *
     * @return List of AlgorithmParameterSpec objects ordered by priority (highest first)
     */
    fun getPrioritizedKeyGenParameterSpecs(): List<KeyGenSpec> {
        val methodTag = "$TAG:getPrioritizedKeyGenParameterSpecs"

        val specs = mutableListOf<KeyGenSpec>()
        // Add specs in order of preference
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && keySpecWithPurposeKey) {
            // First priority: API 28+ with PURPOSE_WRAP_KEY if enabled
            specs.add(
                KeyGenSpec(
                    getKeyGenParamSpecWithPurposeWrapKey(),
                    MODERN_SPEC_WITH_PURPOSE_WRAP_KEY
                )
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && keySpecWithoutPurposeKey) {
            // Second priority: API 23+ without PURPOSE_WRAP_KEY
            specs.add(
                KeyGenSpec(
                    getKeyGenParamSpecWithoutPurposeWrapKey(),
                    MODERN_SPEC_WITHOUT_PURPOSE_WRAP_KEY
                )
            )
        }

        // Always include legacy spec as last resort fallback
        specs.add(KeyGenSpec(getLegacyKeyGenParamSpec(), LEGACY_SPEC))

        Logger.info(
            methodTag,
            "Created prioritized specs list with ${specs.size} options: ${specs.joinToString { it.description }}"
        )
        return specs
    }

    /**
     * Generate a self-signed cert and derive an AlgorithmParameterSpec from that.
     * This is for the key to be generated in {@link KeyStore} via {@link KeyPairGenerator}
     * Note : This is now only for API level < 23 or as fallback.
     *
     * @return a {@link AlgorithmParameterSpec} for the keystore key (that we'll use to wrap the secret key).
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


    @RequiresApi(Build.VERSION_CODES.P)
    private fun getKeyGenParamSpecWithPurposeWrapKey(): AlgorithmParameterSpec {
        return getAlgorithmParameterSpec(
            KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT or
                    KeyProperties.PURPOSE_WRAP_KEY
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getKeyGenParamSpecWithoutPurposeWrapKey(): AlgorithmParameterSpec {
        return getAlgorithmParameterSpec(
            KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
        )
    }

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
