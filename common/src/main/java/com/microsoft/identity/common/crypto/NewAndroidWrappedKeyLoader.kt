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
import com.microsoft.identity.common.crypto.AndroidWrappedKeyLoaderFactory.WRAPPED_KEY_KEY_IDENTIFIER
import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil
import com.microsoft.identity.common.java.crypto.key.AES256KeyLoader
import com.microsoft.identity.common.java.crypto.key.KeyUtil
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.util.CachedData
import com.microsoft.identity.common.java.util.FileUtil
import com.microsoft.identity.common.logging.Logger
import java.io.File
import javax.crypto.SecretKey

/**
 * This class doesn't really use the KeyStore-generated key directly.
 *
 *
 * Instead, the actual key that we use to encrypt/decrypt data is 'wrapped/encrypted' with the keystore key
 * before it get saved to the file.
 */
class NewAndroidWrappedKeyLoader @JvmOverloads constructor(
    override val alias: String,
    private val mFilePath: String,
    private val mContext: Context,
    private val mKekManager: IKekManager = AndroidKeyStoreRsaKekManager(alias, mContext)
) : AES256KeyLoader() {


    // Exposed for testing only.
    val keyCache: CachedData<SecretKey?> = object : CachedData<SecretKey?>() {
        override fun getData(): SecretKey? {
            if (AndroidWrappedKeyLoaderFactory.skipKeyInvalidationCheck) {
                return super.getData()
            }
            if ((!AndroidKeyStoreUtil.canLoadKey(alias) || !keyFile.exists())) {
                this.clear()
            }
            return super.getData()
        }
    }



    /**
     * If key is already generated, that one will be returned.
     * Otherwise, generate a new one and return.
     */
    @get:Throws(ClientException::class)
    @get:Synchronized
    override val key: SecretKey
        get() {
            val methodTag = "$TAG:key"
            keyCache.data?.let { keyOnCache ->
                Logger.info(
                    methodTag, "Key is loaded from cache with thumbprint: " +
                            KeyUtil.getKeyThumbPrint(keyOnCache)
                )
                return keyOnCache
            }
            readSecretKeyFromStorage()?.let { keyFromStorage ->
                Logger.info(
                    methodTag, "Key is loaded from storage with thumbprint: " +
                            KeyUtil.getKeyThumbPrint(keyFromStorage)
                )
                keyCache.data = keyFromStorage
                return keyFromStorage
            }
            secretKeyGenerator.generateRandomKey().let { newKey ->
                Logger.info(
                    methodTag, "New key is generated with thumbprint: " +
                            KeyUtil.getKeyThumbPrint(newKey)
                )
                saveSecretKeyToStorage(newKey, newKey.algorithm)
                keyCache.data = newKey
                return newKey
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
        try {
            if (!mKekManager.kekExists()) {
                Logger.info(methodTag, "key does not exist in keystore")
                deleteSecretKeyFromStorage()
                return null
            }
            val wrappedSecretKey = FileUtil.readFromFile(keyFile, KEY_FILE_SIZE) ?: run {
                Logger.warn(methodTag, "Key file is empty")
                // Do not delete the KeyStoreKeyPair even if the key file is empty. This caused credential cache
                // to be deleted in Office because of sharedUserId allowing keystore to be shared amongst apps.
                FileUtil.deleteFile(keyFile)
                FileUtil.deleteFile(keyAlgorithmFile)
                keyCache.clear()
                return null
            }
            val keyAlgorithm = FileUtil.readStringFromFile(keyAlgorithmFile) ?: run {
                Logger.warn(
                    methodTag, "Key algorithm file is empty, " +
                            "using SecretKeyGenerator to get the key algorithm"
                )
                secretKeyGenerator.keyAlgorithm
            }
            val key = mKekManager.unwrapKey(wrappedSecretKey, keyAlgorithm)
            Logger.info(
                methodTag,
                "Key is loaded with thumbprint: " + KeyUtil.getKeyThumbPrint(key)
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
     * Encrypt the given unencrypted symmetric key with Keystore key and save to storage.
     */
    @Throws(ClientException::class)
    private fun saveSecretKeyToStorage(
        unencryptedKey: SecretKey,
        keyAlgorithm: String
    ) {
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
        val keyWrapped = mKekManager.wrapKey(unencryptedKey)
        FileUtil.writeDataToFile(keyWrapped, keyFile)
        FileUtil.writeStringToFile(keyAlgorithm, keyAlgorithmFile)
    }

    /**
     * Wipe all the data associated from this key.
     */
    // VisibleForTesting
    @Throws(ClientException::class)
    fun deleteSecretKeyFromStorage() {
        AndroidKeyStoreUtil.deleteKey(alias)
        FileUtil.deleteFile(keyFile)
        FileUtil.deleteFile(keyAlgorithmFile)
        keyCache.clear()
    }

    private val keyFile: File
        get() = File(
            mContext.getDir(mContext.packageName, Context.MODE_PRIVATE),
            mFilePath
        )

    private val keyAlgorithmFile: File
        get() = File(
            mContext.getDir(mContext.packageName, Context.MODE_PRIVATE),
            SECRET_KEY_ALGORITHM_FILE
        )

    override val cipherTransformation: String
        get() = mKekManager.cipherTransformation

    override val keyTypeIdentifier: String
        get() = WRAPPED_KEY_KEY_IDENTIFIER

    companion object {
        private val TAG = NewAndroidWrappedKeyLoader::class.java.simpleName

        // Exposed for testing only.
        const val KEY_FILE_SIZE: Int = 1024

        // Exposed for testing only.
        const val SECRET_KEY_ALGORITHM_FILE: String = "key_algorithm_file"
    }
}

