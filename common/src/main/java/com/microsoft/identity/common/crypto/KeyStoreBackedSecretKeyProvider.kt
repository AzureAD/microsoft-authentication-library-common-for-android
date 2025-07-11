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
import androidx.annotation.VisibleForTesting
import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil
import com.microsoft.identity.common.java.controllers.ExceptionAdapter
import com.microsoft.identity.common.java.crypto.key.AES256SecretKeyGenerator.generateRandomKey
import com.microsoft.identity.common.java.crypto.key.ISecretKeyProvider
import com.microsoft.identity.common.java.crypto.key.KeyUtil
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.java.opentelemetry.SpanName
import com.microsoft.identity.common.java.util.FileUtil
import com.microsoft.identity.common.logging.Logger
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.opentelemetry.api.trace.StatusCode
import java.io.File
import java.security.KeyPair
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.crypto.SecretKey

/**
 * This class doesn't really use the KeyStore-generated key directly.
 *
 *
 * Instead, the actual key that we use to encrypt/decrypt data is 'wrapped/encrypted' with the keystore key
 * before it get saved to the file.
 */
class KeyStoreBackedSecretKeyProvider (
    override val alias: String,
    private val mFilePath: String,
    private val mContext: Context
) : ISecretKeyProvider {

    @get:VisibleForTesting
    val keyFromCache: SecretKey?
        get() = sKeyCacheMap[mFilePath]

    @VisibleForTesting
    fun clearKeyFromCache() {
        sKeyCacheMap.remove(mFilePath)
    }

    @VisibleForTesting
    @Throws(ClientException::class)
    fun deleteSecretKeyAndCleanup() {
        AndroidKeyStoreUtil.deleteKey(alias)
        FileUtil.deleteFile(keyFile)
        sKeyCacheMap.remove(mFilePath)
    }

    private val keyFile: File
        get() = File(
            mContext.getDir(mContext.packageName, Context.MODE_PRIVATE),
            mFilePath
        )


    private val cryptoParameterSpecFactory = CryptoParameterSpecFactory(mContext, alias)

    override val keyTypeIdentifier = WRAPPED_KEY_KEY_IDENTIFIER

    override val cipherTransformation = AES_CIPHER_TRANSFORMATION

    @get:Throws(ClientException::class)
    @get:Synchronized
    override val key: SecretKey
        get() {
            val methodTag = "$TAG:getKey"
            if (!sSkipKeyInvalidationCheck && (!AndroidKeyStoreUtil.canLoadKey(alias) || !keyFile.exists())) {
                clearKeyFromCache()
            }
            // If key is on cache, return it.
            keyFromCache?.let { keyFromCache ->
                Logger.info(
                    methodTag, "Key is loaded from cache with thumbprint: " +
                            KeyUtil.getKeyThumbPrint(keyFromCache)
                )
                return keyFromCache
            }
            // If key is in storage, load it.
            Logger.info(methodTag, "Key not in cache or cache is empty.")
            readSecretKeyFromStorage()?.let { keyFromStorage ->
                Logger.info(
                    methodTag, "Key is loaded from storage with thumbprint: " +
                            KeyUtil.getKeyThumbPrint(keyFromStorage) +", caching it."
                )
                sKeyCacheMap[mFilePath] = keyFromStorage
                return keyFromStorage
            }
            Logger.info(methodTag, "Key does not exist in storage.")
            val newKey = generateAndStoreSecretKey()
            sKeyCacheMap[mFilePath] = newKey
            return newKey
        }

    @Throws(ClientException::class)
    private fun generateAndStoreSecretKey(): SecretKey {
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
        val methodTag = "$TAG:generateAndStoreSecretKey"
        Logger.info(methodTag, "Generating a new SecretKey")
        val newSecretKey = generateRandomKey()

        var keyPair = AndroidKeyStoreUtil.readKey(alias)
        Logger.info(methodTag, "KeyPair is null: ${keyPair == null}")
        if (keyPair == null) {
            keyPair = generateKeyPair()
        }

        val cipherParamsSpec = selectCompatibleCipherSpec(keyPair)
        Logger.info(TAG, "Wrapping key with cipher: $cipherParamsSpec")
        val keyWrapped = AndroidKeyStoreUtil.wrap(
            newSecretKey,
            keyPair,
            cipherParamsSpec.transformation,
            cipherParamsSpec.algorithmParameterSpec
        )

        FileUtil.writeDataToFile(keyWrapped, keyFile)
        Logger.info(methodTag, "New key is generated with thumbprint: " + KeyUtil.getKeyThumbPrint(newSecretKey))
        return newSecretKey
    }





    /* package */@Synchronized
    @Throws(ClientException::class)
    fun readSecretKeyFromStorage(): SecretKey? {
        val methodTag = "$TAG:readSecretKeyFromStorage"
        try {
            val keyPair = AndroidKeyStoreUtil.readKey(alias)
            if (keyPair == null) {
                Logger.info(methodTag, "key does not exist in keystore")
                deleteSecretKeyAndCleanup()
                return null
            }

            val wrappedSecretKey = FileUtil.readFromFile(keyFile, KEY_FILE_SIZE)
            if (wrappedSecretKey == null) {
                Logger.warn(methodTag, "Key file is empty")
                // Do not delete the KeyStoreKeyPair even if the key file is empty. This caused credential cache
                // to be deleted in Office because of sharedUserId allowing keystore to be shared amongst apps.
                FileUtil.deleteFile(keyFile)
                clearKeyFromCache()
                return null
            }

            val cipherParamsSpec = selectCompatibleCipherSpec(keyPair)
            val secretKey =  AndroidKeyStoreUtil.unwrap(
                wrappedSecretKey,
                WRAP_KEY_ALGORITHM,
                keyPair,
                cipherParamsSpec.transformation,
                cipherParamsSpec.algorithmParameterSpec
            )
            Logger.info(methodTag, "secretkey algorithm: ${secretKey.algorithm}, " +
                    "key size: ${secretKey.encoded.size}, " +
                    "key thumbprint: ${KeyUtil.getKeyThumbPrint(secretKey)}"
            )
            return secretKey
        } catch (e: ClientException) {
            // Reset KeyPair info so that new request will generate correct KeyPairs.
            // All tokens with previous SecretKey are not possible to decrypt.
            Logger.warn(
                methodTag, "Error when loading key from Storage, " +
                        "wipe all existing key data "
            )
            deleteSecretKeyAndCleanup()
            throw e
        }
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
        Logger.info(TAG,
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
        val methodTag = "${TAG}:generateKeyPair"
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

    companion object {
        private val TAG = KeyStoreBackedSecretKeyProvider::class.java.simpleName

        /**
         * AES is 16 bytes (128 bits), thus PKCS#5 padding should not work, but in
         * Java AES/CBC/PKCS5Padding is default(!) algorithm name, thus PKCS5 here
         * probably doing PKCS7. We decide to go with Java default string.
         */
        const val AES_CIPHER_TRANSFORMATION: String = "AES/CBC/PKCS5Padding"

        /**
         * Should KeyStore and key file check for validity before every key load be skipped.
         */
        @SuppressFBWarnings("MS_SHOULD_BE_FINAL")
        var sSkipKeyInvalidationCheck: Boolean = false

        /**
         * Algorithm for the wrapping key itself.
         */
        private const val WRAP_KEY_ALGORITHM = "RSA"

        /**
         * Indicate that token item is encrypted with the key loaded in this class.
         */
        const val WRAPPED_KEY_KEY_IDENTIFIER : String = "A001"

        @VisibleForTesting
        const val KEY_FILE_SIZE: Int = 1024

        /**
         * SecretKey cache. Maps wrapped secret key file path to the SecretKey.
         */
        private val sKeyCacheMap: ConcurrentMap<String, SecretKey> = ConcurrentHashMap()
    }
}
