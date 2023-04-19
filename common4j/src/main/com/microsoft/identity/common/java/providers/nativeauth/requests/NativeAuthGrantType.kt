package com.microsoft.identity.common.java.providers.nativeauth.requests

enum class NativeAuthGrantType(val jsonValue: String) {
    PASSWORD("password"),
    PASSWORDLESS_OTP("oob"),
    ATTRIBUTES("attributes")
}
