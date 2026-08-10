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
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import java.io.File
import java.security.KeyPair
import java.util.Collections
import java.util.IdentityHashMap
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

        /** [AttributeName.secret_key_wipe_reason]: wrapped-key file present but its keystore key is gone. */
        private const val WIPE_REASON_KEYSTORE_KEY_ABSENT_ORPHANED_FILE: String =
            "keystore_key_absent_orphaned_file"

        /** [AttributeName.secret_key_wipe_reason]: unrecoverable error while reading existing key material. */
        private const val WIPE_REASON_LOAD_ERROR: String = "load_error"

        /** [AttributeName.secret_key_wipe_reason]: wrapped-key file exists but is empty (silent re-key). */
        private const val WIPE_REASON_EMPTY_KEY_FILE_REKEY: String = "empty_key_file_rekey"

        /**
         * SecretKey cache. Maps wrapped secret key file path to the SecretKey.
         */
        private val sKeyCacheMap: ConcurrentMap<String, SecretKey> = ConcurrentHashMap()

        /**
         * Walks [throwable]'s cause chain down to its root cause and returns it. Identity tracking
         * guarantees termination even for self-referential or multi-node cause cycles, so no depth
         * cap is needed.
         */
        @VisibleForTesting
        fun findRootCause(throwable: Throwable): Throwable {
            val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
            var current = throwable
            seen.add(current)
            var next = current.cause
            while (next != null && seen.add(next)) {
                current = next
                next = current.cause
            }
            return current
        }
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
        val span = OTelUtility.createSpanFromParent(
            SpanName.SecretKeyGeneration.name,
            SpanExtension.current().spanContext
        )
        try {
            SpanExtension.makeCurrentSpan(span).use { _ ->
                val newSecretKey = AES256SecretKeyGenerator.generateRandomKey()
                val keyPair = AndroidKeyStoreUtil.readKey(alias) ?: run {
                    Logger.info(methodTag, "No existing keypair found. Generating a new one.")
                    generateKeyPair()
                }
                val wrappedSecretKey = wrapSecretKey(newSecretKey, keyPair)
                recordSecretKey(wrappedSecretKey)
                FileUtil.writeDataToFile(wrappedSecretKey.serialize(), keyFile)
                span.setStatus(StatusCode.OK)
                return newSecretKey
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
     * Load the saved keystore-encrypted key. Will only do read operation.
     *
     * @return SecretKey. Null if there isn't any.
     */
    /* package */@Synchronized
    @Throws(ClientException::class)
    fun readSecretKeyFromStorage(): SecretKey? {
        val methodTag = "$TAG:readSecretKeyFromStorage"
        val span = OTelUtility.createSpanFromParent(
            SpanName.SecretKeyRetrieval.name,
            SpanExtension.current().spanContext
        )
        SpanExtension.makeCurrentSpan(span).use { _ ->
            try {
                // Load KeyPair from Android KeyStore
                val keyPair = AndroidKeyStoreUtil.readKey(alias) ?: run {
                    Logger.info(methodTag, "key does not exist in keystore")
                    // Orphaned wrapped-key file with no keystore key = discarding undecryptable data,
                    // so record it. No file = legitimate first-time read, not recorded.
                    if (keyFile.exists()) {
                        recordWipe(span, WIPE_REASON_KEYSTORE_KEY_ABSENT_ORPHANED_FILE)
                    }
                    deleteSecretKeyFromStorage()
                    span.setStatus(StatusCode.OK)
                    return null
                }
                // Load wrapped secret key from file
                val rawWrappedSecretKey = readRawWrappedSecretKeyFromFile() ?: run {
                    Logger.warn(methodTag, "Key file is empty")
                    // Do not delete the KeyStoreKeyPair even if the key file is empty. This caused credential cache
                    // to be deleted in Office because of sharedUserId allowing keystore to be shared amongst apps.
                    // An existing-but-empty file is a silent re-key of undecryptable data, so record it; an absent
                    // file is the legitimate first-time / sharedUserId read and is not recorded.
                    if (keyFile.exists()) {
                        recordWipe(span, WIPE_REASON_EMPTY_KEY_FILE_REKEY)
                    }
                    FileUtil.deleteFile(keyFile)
                    clearKeyFromCache()
                    span.setStatus(StatusCode.OK)
                    return null
                }
                // Deserialize and unwrap the secret key
                val secretKey = deserializeAndUnwrapSecretKey(rawWrappedSecretKey, keyPair)
                span.setStatus(StatusCode.OK)
                return secretKey
            } catch (e: ClientException) {
                // Reset KeyPair info so that new request will generate correct KeyPairs.
                // All tokens with previous SecretKey are not possible to decrypt.
                Logger.warn(
                    methodTag, "Error when loading key from Storage, " +
                            "wipe all existing key data "
                )
                recordWipe(span, WIPE_REASON_LOAD_ERROR, e)
                span.setStatus(StatusCode.ERROR)
                span.recordException(e)
                deleteSecretKeyFromStorage()
                throw e
            } catch (e: Exception) {
                // Non-ClientException failures are recorded but not wiped (only ClientException wipes).
                span.setStatus(StatusCode.ERROR)
                span.recordException(e)
                throw e
            } finally {
                span.end()
            }
        }
    }

    /**
     * Records telemetry for a genuine wipe / silent re-key of EXISTING secret-key material onto the
     * current [SpanName.SecretKeyRetrieval] span. Never called for legitimate first-time reads. Any
     * failure here is swallowed so telemetry can never break the read/wipe path.
     *
     * @param span the active SecretKeyRetrieval span.
     * @param reason a stable identifier for which check triggered the wipe.
     * @param exception the failure that triggered the wipe, when caused by an exception.
     */
    private fun recordWipe(span: Span, reason: String, exception: ClientException? = null) {
        try {
            span.setAttribute(AttributeName.secret_key_wipe_reason.name, reason)
            if (exception != null) {
                // Record the true root cause, not the generic ClientException wrapper.
                val rootCause = findRootCause(exception)
                span.setAttribute(
                    AttributeName.secret_key_read_root_cause.name,
                    "${rootCause.javaClass.simpleName}: ${rootCause.message ?: "null"}"
                )
                // Exact platform error code, when a KeyStoreException is present (API 33+).
                AndroidKeyStoreUtil.getKeyStoreExceptionNumericErrorCode(exception)?.let { numericCode ->
                    span.setAttribute(AttributeName.keystore_numeric_error_code.name, numericCode)
                }
                // Whether the failure is transient/permanent (API 33+) so a retryable error is
                // distinguishable from genuine data loss.
                span.setAttribute(
                    AttributeName.keystore_error_transience.name,
                    AndroidKeyStoreUtil.getKeyStoreErrorTransience(exception).name
                )
            }
        } catch (e: Exception) {
            Logger.warn("$TAG:recordWipe", "Failed to record wipe telemetry: ${e.message}")
        }
    }

    /**
     * Deserializes the raw wrapped secret key data and unwraps it using the provided KeyPair.
     *
     * Runs under the [SpanName.SecretKeyRetrieval] span created by [readSecretKeyFromStorage]. Telemetry
     * recorded here (and inside [WrappedSecretKey]) via [SpanExtension.current] therefore lands on that
     * span, and any failure propagates to [readSecretKeyFromStorage] where the wipe is recorded.
     *
     * @param rawWrappedSecretKey The raw byte array of the wrapped secret key.
     * @param keyPair The KeyPair used to unwrap the secret key.
     * @return The unwrapped SecretKey.
     * @throws ClientException if there is an error during deserialization or unwrapping.
     */
    private fun deserializeAndUnwrapSecretKey(
        rawWrappedSecretKey: ByteArray,
        keyPair: KeyPair
    ): SecretKey {
        val wrappedSecretKey = WrappedSecretKey.deserialize(rawWrappedSecretKey)
        recordSecretKey(wrappedSecretKey)
        return unwrapSecretKey(wrappedSecretKey, keyPair)
    }

    /**
     * Records telemetry attributes for secret key
     *
     * Captures the cipher transformation and algorithm used for the wrapped secret key
     * in the active span for observability and auditing purposes.
     *
     * @param wrappedSecretKey The deserialized wrapped secret key containing metadata
     */
    private fun recordSecretKey(wrappedSecretKey: WrappedSecretKey) {
        SpanExtension.current().apply {
            setAttribute(
                AttributeName.secret_key_transformation.name,
                wrappedSecretKey.cipherTransformation
            )
            setAttribute(
                AttributeName.secret_key_algorithm.name,
                wrappedSecretKey.algorithm
            )
        }
    }

    private fun wrapSecretKey(
        secretKey: SecretKey,
        keyPair: KeyPair
    ): WrappedSecretKey {
        val methodTag = "$TAG:wrapSecretKey"
        val cipherParamsSpec = getKeyPairCompatibleCipherSpecs(keyPair).firstOrNull()
            ?: throw ClientException(
                ClientException.UNKNOWN_CRYPTO_ERROR,
                "No compatible cipher specs found for key pair: $keyPair"
            )
        Logger.info(methodTag, "Wrapping secret key with cipher spec: $cipherParamsSpec")
        val wrappedKey = AndroidKeyStoreUtil.wrap(
            secretKey,
            keyPair,
            cipherParamsSpec.transformation,
            cipherParamsSpec.algorithmParameterSpec
        )
        return WrappedSecretKey(
            wrappedKeyData = wrappedKey,
            algorithm = secretKey.algorithm,
            cipherTransformation = cipherParamsSpec.transformation
        )

    }

    private fun unwrapSecretKey(
        wrappedSecretKey: WrappedSecretKey,
        keyPair: KeyPair
    ): SecretKey {
        val methodTag = "$TAG:unwrapSecretKey"
        val cipherParamsSpec = getKeyPairCompatibleCipherSpecs(keyPair).firstOrNull { spec ->
            spec.transformation.contains(wrappedSecretKey.cipherTransformation, ignoreCase = true)
        } ?: throw ClientException(
            ClientException.UNKNOWN_CRYPTO_ERROR,
            "No compatible cipher specs found for key pair: $keyPair"
        )
        Logger.info(methodTag, "Unwrapping secret key with cipher spec: $cipherParamsSpec")
        return AndroidKeyStoreUtil.unwrap(
            wrappedSecretKey.wrappedKeyData,
            wrappedSecretKey.algorithm,
            keyPair,
            cipherParamsSpec.transformation,
            cipherParamsSpec.algorithmParameterSpec
        )
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
        SpanExtension.current().apply {
            setAttribute(
                AttributeName.key_pair_supported_paddings.name,
                supportedPaddings.toString()
            )
            setAttribute(
                AttributeName.available_transformation_list.name,
                availableCipherSpecs.joinToString(separator = ",") { spec -> spec.transformation }
            )
        }

        val compatibleSpecs = availableCipherSpecs.filter { spec ->
            supportedPaddings.any { padding ->
                spec.padding.contains(padding, ignoreCase = true)
            }
        }
        if (compatibleSpecs.isNotEmpty()) {
            SpanExtension.current().setAttribute(
                AttributeName.elected_cipher_transformation.name,
                compatibleSpecs.first().transformation
            )
        }
        Logger.verbose(
            methodTag,
            "Found ${compatibleSpecs.size} compatible cipher specs: $compatibleSpecs"
        )
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
        val failures = mutableMapOf<IKeyGenSpec, Throwable>()
        val specs = cryptoParameterSpecFactory.getPrioritizedKeyGenParameterSpecs()
        if (specs.isEmpty()) {
            throw ClientException(
                ClientException.UNKNOWN_CRYPTO_ERROR,
                "No key generation specifications available for generating key pair."
            )
        }
        val span = OTelUtility.createSpanFromParent(
            SpanName.KeyPairGeneration.name,
            SpanExtension.current().spanContext
        )
        SpanExtension.makeCurrentSpan(span).use { _ ->
            try {
                for ((index, spec) in specs.withIndex()) {
                    Logger.verbose(
                        methodTag,
                        "Attempting key generation with spec ${index + 1}: $spec"
                    )
                    attemptKeyGeneration(spec)
                        .onSuccess { keyPair ->
                            recordKeyPairGenSuccess(spec, failures)
                            Logger.info(
                                methodTag,
                                "Key pair generated successfully with spec: $spec"
                            )
                            return keyPair
                        }
                        .onFailure { throwable ->
                            Logger.warn(
                                methodTag,
                                "Failed to generate key pair with spec: $spec, error: ${throwable.message}"
                            )
                            failures[spec] = throwable
                        }
                }
                handleAllFailures(failures)
            } catch (e: ClientException) {
                span.setStatus(StatusCode.ERROR)
                span.recordException(e)
                throw e
            } finally {
                span.end()
            }
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
            val startTime = System.nanoTime()
            val keyPair = AndroidKeyStoreUtil.generateKeyPair(
                spec.algorithm,
                spec.algorithmParameterSpec
            )
            val elapsedTime = System.nanoTime() - startTime
            SpanExtension.current().setAttribute(
                AttributeName.key_pair_gen_elapsed_time.name,
                elapsedTime
            )
            keyPair
        }
    }

    /**
     * Records telemetry attributes for successful key pair generation.
     *
     * Captures key generation specifications, encryption paddings, and failure history
     * in the active span for observability and debugging purposes.
     *
     * @param spec The successful key generation specification
     * @param failures Map of previously failed key generation attempts
     */
    private fun recordKeyPairGenSuccess(
        spec: IKeyGenSpec,
        failures: Map<IKeyGenSpec, Throwable>
    ) {
        SpanExtension.current().apply {
            setAttribute(
                AttributeName.key_pair_gen_description.name,
                spec.description
            )
            setAttribute(
                AttributeName.key_pair_gen_algorithm.name,
                spec.algorithm
            )
            setAttribute(
                AttributeName.key_pair_gen_encryption_paddings.name,
                spec.encryptionPaddings.toString()
            )
            setAttribute(
                AttributeName.key_pair_gen_failure_history.name,
                formatFailureHistory(failures)
            )
            setStatus(StatusCode.OK)
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
    private fun handleAllFailures(failures: Map<IKeyGenSpec, Throwable>): Nothing {
        val methodTag = "$TAG:handleAllFailures"
        require(failures.isNotEmpty()) {
            "No failures encountered, but no key pair generated. This should not happen."
        }
        val lastFailure = failures.values.last()
        val finalError = ClientException(
            ClientException.UNKNOWN_CRYPTO_ERROR,
            "All key generation attempts failed. Total failures: ${failures.size}",
            lastFailure
        )
        Logger.error(methodTag, finalError.message, finalError)
        throw finalError
    }

    /**
     * Formats key generation failures into a semicolon-delimited summary string for telemetry.
     *
     * @param failures Map of key generation specifications to their corresponding exceptions
     * @return Formatted failure history string (e.g., "spec1: error1;spec2: error2;")
     * if no failures, returns "None"
     */
    private fun formatFailureHistory(failures: Map<IKeyGenSpec, Throwable>): String {
        if (failures.isEmpty()) return "None"
        return failures.entries.joinToString(separator = ";") { (spec, exception) ->
            "$spec: ${exception.message}"
        }
    }

    /**
     * Reads the raw wrapped secret key data from the file system.
     *
     * Attempts to read the serialized wrapped secret key from the configured key file.
     * Returns null if the file does not exist or contains no data.
     *
     * @return Raw byte array containing the serialized wrapped secret key, or null if file is missing or empty
     */
    private fun readRawWrappedSecretKeyFromFile(): ByteArray? {
        val methodTag = "$TAG:readRawWrappedSecretKeyFromFile"
        if (!keyFile.exists()) {
            Logger.warn(methodTag, "Key file does not exist")
            return null
        }
        val wrappedSecretKeyData = FileUtil.readFromFile(keyFile, KEY_FILE_SIZE)
        if (wrappedSecretKeyData == null || wrappedSecretKeyData.isEmpty()) {
            Logger.warn(methodTag, "Key file is empty")
            return null
        }
        return wrappedSecretKeyData
    }
}
