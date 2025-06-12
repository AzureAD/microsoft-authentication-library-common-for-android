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
import java.security.spec.AlgorithmParameterSpec
import java.util.LinkedList
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
class AndroidKeyStoreRsaKekManager(
    private val keyAlias: String,
    context: Context,
) : IKekManager {

    private val cryptoParameterSpecFactory: CryptoParameterSpecFactory = CryptoParameterSpecFactory(context, keyAlias)


    /**
     * Checks if a key encryption key (KEK) exists in the Android KeyStore for the specified alias.
     *
     * @return true if the key pair exists and is accessible, false otherwise
     * @throws ClientException if there's an error accessing the Android KeyStore
     */
    @Throws(ClientException::class)
    override fun kekExists(): Boolean {
        return AndroidKeyStoreUtil.readKey(keyAlias) != null
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
    override fun unwrapKey(wrappedSecretKey: ByteArray, secretKeyAlgorithm: String): SecretKey {
        val methodTag = "$TAG:unwrapKey"
        val keyPair = AndroidKeyStoreUtil.readKey(keyAlias)
        if (keyPair == null) {
            val clientException = ClientException(
                ClientException.KEY_LOAD_FAILURE,
                "No existing keypair found for alias: $keyAlias"
            )
            Logger.error(methodTag, clientException.message, clientException)
            throw clientException
        }
        val specs = cryptoParameterSpecFactory.getPrioritizedCipherParameterSpecs()
        val exceptions = LinkedList<Throwable>()
        for ((algorithmParameterSpecs, transformation) in specs) {
            try {
                // Attempt to unwrap the key using the current spec
                return AndroidKeyStoreUtil.unwrap(
                    wrappedSecretKey,
                    secretKeyAlgorithm,
                    keyPair,
                    transformation,
                    algorithmParameterSpecs
                )
            } catch (throwable: Throwable) {
                Logger.warn(
                    methodTag,
                    "Failed to unwrap key with spec: $transformation"
                )
                // Continue to the next spec if this one fails
                exceptions.add(throwable)
            }
        }
        for (exception in exceptions) {
            Logger.error(
                methodTag,
                "Exception encountered during key pair generation: " + exception.message,
                exception
            )
        }

        // If we've tried all specs and failed, set span status and throw the last exception
        if (exceptions.isEmpty()) {
            exceptions.add(
                ClientException(
                    ClientException.UNKNOWN_CRYPTO_ERROR,
                    "Failed to unwrap key after trying all available specs."
                )
            )
        }
        throw ExceptionAdapter.clientExceptionFromException(exceptions.last)
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
        val cipherSpecs = cryptoParameterSpecFactory.getPrimaryCipherParameterSpec()
        return AndroidKeyStoreUtil.wrap(
            keyToWrap,
            keyPair,
            cipherSpecs.transformation,
            cipherSpecs.algorithmParameterSpecs
        )
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
        val span = OTelUtility.createSpanFromParent(
            SpanName.KeyPairGeneration.name,
            SpanExtension.current().spanContext
        )
        val specs = cryptoParameterSpecFactory.getPrioritizedKeyGenParameterSpecs()
        // Track the last exception encountered to throw if all attempts fail
        val exceptions = LinkedList<Throwable>()

        try {
            SpanExtension.makeCurrentSpan(span).use { _ ->
                // Try each spec in order of priority
                for ((keyGenParameterSpec, description) in specs) {
                    try {
                        val keyPair = attemptKeyPairGeneration(keyGenParameterSpec)

                        // Log the success with a descriptive name for telemetry
                        span.setAttribute(
                            AttributeName.key_pair_gen_successful_method.name,
                            description
                        )
                        Logger.info(
                            methodTag,
                            "Successfully generated key pair using: $description"
                        )

                        // Return successful key pair
                        span.setStatus(StatusCode.OK)
                        return keyPair
                    } catch (throwable: Throwable) {
                        // Log the failure but continue to the next spec
                        Logger.warn(
                            methodTag,
                            "Failed to generate keypair with spec: $description"
                        )
                        throwable.message?.let {
                            span.setAttribute(
                                AttributeName.keypair_gen_exception.name,
                                it
                            )
                        }
                        exceptions.add(throwable)
                    }
                }
                for (exception in exceptions) {
                    Logger.error(
                        methodTag,
                        "Exception encountered during key pair generation: " + exception.message,
                        exception
                    )
                }

                // If we've tried all specs and failed, set span status and throw the last exception
                if (exceptions.isEmpty()) {
                    exceptions.add(
                        ClientException(
                            ClientException.UNKNOWN_CRYPTO_ERROR,
                            "Failed to generate key pair after trying all available specs."
                        )
                    )
                }
                span.setStatus(StatusCode.ERROR)
                span.recordException(exceptions.last)
                Logger.error(
                    methodTag,
                    "Failed to generate key pair with all available specs",
                    exceptions.last
                )
                throw ExceptionAdapter.clientExceptionFromException(exceptions.last)
            }
        } finally {
            span.end() // Span is ended only once, after all attempts
        }
    }

    /**
     * Attempts to generate a key pair using the provided algorithm parameter specification.
     *
     *
     * This method records the time taken for key generation for performance monitoring.
     *
     * @param keyPairGenSpec The algorithm parameter specification for key generation
     * @return The generated KeyPair
     * @throws ClientException if key generation fails
     */
    @Throws(ClientException::class)
    private fun attemptKeyPairGeneration(keyPairGenSpec: AlgorithmParameterSpec): KeyPair {
        val keypairGenStartTime = System.currentTimeMillis()
        val keyPair = AndroidKeyStoreUtil.generateKeyPair(KEK_ALGORITHM, keyPairGenSpec)
        recordKeyGenerationTime(keypairGenStartTime)
        return keyPair
    }

    /**
     * Records the elapsed time for key pair generation in the current span
     * for performance monitoring and diagnostics.
     *
     * @param keypairGenStartTime The timestamp when the key generation process started
     */
    private fun recordKeyGenerationTime(keypairGenStartTime: Long) {
        val elapsedTime = System.currentTimeMillis() - keypairGenStartTime
        SpanExtension.current()
            .setAttribute(AttributeName.elapsed_time_keypair_generation.name, elapsedTime)
    }


    /**
     * Returns the primary cipher transformation to be used for cryptographic operations.
     *
     *
     * The transformation string specifies the algorithm, mode, and padding in the format
     * "algorithm/mode/padding" (e.g., "RSA/ECB/PKCS1Padding").
     *
     * @return The cipher transformation string for cryptographic operations
     */
    override val cipherTransformation = cryptoParameterSpecFactory.getPrimaryCipherParameterSpec().transformation

    companion object {
        private val TAG: String = AndroidKeyStoreRsaKekManager::class.java.simpleName


        /**
         * Algorithm used to generate the RSA wrapping key.
         * RSA is used for asymmetric key wrapping operations where the public key
         * encrypts (wraps) the data key and the private key decrypts (unwraps) it.
         */
        private const val KEK_ALGORITHM = "RSA"
    }
}
