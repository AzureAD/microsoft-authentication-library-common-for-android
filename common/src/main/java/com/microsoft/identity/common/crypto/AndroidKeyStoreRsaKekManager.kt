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

        val keyPair = AndroidKeyStoreUtil.readKey(keyAlias) ?: run {
            val error = ClientException(
                ClientException.KEY_LOAD_FAILURE,
                "No existing keypair found for alias: $keyAlias"
            )
            Logger.error(methodTag, error.message, error)
            throw error
        }
        return executeWithFallbacks(
            specs = cryptoParameterSpecFactory.getPrioritizedCipherParameterSpecs(),
            spanName = SpanName.KeyPairUnWrap.name,
            operation = { cipherParameterSpec ->
                AndroidKeyStoreUtil.unwrap(
                    wrappedSecretKey,
                    secretKeyAlgorithm,
                    keyPair,
                    cipherParameterSpec.transformation,
                    cipherParameterSpec.algorithmParameterSpec
                )
            }
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
        val cipherParamsSpec = cryptoParameterSpecFactory.getPrioritizedCipherParameterSpecs().first()
        return AndroidKeyStoreUtil.wrap(
            keyToWrap,
            keyPair,
            cipherParamsSpec.transformation,
            cipherParamsSpec.algorithmParameterSpec
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
        return executeWithFallbacks(
            specs = cryptoParameterSpecFactory.getPrioritizedKeyGenParameterSpecs(),
            spanName = SpanName.KeyPairGeneration.name,
            operation = { keyGenParameterSpec ->
                val keypairGenStartTime = System.currentTimeMillis()
                val keyPair = AndroidKeyStoreUtil.generateKeyPair(
                    keyGenParameterSpec.algorithm,
                    keyGenParameterSpec.algorithmParameterSpec
                )
                val elapsedTime = System.currentTimeMillis() - keypairGenStartTime
                SpanExtension.current().setAttribute(AttributeName.elapsed_time_keypair_generation.name, elapsedTime)
                return@executeWithFallbacks keyPair
            }
        )
    }

    /**
     * Executes a cryptographic operation with a fallback mechanism, iterating through a list of
     * specifications until the operation succeeds.
     *
     * This generic function is designed to handle operations that might fail with certain
     * configurations, providing resilience by attempting multiple alternatives. It also integrates
     * with OpenTelemetry to trace the execution and log relevant information for monitoring.
     *
     * @param T The type of the specification object.
     * @param R The return type of the cryptographic operation.
     * @param specs A list of specifications to try in order.
     * @param spanName The name for the OpenTelemetry span that will trace the operation.
     * @param operation A lambda function that takes a spec and performs the cryptographic operation, returning the result.
     * @return The result of the successful cryptographic operation.
     * @throws ClientException if all attempts fail.
     */
    private fun <T, R> executeWithFallbacks(
        specs: List<T>,
        spanName: String,
        operation: (T) -> R
    ): R {
        val methodTag = "$TAG:executeWithFallbacks"
        val span = OTelUtility.createSpanFromParent(spanName, SpanExtension.current().spanContext)
        val failures = mutableListOf<Throwable>()

        try {
            SpanExtension.makeCurrentSpan(span).use { _ ->
                for (spec in specs) {
                    try {
                        val result = operation(spec)
                        span.setStatus(StatusCode.OK)
                        //Logger.info(methodTag, "Successfully executed ${spanName} with spec: ${spec.getDescription()}")
                        return result
                    } catch (throwable: Throwable) {
                        Logger.warn(methodTag, "Failed to execute $spanName with")
                        failures.add(throwable)
                    }
                }

                // If we reach here, all attempts have failed
                failures.forEach { exception ->
                    Logger.error(methodTag, "Operation failed with: ${exception.message}", exception)
                }
                val finalError = failures.lastOrNull() ?: ClientException(
                    ClientException.UNKNOWN_CRYPTO_ERROR,
                    "Operation failed after trying all available specs."
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
