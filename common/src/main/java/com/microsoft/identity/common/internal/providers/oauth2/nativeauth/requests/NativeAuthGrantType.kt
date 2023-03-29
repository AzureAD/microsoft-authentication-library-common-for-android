package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests

enum class NativeAuthGrantType(val jsonValue: String) {
    PASSWORD("password"),
    PASSWORDLESS_OTP("passwordless_otp")
}
