package com.microsoft.identity.internal.testutils.nativeauth

enum class ConfigType(val stringValue: String) {
    SSPR("SSPR"),
    SIGN_UP_PASSWORD("SIGN_UP_PASSWORD"),
    SIGN_IN_PASSWORD("SIGN_IN_PASSWORD"),
    ACCESS_TOKEN("ACCESS_TOKEN")
}