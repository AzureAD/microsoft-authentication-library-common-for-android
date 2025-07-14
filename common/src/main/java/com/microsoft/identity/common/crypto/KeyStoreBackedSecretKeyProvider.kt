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
class NewAndroidWrappedKeyProvider(
    override val alias: String,
    private val mFilePath: String,
    private val mContext: Context
) : ISecretKeyProvider {
    override val keyTypeIdentifier = KEY_TYPE_IDENTIFIER
    override val cipherTransformation = AES_CBC_PKCS5_PADDING_TRANSFORMATION
    private val cryptoParameterSpecFactory: CryptoParameterSpecFactory =
        CryptoParameterSpecFactory(mContext, alias)
    @get:VisibleForTesting
    val keyFromCache: SecretKey?
        get() {
            clearCachedKeyIfCantLoadOrFileDoesNotExist()
            return sKeyCacheMap[mFilePath]
        }


    @VisibleForTesting
    fun clearKeyFromCache() {
        sKeyCacheMap.remove(mFilePath)
    }

    private fun clearCachedKeyIfCantLoadOrFileDoesNotExist() {
        val shouldClearCache = !sSkipKeyInvalidationCheck &&
                (!AndroidKeyStoreUtil.canLoadKey(alias) || !keyFile.exists())
        if (shouldClearCache) {
            sKeyCacheMap.remove(mFilePath)
        }
    }


    @get:Throws(ClientException::class)
    @get:Synchronized
    override val key: SecretKey
        /**
         * If key is already generated, that one will be returned.
         * Otherwise, generate a new one and return.
         */
        get() {
            val methodTag = "$TAG:getKey"

            keyFromCache?.let {
                Logger.info(
                    methodTag,
                    "Key is already cached, returning cached key with thumbprint: " +
                            KeyUtil.getKeyThumbPrint(it)
                )
                return it
            }

            readSecretKeyFromStorage()?.let {
                sKeyCacheMap[mFilePath] = it
                Logger.info(
                    methodTag,
                    "Key loaded from storage and cached with thumbprint: " +
                            KeyUtil.getKeyThumbPrint(it)
                )
                return it
            }

            val newKey = generateNewSecretKey()
            sKeyCacheMap[mFilePath] = newKey
            Logger.info(
                methodTag,
                "New key is generated and cached with thumbprint: " +
                        KeyUtil.getKeyThumbPrint(newKey)
            )
            return newKey
        }

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
        val methodTag = "$TAG:generateRandomKey"
        val newSecretKey = AES256SecretKeyGenerator.generateRandomKey()
        val keyPair : KeyPair = AndroidKeyStoreUtil.readKey(alias)
            ?: run {
                Logger.info(methodTag, "No existing keypair found. Generating a new one.")
                generateKeyPair()
                //generateNewKeyPair()
        }
        val cipherParamsSpec = selectCompatibleCipherSpec(keyPair)
        Log.i(
            methodTag,
            "Selected cipher spec for key wrapping: ${cipherParamsSpec.transformation}"+
                    "\n cipherParamsSpec = ${cipherParamsSpec.algorithmParameterSpec}"
        )
        val keyWrapped = AndroidKeyStoreUtil.wrap(
            newSecretKey,
            keyPair,
            cipherParamsSpec.transformation,
            cipherParamsSpec.algorithmParameterSpec
        )
        FileUtil.writeDataToFile(keyWrapped, keyFile)
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
            Log.i(
                methodTag,
                "Selected cipher spec for key unwrapping: ${cipherParamsSpec.transformation}"+
                "\n cipherParamsSpec = ${cipherParamsSpec.algorithmParameterSpec}"
            )
            val key = AndroidKeyStoreUtil.unwrap(
                wrappedSecretKey,
                AES256SecretKeyGenerator.AES_ALGORITHM,
                keyPair,
                cipherParamsSpec.transformation,
                cipherParamsSpec.algorithmParameterSpec
            )

            Logger.info(
                methodTag, "Key is loaded with thumbprint: " +
                        KeyUtil.getKeyThumbPrint(key)
            )

            return key
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












    /**
     * Wipe all the data associated from this key.
     */
    // VisibleForTesting
    @Throws(ClientException::class)
    fun deleteSecretKeyFromStorage() {
        AndroidKeyStoreUtil.deleteKey(alias)
        FileUtil.deleteFile(keyFile)
        sKeyCacheMap.remove(mFilePath)
    }



    private fun selectCompatibleCipherSpec(keyPair: KeyPair): CipherSpec {
        val methodTag = "$TAG:selectCompatibleCipherSpec"
        val supportedPaddings = AndroidKeyStoreUtil.getEncryptionPaddings(keyPair)
        val availableCipherSpecs = cryptoParameterSpecFactory.getPrioritizedCipherParameterSpecs()
        Logger.verbose(
            methodTag,
            "Supported paddings by the keyPair: $supportedPaddings" +
                    ",Specs available in order of priority: $availableCipherSpecs"
        )
        for (cipherSpec in availableCipherSpecs) {
            for (padding in supportedPaddings) {
                if (cipherSpec.padding.contains(padding, ignoreCase = true)) {
                    return cipherSpec
                }
            }
        }
        Logger.warn(methodTag, "No supported cipher specification found for wrapping the key.")
        // Fallback to PKCS#1 padding if no compatible spec is found, instead of throwing an error.
        return cryptoParameterSpecFactory.pkcs1CipherSpec
    }


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
                        Log.i(methodTag, "Key pair generated successfully with spec: $spec ")
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


    private val keyFile: File
        /**
         * Get the file that stores the wrapped key.
         */
        get() = File(
            mContext.getDir(mContext.packageName, Context.MODE_PRIVATE),
            mFilePath
        )

    companion object {
        /**
         * AES is 16 bytes (128 bits), thus PKCS#5 padding should not work, but in
         * Java AES/CBC/PKCS5Padding is default(!) algorithm name, thus PKCS5 here
         * probably doing PKCS7. We decide to go with Java default string.
         */
        const val AES_CBC_PKCS5_PADDING_TRANSFORMATION: String = "AES/CBC/PKCS5Padding"


        private val TAG = NewAndroidWrappedKeyProvider::class.java.simpleName + "#"

        /**
         * Should KeyStore and key file check for validity before every key load be skipped.
         */
        @SuppressFBWarnings("MS_SHOULD_BE_FINAL")
        var sSkipKeyInvalidationCheck: Boolean = false


        /**
         * Indicate that token item is encrypted with the key loaded in this class.
         */
        const val KEY_TYPE_IDENTIFIER: String = "A001"


        // Exposed for testing only.
        /* package */
        const val KEY_FILE_SIZE: Int = 1024

        /**
         * SecretKey cache. Maps wrapped secret key file path to the SecretKey.
         */
        private val sKeyCacheMap: ConcurrentMap<String, SecretKey> = ConcurrentHashMap()
    }
}