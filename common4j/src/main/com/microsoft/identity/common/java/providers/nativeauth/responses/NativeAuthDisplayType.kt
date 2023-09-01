package com.microsoft.identity.common.java.providers.nativeauth.responses

import com.google.gson.annotations.SerializedName

enum class NativeAuthDisplayType {
    @SerializedName("email")
    EMAIL,

    UNKNOWN
}
