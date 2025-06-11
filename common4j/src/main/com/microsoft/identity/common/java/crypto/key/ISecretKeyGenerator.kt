package com.microsoft.identity.common.java.crypto.key

import javax.crypto.SecretKey

interface ISecretKeyGenerator {
    val keySize: Int
    val keyAlgorithm: String
    fun generateRandomKey(): SecretKey
    fun generateKeyFromRawBytes(rawBytes: ByteArray): SecretKey
}