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
import androidx.annotation.VisibleForTesting
import com.microsoft.identity.common.crypto.wrappedsecretkey.WrappedSecretKey
import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil
import com.microsoft.identity.common.java.controllers.ExceptionAdapter
import com.microsoft.identity.common.java.crypto.key.AES256SecretKeyGenerator
import com.microsoft.identity.common.java.crypto.key.ISecretKeyProvider
import com.microsoft.identity.common.java.crypto.key.KeyUtil
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.java.opentelemetry.SpanName
import com.microsoft.identity.common.java.util.FileUtil
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.StatusCode
import java.io.File
import java.security.KeyPair
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.crypto.SecretKey

/**
 * A secret key provider that uses Android KeyStore to store and retrieve the secret key.
 * The secret key is wrapped using a KeyPair stored in the Android KeyStore.
 *
 * @param alias The alias for the key in the Android KeyStore.
 * @param filePath The file path where the wrapped secret key is stored.
 * @param context The context used to access the Android KeyStore and file system.
 */
class KeyStoreBackedSecretKeyProvider(
    context: Context,
    override val alias: String,
    private val filePath: String
) : ISecretKeyProvider {
    companion object {
        private const val TAG = "KeyStoreBackedSecretKeyProvider"

        /**
         * AES is 16 bytes (128 bits), thus PKCS#5 padding should not work, but in
         * Java AES/CBC/PKCS5Padding is default(!) algorithm name, thus PKCS5 here
         * probably doing PKCS7. We decide to go with Java default string.
         */
        const val AES_CBC_PKCS5_PADDING_TRANSFORMATION: String = "AES/CBC/PKCS5Padding"

        /**
         * Indicate that token item is encrypted with the key loaded in this class.
         */
        const val KEY_TYPE_IDENTIFIER: String = "A001"

        @VisibleForTesting
        const val KEY_FILE_SIZE: Int = 1024

        /**
         * SecretKey cache. Maps wrapped secret key file path to the SecretKey.
         */
        private val sKeyCacheMap: ConcurrentMap<String, SecretKey> = ConcurrentHashMap()
    }

    override val keyTypeIdentifier = KEY_TYPE_IDENTIFIER
    override val cipherTransformation = AES_CBC_PKCS5_PADDING_TRANSFORMATION

    private val cryptoParameterSpecFactory: CryptoParameterSpecFactory = CryptoParameterSpecFactory(
        context,
        alias
    )

    /**
     * File where the wrapped secret key is stored.
     */
    private val keyFile =
        File(context.getDir(context.packageName, Context.MODE_PRIVATE), filePath)

    @get:VisibleForTesting
    val keyFromCache: SecretKey?
        get() {
            clearCachedKeyIfCantLoadOrFileDoesNotExist()
            return sKeyCacheMap[filePath]
        }

    @VisibleForTesting
    fun clearKeyFromCache() {
        sKeyCacheMap.remove(filePath)
    }

    /**
     * Wipe all the data associated from this key.
     */
    @VisibleForTesting
    @Throws(ClientException::class)
    fun deleteSecretKeyFromStorage() {
        AndroidKeyStoreUtil.deleteKey(alias)
        FileUtil.deleteFile(keyFile)
        sKeyCacheMap.remove(filePath)
    }

    private fun clearCachedKeyIfCantLoadOrFileDoesNotExist() {
        // TODO: Replace on next OneAuth major release.
        val shouldClearCache = !AndroidWrappedKeyProvider.sSkipKeyInvalidationCheck &&
                (!AndroidKeyStoreUtil.canLoadKey(alias) || !keyFile.exists())
        if (shouldClearCache) {
            sKeyCacheMap.remove(filePath)
        }
    }

    /**
     * Returns the secret key. If the key is already cached, it returns the cached key.
     * If the key is not cached, it tries to read the key from storage.
     * If the key does not exist in storage, it generates a new secret key and caches it.
     *
     * @return SecretKey
     * @throws ClientException if there is an error reading or generating the key
     */
    @get:Throws(ClientException::class)
    @get:Synchronized
    override val key: SecretKey
        get() {
            val methodTag = "$TAG:getKey"

            keyFromCache?.let {
                return it
            }

            readSecretKeyFromStorage()?.let {
                sKeyCacheMap[filePath] = it
                Logger.verbose(
                    methodTag,
                    "Key loaded from storage and cached with thumbprint: " +
                            KeyUtil.getKeyThumbPrint(it)
                )
                return it
            }

            val newKey = generateNewSecretKey()
            sKeyCacheMap[filePath] = newKey
            Logger.verbose(
                methodTag,
                "New key is generated and cached with thumbprint: " +
                        KeyUtil.getKeyThumbPrint(newKey)
            )
            return newKey
        }

    /**
     * Generates a new secret key and wraps it using a KeyPair stored in the Android KeyStore.
     * If a KeyPair does not exist, it generates a new KeyPair.
     * This method will also clear the cached key if it cannot load the key or if the key file does not exist.
     *
     * @return SecretKey The newly generated secret key.
     * @throws ClientException if there is an error generating the key or wrapping it
     *
     */
    @Throws(ClientException::class)
    fun generateNewSecretKey(): SecretKey {
        /*
         * !!WARNING!!
         * Multiple apps as of Today (1/4/2022) can still share a linux user id, by configuring
         * the sharedUserId attribute in their Android Manifest file.  If multiple apps reference
         * the same value for sharedUserId and are signed with the same keys, they will use
         * the same AndroidKeyStore and may obtain access to the files and shared preferences
         * of other applications by invoking createPackageContext.
         *
         * Support for sharedUserId is deprecated, however some applications still use this Android capability.
         * See: https://developer.android.com/guide/topics/manifest/manifest-element
         *
         * To address apps in this scenario we will attempt to load an existing KeyPair
         * instead of immediately generating a new key pair.  This will use the same keypair
         * to encrypt the symmetric key generated separately for each
         * application using a shared linux user id... and avoid these applications from
         * stomping/overwriting one another's keypair.
         */
        val methodTag = "$TAG:generateNewSecretKey"
        val newSecretKey = AES256SecretKeyGenerator.generateRandomKey()
        val keyPair: KeyPair = AndroidKeyStoreUtil.readKey(alias)
            ?: run {
                Logger.info(methodTag, "No existing keypair found. Generating a new one.")
                generateKeyPair()
            }
        val wrappedSecretKey = wrapSecretKey(newSecretKey, keyPair)


        FileUtil.writeDataToFile(wrappedSecretKey.serialize(), keyFile)
        return newSecretKey
    }

    /**
     * Load the saved keystore-encrypted key. Will only do read operation.
     *
     * @return SecretKey. Null if there isn't any.
     */
    /* package */@Synchronized
    @Throws(ClientException::class)
    fun readSecretKeyFromStorage(): SecretKey? {
        val methodTag = "$TAG:readSecretKeyFromStorage"
        try {
            val keyPair = AndroidKeyStoreUtil.readKey(alias)
            if (keyPair == null) {
                Logger.info(methodTag, "key does not exist in keystore")
                deleteSecretKeyFromStorage()
                return null
            }
            val wrappedSecretKey = loadSecretKeyFromFile()
            if (wrappedSecretKey == null) {
                Logger.warn(methodTag, "Key file is empty")
                // Do not delete the KeyStoreKeyPair even if the key file is empty. This caused credential cache
                // to be deleted in Office because of sharedUserId allowing keystore to be shared amongst apps.
                FileUtil.deleteFile(keyFile)
                clearKeyFromCache()
                return null
            }
            return unwrapSecretKey(wrappedSecretKey, keyPair)
        } catch (e: ClientException) {
            // Reset KeyPair info so that new request will generate correct KeyPairs.
            // All tokens with previous SecretKey are not possible to decrypt.
            Logger.warn(
                methodTag, "Error when loading key from Storage, " +
                        "wipe all existing key data "
            )
            deleteSecretKeyFromStorage()
            throw e
        }
    }

    private fun wrapSecretKey(
        secretKey: SecretKey,
        keyPair: KeyPair
    ): WrappedSecretKey {
        val methodTag = "$TAG:wrapSecretKey"
        val span = OTelUtility.createSpanFromParent(
            SpanName.SecretKeyWrapping.name,
            SpanExtension.current().spanContext
        )

        return try {
            SpanExtension.makeCurrentSpan(span).use { _ ->
                span.setAttribute(AttributeName.secret_key_wrapping_operation.name, "WRAP")
                val cipherParamsSpec = getKeyPairCompatibleCipherSpecs(keyPair).firstOrNull()
                    ?: throw ClientException(
                        ClientException.UNKNOWN_CRYPTO_ERROR,
                        "No compatible cipher specs found for key pair: $keyPair"
                    )
                span.setAttribute(
                    AttributeName.secret_key_wrapping_transformation.name,
                    cipherParamsSpec.transformation
                )
                Logger.info(methodTag, "Wrapping secret key with cipher spec: $cipherParamsSpec")
                val wrappedKey = AndroidKeyStoreUtil.wrap(
                    secretKey,
                    keyPair,
                    cipherParamsSpec.transformation,
                    cipherParamsSpec.algorithmParameterSpec
                )
                span.setStatus(StatusCode.OK)
                WrappedSecretKey(
                    wrappedKeyData = wrappedKey,
                    algorithm = secretKey.algorithm,
                    cipherTransformation = cipherParamsSpec.transformation
                )
            }
        } catch (exception: Exception) {
            Logger.error(methodTag, "Failed to wrap secret key", exception)
            span.setStatus(StatusCode.ERROR)
            span.recordException(exception)
            throw exception
        } finally {
            span.end()
        }
    }

    private fun unwrapSecretKey(
        wrappedSecretKey: WrappedSecretKey,
        keyPair: KeyPair
    ): SecretKey {
        val methodTag = "$TAG:unwrapSecretKey"
        val span = OTelUtility.createSpanFromParent(
            SpanName.SecretKeyWrapping.name,
            SpanExtension.current().spanContext
        )

        return try {
            SpanExtension.makeCurrentSpan(span).use { _ ->
                span.setAttribute(AttributeName.secret_key_wrapping_operation.name, "UNWRAP")
                val cipherParamsSpec = getKeyPairCompatibleCipherSpecs(keyPair).firstOrNull { spec ->
                    spec.transformation.contains(wrappedSecretKey.cipherTransformation, ignoreCase = true)
                } ?: throw ClientException(
                        ClientException.UNKNOWN_CRYPTO_ERROR,
                        "No compatible cipher specs found for key pair: $keyPair"
                )
                
                span.setAttribute(
                    AttributeName.secret_key_wrapping_transformation.name,
                    cipherParamsSpec.transformation
                )
                Logger.info(methodTag, "Unwrapping secret key with cipher spec: $cipherParamsSpec")
                val key = AndroidKeyStoreUtil.unwrap(
                    wrappedSecretKey.wrappedKeyData,
                    wrappedSecretKey.algorithm,
                    keyPair,
                    cipherParamsSpec.transformation,
                    cipherParamsSpec.algorithmParameterSpec
                )
                span.setStatus(StatusCode.OK)
                key
            }
        } catch (exception: Exception) {
            Logger.error(methodTag, "Failed to wrap secret key", exception)
            span.setStatus(StatusCode.ERROR)
            span.recordException(exception)
            throw exception
        } finally {
            span.end()
        }
    }

    /**
     * Get all compatible cipher specifications for the given key pair in priority order.
     *
     * Matches key pair's supported encryption paddings with available cipher specs,
     * returning all compatible specs prioritized by security (most secure first).
     * Returns an empty list if no compatible specs are found.
     *
     * @param keyPair The key pair to find compatible cipher specs for
     * @return List of compatible [CipherSpec] ordered by priority (most secure first)
     */
    @Throws(ClientException::class)
    private fun getKeyPairCompatibleCipherSpecs(keyPair: KeyPair): List<CipherSpec> {
        val methodTag = "$TAG:selectCompatibleCipherSpecs"
        val supportedPaddings = AndroidKeyStoreUtil.getKeyPairEncryptionPaddings(keyPair)
        val availableCipherSpecs = cryptoParameterSpecFactory.getPrioritizedCipherParameterSpecs()
        Logger.verbose(
            methodTag,
            "Supported paddings by the keyPair: $supportedPaddings" +
                    ",Specs available in order of priority: $availableCipherSpecs"
        )
        val compatibleSpecs = availableCipherSpecs.filter { spec ->
            supportedPaddings.any { padding ->
                spec.padding.contains(padding, ignoreCase = true)
            }
        }
        Logger.verbose(methodTag, "Found ${compatibleSpecs.size} compatible cipher specs: $compatibleSpecs")
        return compatibleSpecs
    }

    /**
     * Generates a new RSA key pair using prioritized specifications with fallback support.
     *
     * Attempts key generation with multiple specs in order of preference (modern to legacy).
     * Includes comprehensive error handling and telemetry tracking.
     *
     * @return Generated [KeyPair] from Android KeyStore
     * @throws ClientException if all key generation attempts fail
     */
    @Throws(ClientException::class)
    private fun generateKeyPair(): KeyPair {
        val methodTag = "$TAG:generateKeyPair"
        val span = OTelUtility.createSpanFromParent(
            SpanName.KeyPairGeneration.name,
            SpanExtension.current().spanContext
        )
        val failures = mutableListOf<Throwable>()
        return try {
            SpanExtension.makeCurrentSpan(span).use { _ ->
                val specs = cryptoParameterSpecFactory.getPrioritizedKeyGenParameterSpecs()
                validateSpecsAvailable(specs)
                for ((index, spec) in specs.withIndex()) {
                    Logger.verbose(
                        methodTag,
                        "Attempting key generation with spec ${index + 1}: $spec"
                    )
                    attemptKeyGeneration(spec)
                        .onSuccess { keyPair ->
                            Logger.info(
                                methodTag,
                                "Key pair generated successfully with spec: $spec"
                            )
                            span.setAttribute(
                                AttributeName.key_pair_gen_description.name,
                                spec.description
                            )
                            span.setAttribute(
                                AttributeName.key_pair_gen_algorithm.name,
                                spec.algorithm
                            )
                            span.setAttribute(
                                AttributeName.key_pair_gen_encryptionPaddings.name,
                                spec.encryptionPaddings.toString()
                            )
                            span.setStatus(StatusCode.OK)
                            return@use keyPair
                        }
                        .onFailure { throwable ->
                            Logger.warn(
                                methodTag,
                                "Failed to generate key pair with spec: $spec, error: ${throwable.message}"
                            )
                            failures.add(throwable)
                        }
                }
                handleAllFailures(failures)
            }
        } finally {
            span.end()
        }
    }

    /**
     * Validates that key generation specifications are available for use.
     *
     * Ensures at least one specification exists before attempting key generation.
     * Records telemetry and throws exception if no specs are available.
     *
     * @param specs List of key generation specifications to validate
     * @throws ClientException if specs list is empty
     */
    @Throws(ClientException::class)
    private fun validateSpecsAvailable(specs: List<IKeyGenSpec>) {
        if (specs.isEmpty()) {
            val error = ClientException(
                ClientException.UNKNOWN_CRYPTO_ERROR,
                "No key generation specifications available for generating key pair."
            )
            SpanExtension.current().setStatus(StatusCode.ERROR)
            SpanExtension.current().recordException(error)
            throw ExceptionAdapter.clientExceptionFromException(error)
        }
    }

    /**
     * Attempts key pair generation with a single specification and measures performance.
     *
     * Wraps key generation in Result for safe exception handling and tracks
     * generation time for telemetry purposes.
     *
     * @param spec The key generation specification to attempt
     * @return [Result] containing generated KeyPair or captured exception
     */
    private fun attemptKeyGeneration(spec: IKeyGenSpec): Result<KeyPair> {
        return runCatching {
            val startTime = System.currentTimeMillis()
            val keyPair = AndroidKeyStoreUtil.generateKeyPair(
                spec.algorithm,
                spec.algorithmParameterSpec
            )
            val elapsedTime = System.currentTimeMillis() - startTime
            SpanExtension.current()
                .setAttribute(AttributeName.elapsed_time_keypair_generation.name, elapsedTime)
            keyPair
        }
    }

    /**
     * Handles all key generation failures and throws a ClientException.
     *
     * Logs each failure, records telemetry data, and throws an exception based on the last failure.
     *
     * @param failures List of exceptions encountered during key generation attempts
     * @throws ClientException Always throws after processing all failures
     */
    private fun handleAllFailures(failures: List<Throwable>): Nothing {
        val methodTag = "$TAG:handleAllFailures"
        require(failures.isNotEmpty()) {
            "No failures encountered, but no key pair generated. This should not happen."
        }
        val errorMessages = failures.joinToString(separator = "; ") { exception ->
            "${exception.javaClass.simpleName}: ${exception.message ?: "Unknown error"}"
        }

        failures.forEach { exception ->
            Logger.error(
                methodTag,
                "Key pair generation failed with: ${exception.message}",
                exception
            )
        }
        SpanExtension.current()
            .setAttribute(AttributeName.keypair_gen_exception.name, errorMessages)

        val finalError = failures.last()
        SpanExtension.current().setStatus(StatusCode.ERROR)
        SpanExtension.current().recordException(finalError)
        throw ExceptionAdapter.clientExceptionFromException(finalError)
    }

    /**
     * Loads a wrapped secret key from file, automatically detecting the storage format.
     *
     * @return WrappedSecretKey instance or null if file doesn't exist or is empty
     */
    private fun loadSecretKeyFromFile(): WrappedSecretKey? {
        val methodTag = "$TAG:loadFromFile"
        if (!keyFile.exists()) {
            Logger.warn(methodTag, "Key file does not exist")
            return null
        }
        val wrappedSecretKeyData = FileUtil.readFromFile(keyFile, KEY_FILE_SIZE)
        if (wrappedSecretKeyData == null || wrappedSecretKeyData.isEmpty()) {
            Logger.warn(methodTag, "Key file is empty")
            return null
        }
        return WrappedSecretKey.deserialize(wrappedSecretKeyData)
    }
}
