package com.microsoft.identity.common.java.crypto.key

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.logging.Logger
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class AES256SecretKeyGenerator : ISecretKeyGenerator {

    companion object {
        private val TAG = AES256SecretKeyGenerator::class.java.simpleName
    }

    override val keySize: Int
        get() = 256

    override val keyAlgorithm: String
        get() = "AES"

    @Throws (ClientException::class)
    override fun generateRandomKey(): SecretKey {
        val methodTag = "$TAG:generateRandomKey"
        try {
            val keygen = KeyGenerator.getInstance(keyAlgorithm)
            keygen.init(keySize, SecureRandom())
            return keygen.generateKey()
        } catch (e: NoSuchAlgorithmException) {
            val clientException = ClientException(
                ClientException.NO_SUCH_ALGORITHM,
                e.message,
                e
            )
            Logger.error(methodTag, clientException.errorCode, e)
            throw clientException
        }
    }

    override fun generateKeyFromRawBytes(rawBytes: ByteArray): SecretKey {
        return SecretKeySpec(rawBytes, keyAlgorithm)
    }
}