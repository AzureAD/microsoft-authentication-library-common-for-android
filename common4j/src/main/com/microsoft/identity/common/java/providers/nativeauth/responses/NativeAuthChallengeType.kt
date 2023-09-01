package com.microsoft.identity.common.java.providers.nativeauth.responses

import com.google.gson.annotations.SerializedName

enum class NativeAuthChallengeType {
    @SerializedName("oob")
    OOB,

    @SerializedName("redirect")
    REDIRECT,

    UNKNOWN
}
