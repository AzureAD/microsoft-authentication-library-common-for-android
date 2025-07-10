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
import android.util.Log
import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil
import com.microsoft.identity.common.java.controllers.ExceptionAdapter
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.java.opentelemetry.SpanName
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.StatusCode
import java.security.KeyPair
import javax.crypto.SecretKey


/**
 * Implementation of [IKekManager] that uses Android KeyStore system with RSA key pairs
 * for key encryption key (KEK) management.
 *
 *
 * This class is responsible for generating RSA key pairs used for wrapping (encrypting) and
 * unwrapping (decrypting) secret keys used in the application. It leverages the Android KeyStore
 * system for secure key storage and handling.
 *
 *
 * The implementation provides robustness through a fallback mechanism that tries multiple
 * cipher and key generation specifications in order of preference, allowing for compatibility
 * across different Android API levels and device capabilities.
 */
class AndroidKeyStoreRsaKekManager @JvmOverloads constructor(
    private val keyAlias: String,
    context: Context,
    private val cryptoParameterSpecFactory: CryptoParameterSpecFactory =
        CryptoParameterSpecFactory(context, keyAlias)
) : IKekManager {

    companion object {
        private val TAG: String = AndroidKeyStoreRsaKekManager::class.java.simpleName

    }

    /**
     * Unwraps (decrypts) a previously wrapped secret key using the RSA private key
     * from the Android KeyStore.
     *
     *
     * This method tries multiple cipher specifications in order of preference to provide
     * maximum compatibility across different Android API versions and devices. If the primary
     * specification fails, it will attempt fallback specifications.
     *
     * @param wrappedSecretKey The wrapped (encrypted) key as a byte array
     * @param secretKeyAlgorithm The algorithm of the secret key (e.g., "AES")
     * @return The unwrapped plaintext SecretKey
     * @throws ClientException if the key cannot be unwrapped due to missing key pair,
     * invalid wrapped key data, or unsupported cipher specifications
     */
    @Throws(ClientException::class)
    override fun unwrapKey(keyPair: KeyPair, wrappedSecretKey: ByteArray, secretKeyAlgorithm: String): SecretKey {
        val cipherParamsSpec = selectCompatibleCipherSpec(keyPair)
        Log.i(TAG, "Unwrapping key with CipherSpec: $cipherParamsSpec")
        return AndroidKeyStoreUtil.unwrap(
            wrappedSecretKey,
            secretKeyAlgorithm,
            keyPair,
            cipherParamsSpec.transformation,
            cipherParamsSpec.algorithmParameterSpec
        )
    }


    /**
     * Wraps (encrypts) a secret key using the RSA public key from the Android KeyStore.
     *
     *
     * If no key pair exists for the specified alias, this method will automatically
     * generate a new key pair before performing the wrapping operation.
     *
     * @param keyToWrap The plaintext secret key that needs to be wrapped
     * @return The wrapped (encrypted) key as a byte array
     * @throws ClientException if key generation fails or if wrapping operation fails
     */
    @Throws(ClientException::class)
    override fun wrapKey(keyToWrap: SecretKey): ByteArray {
        val methodTag = "$TAG:wrapKey"
        var keyPair = AndroidKeyStoreUtil.readKey(keyAlias)
        if (keyPair == null) {
            Logger.info(methodTag, "No existing keypair found for alias. Generating a new keypair.")
            keyPair = generateKeyPair()
        }
        val cipherParamsSpec = selectCompatibleCipherSpec(keyPair)
        Log.i(TAG, "Wrapping key with cipher: $cipherParamsSpec")
        return AndroidKeyStoreUtil.wrap(
            keyToWrap,
            keyPair,
            cipherParamsSpec.transformation,
            cipherParamsSpec.algorithmParameterSpec
        )
    }

    /**
     * Selects the most appropriate [CipherSpec] for the given [KeyPair] by matching the supported
     * encryption paddings from the Android Keystore with a prioritized list of available cipher specs.
     *
     * This function attempts to find a compatible cipher configuration for key wrapping by:
     * 1. Fetching the encryption paddings supported by the provided [keyPair].
     * 2. Iterating through the prioritized list of [CipherSpec]s.
     * 3. Returning the first compatible spec where the padding is supported by the key.
     *
     * If no matching specification is found, a fallback using PKCS#1 padding is returned.
     *
     * @param keyPair The [KeyPair] for which a compatible [CipherSpec] should be determined.
     * @return A compatible [CipherSpec], or a fallback to a PKCS#1-based spec if none are supported.
     */
    private fun selectCompatibleCipherSpec(keyPair: KeyPair): CipherSpec {
        val methodTag = "$TAG:selectCompatibleCipherSpec"
        val supportedPaddings = AndroidKeyStoreUtil.getEncryptionPaddings(keyPair)
        val availableSpecs = cryptoParameterSpecFactory.getPrioritizedCipherParameterSpecs()
        Log.i(
            TAG,
            "Supported paddings by the keyPair: $supportedPaddings" +
                    ",Specs available in order of priority: $availableSpecs"
        )
        for (spec in availableSpecs) {
            for (padding in supportedPaddings) {
                if (spec.padding.contains(padding, ignoreCase = true)) {
                    return spec
                }
            }
        }
        Logger.warn(methodTag, "No supported cipher specification found for wrapping the key.")
        // Fallback to PKCS#1 padding if no compatible spec is found, instead of throwing an error.
        return cryptoParameterSpecFactory.getPkcs1CipherSpec()
    }

    /**
     * Generates a new RSA key pair and stores it in the Android KeyStore.
     *
     *
     * This method attempts to generate a key pair using multiple key generation specifications
     * in order of preference. If the primary specification fails, it will attempt fallback
     * specifications to ensure compatibility across different Android versions and devices.
     *
     *
     * The key generation process is traced using OpenTelemetry for monitoring and diagnostics.
     *
     * @return A new RSA KeyPair stored in the Android KeyStore
     * @throws ClientException if all key generation attempts fail
     */
    @Throws(ClientException::class)
    private fun generateKeyPair(): KeyPair {
        val methodTag = "$TAG:generateKeyPair"
        val span = OTelUtility.createSpanFromParent(SpanName.KeyPairGeneration.name, SpanExtension.current().spanContext)
        val failures = mutableListOf<Throwable>()
        val specs = cryptoParameterSpecFactory.getPrioritizedKeyGenParameterSpecs()

        try {
            SpanExtension.makeCurrentSpan(span).use { _ ->
                for (spec in specs) {
                    try {
                        val keypairGenStartTime = System.currentTimeMillis()
                        val keyPair = AndroidKeyStoreUtil.generateKeyPair(
                            spec.algorithm,
                            spec.algorithmParameterSpec
                        )
                        val elapsedTime = System.currentTimeMillis() - keypairGenStartTime
                        SpanExtension.current().setAttribute(AttributeName.elapsed_time_keypair_generation.name, elapsedTime)
                        span.setStatus(StatusCode.OK)
                        Log.i(TAG, "Key pair generated successfully with spec: $spec ")
                        return keyPair
                    } catch (throwable: Throwable) {
                        Logger.warn(methodTag, "Failed to generate key pair with spec: $spec")
                        failures.add(throwable)
                    }
                }

                // If we reach here, all attempts have failed
                failures.forEach { exception ->
                    Logger.error(methodTag, "Key pair generation failed with: ${exception.message}", exception)
                }
                val finalError = failures.lastOrNull() ?: ClientException(
                    ClientException.UNKNOWN_CRYPTO_ERROR,
                    "Key pair generation failed after trying all available specs."
                )
                span.setStatus(StatusCode.ERROR)
                span.recordException(finalError)
                throw ExceptionAdapter.clientExceptionFromException(finalError)
            }
        } finally {
            span.end()
        }
    }
}
