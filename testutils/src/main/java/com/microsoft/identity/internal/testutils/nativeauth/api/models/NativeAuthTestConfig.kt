package com.microsoft.identity.internal.testutils.nativeauth.api.models


data class NativeAuthTestConfig(
    val configs: Map<String, Config>
) {
    data class Config(
        val email: String,
        val client_id: String,
        val authority_url: String
    )
}
