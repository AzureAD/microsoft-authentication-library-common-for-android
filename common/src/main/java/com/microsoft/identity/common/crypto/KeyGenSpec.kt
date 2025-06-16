package com.microsoft.identity.common.crypto

import java.security.spec.AlgorithmParameterSpec

data class KeyGenSpec(
    val keyGenParameterSpec: AlgorithmParameterSpec,
    val description: String
)