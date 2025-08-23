package com.microsoft.identity.common.crypto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class SecretKey @OptIn(ExperimentalSerializationApi::class) constructor(
    @ProtoNumber(1)
    val algorithm: String,
    @ProtoNumber(2)
    val transformation: String,
    @ProtoNumber(number = 3)
    val keySize: Int
)
