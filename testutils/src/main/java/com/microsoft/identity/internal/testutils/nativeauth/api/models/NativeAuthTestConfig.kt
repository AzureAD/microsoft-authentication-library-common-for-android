package com.microsoft.identity.internal.testutils.nativeauth.api.models


data class NativeAuthTestConfig(
    val SIGN_IN_PASSWORD: Config,
    val SIGN_UP_PASSWORD: Config,
    val SSPR: Config) {
    data class Config(
        val email: String,
        val client_id: String,
        val authority_url: String
    )
}
