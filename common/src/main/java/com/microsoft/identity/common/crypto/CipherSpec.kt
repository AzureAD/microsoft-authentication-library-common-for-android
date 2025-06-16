package com.microsoft.identity.common.crypto

import java.security.spec.AlgorithmParameterSpec

data class CipherSpec(
    val algorithmParameterSpecs: AlgorithmParameterSpec?,
    val transformation: String
)
