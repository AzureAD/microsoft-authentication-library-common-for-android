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
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil
import com.microsoft.identity.common.java.controllers.ExceptionAdapter
import com.microsoft.identity.common.java.crypto.key.AES256SecretKeyGenerator
import com.microsoft.identity.common.java.crypto.key.ISecretKeyProvider
import com.microsoft.identity.common.java.crypto.key.KeyUtil
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager.getFlightsProvider
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.java.opentelemetry.SpanName
import com.microsoft.identity.common.java.util.FileUtil
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.common.logging.Logger
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.opentelemetry.api.trace.StatusCode
import java.io.File
import java.math.BigInteger
import java.security.KeyPair
import java.security.spec.AlgorithmParameterSpec
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.crypto.SecretKey
import javax.security.auth.x500.X500Principal

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
     * Generate a new key pair wrapping key, based on API level uses different spec to generate
     * the key pair.
     * @return a key pair
     */
    @Throws(ClientException::class)
    private fun generateNewKeyPair(): KeyPair {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            generateNewKeyPairAPI28AndAbove()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            generateNewKeyPairAPI23AndAbove()
        } else {
            generateKeyPairWithLegacySpec()
        }
    }

    /**
     * Call this for API level >= 28. Starting level API 28 PURPOSE_WRAP_KEY is added. Based on flights
     * this method may or may not use the PURPOSE_WRAP_KEY along with PURPOSE_ENCRYPT and PURPOSE_DECRYPT. The logic
     * if (wrap key flight enabled) use all three purposes
     * else if (new key gen flight enabled) use only encrypt and decrypt purposes
     * else use legacy spec.
     * @return key pair
     */
    @RequiresApi(Build.VERSION_CODES.P)
    @Throws(
        ClientException::class
    )
    private fun generateNewKeyPairAPI28AndAbove(): KeyPair {
        return if (getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY)
        ) {
            generateWrappingKeyPair_WithPurposeWrapKey()
        } else if (getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY)
        ) {
            generateWrappingKeyPair()
        } else {
            generateKeyPairWithLegacySpec()
        }
    }

    /**
     * Call this for API level >= 23. Based on flight new key gen spec is used else legacy which
     * is deprecated starting API 23.
     * @return key pair
     */
    @RequiresApi(Build.VERSION_CODES.M)
    @Throws(
        ClientException::class
    )
    private fun generateNewKeyPairAPI23AndAbove(): KeyPair {
        return if (getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY)
        ) {
            generateWrappingKeyPair()
        } else {
            generateKeyPairWithLegacySpec()
        }
    }

    /**
     * Generate a new key pair wrapping key based on legacy logic. Call this for API < 23 or as fallback
     * until new key gen specs are stable.
     * @return key pair generated with legacy spec
     * @throws ClientException if there is an error generating the key pair.
     */
    @Throws(ClientException::class)
    private fun generateKeyPairWithLegacySpec(): KeyPair {
        val span = SpanExtension.current()
        try {
            val keyPairGenSpec =
                legacySpecForKeyStoreKey
            val keyPair = attemptKeyPairGeneration(keyPairGenSpec)
            span.setAttribute(
                AttributeName.key_pair_gen_successful_method.name,
                "legacy_key_gen_spec"
            )
            return keyPair
        } catch (e: Throwable) {
            Logger.error(
                TAG + ":generateKeyPairWithLegacySpec",
                "Error generating keypair with legacy spec.",
                e
            )
            throw ExceptionAdapter.clientExceptionFromException(e)
        }
    }

    /**
     * Generate a new key pair wrapping key, based on API level >= 28. This method uses new key gen spec
     * with PURPOSE_WRAP_KEY. If this fails, it will fallback to generateWrappingKeyPair() which does not use
     * PURPOSE_WRAP_KEY (still uses new key gen spec).
     */
    @RequiresApi(Build.VERSION_CODES.P)
    @Throws(
        ClientException::class
    )
    private fun generateWrappingKeyPair_WithPurposeWrapKey(): KeyPair {
        val methodTag = TAG + ":generateWrappingKeyPair_WithPurposeWrapKey"
        val span = SpanExtension.current()
        try {
            Logger.info(methodTag, "Generating new keypair with new spec with purpose_wrap_key")
            val purposes =
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_WRAP_KEY
            val keyPairGenSpec = getSpecForWrappingKey(purposes)
            val keyPair = attemptKeyPairGeneration(keyPairGenSpec)
            span.setAttribute(
                AttributeName.key_pair_gen_successful_method.name,
                "new_key_gen_spec_with_wrap"
            )
            return keyPair
        } catch (e: Throwable) {
            Logger.error(
                methodTag, "Error generating keypair with new spec with purpose_wrap_key." +
                        "Attempting without purpose_wrap_key.", e
            )
            if (!StringUtil.isNullOrEmpty(e.message)) {
                span.setAttribute(AttributeName.keypair_gen_exception.name, e.message)
            }
            return generateWrappingKeyPair()
        }
    }

    /**
     * Generate a new key pair wrapping key, based on API level >= 23. This method uses new key gen spec
     * with purposes PURPOSE_ENCRYPT and PURPOSE_DECRYPT. If this fails, it will fallback to generateKeyPairWithLegacySpec()
     * which uses olg key gen spec.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    @Throws(
        ClientException::class
    )
    private fun generateWrappingKeyPair(): KeyPair {
        val methodTag = TAG + ":generateWrappingKeyPair"
        val span = SpanExtension.current()
        try {
            Logger.info(methodTag, "Generating new keypair with new spec without wrap key")
            val purposes = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            val keyPairGenSpec = getSpecForWrappingKey(purposes)
            val keyPair = attemptKeyPairGeneration(keyPairGenSpec)
            span.setAttribute(
                AttributeName.key_pair_gen_successful_method.name,
                "new_key_gen_spec_without_wrap"
            )
            return keyPair
        } catch (e: Throwable) {
            Logger.error(
                methodTag, "Error generating keypair with new spec." +
                        "Attempting with legacy spec.", e
            )
            if (!StringUtil.isNullOrEmpty(e.message)) {
                span.setAttribute(AttributeName.keypair_gen_exception.name, e.message)
            }
            return generateKeyPairWithLegacySpec()
        }
    }

    @Throws(ClientException::class)
    private fun attemptKeyPairGeneration(keyPairGenSpec: AlgorithmParameterSpec): KeyPair {
        val keypairGenStartTime = System.currentTimeMillis()
        val keyPair = AndroidKeyStoreUtil.generateKeyPair(
            WRAP_KEY_ALGORITHM, keyPairGenSpec
        )
        recordKeyGenerationTime(keypairGenStartTime)
        return keyPair
    }

    private fun recordKeyGenerationTime(keypairGenStartTime: Long) {
        val elapsedTime = System.currentTimeMillis() - keypairGenStartTime
        SpanExtension.current()
            .setAttribute(AttributeName.elapsed_time_keypair_generation.name, elapsedTime)
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

    private val legacySpecForKeyStoreKey: AlgorithmParameterSpec
        /**
         * Generate a self-signed cert and derive an AlgorithmParameterSpec from that.
         * This is for the key to be generated in [KeyStore] via [KeyPairGenerator]
         * Note : This is now only for API level < 23 or as fallback.
         *
         * @return a [AlgorithmParameterSpec] for the keystore key (that we'll use to wrap the secret key).
         */
        get() {
            // Generate a self-signed cert.
            val certInfo = String.format(
                Locale.ROOT, "CN=%s, OU=%s",
                alias,
                mContext.packageName
            )

            val start = Calendar.getInstance()
            val end = Calendar.getInstance()
            val certValidYears = 100
            end.add(Calendar.YEAR, certValidYears)

            return KeyPairGeneratorSpec.Builder(mContext)
                .setAlias(alias)
                .setSubject(X500Principal(certInfo))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(start.time)
                .setEndDate(end.time)
                .build()
        }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun getSpecForWrappingKey(purposes: Int): AlgorithmParameterSpec {
        return KeyGenParameterSpec.Builder(alias, purposes)
            .setKeySize(2048)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .build()
    }

    private fun selectCompatibleCipherSpec(keyPair: KeyPair): CipherSpec {
        val methodTag = "$TAG:selectCompatibleCipherSpec"
        val supportedPaddings = AndroidKeyStoreUtil.getEncryptionPaddings(keyPair)
        val availableSpecs = cryptoParameterSpecFactory.getPrioritizedCipherParameterSpecs()
        Logger.verbose(
            methodTag,
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
         * Algorithm for key wrapping.
         */
        private const val WRAP_ALGORITHM = "RSA/ECB/PKCS1Padding"

        /**
         * Algorithm for the wrapping key itself.
         */
        private const val WRAP_KEY_ALGORITHM = "RSA"

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