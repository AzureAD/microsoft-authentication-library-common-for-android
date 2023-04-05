package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses

import com.google.gson.annotations.SerializedName

enum class NativeAuthDisplayType {
    @SerializedName("email")
    EMAIL,

    UNKNOWN
}
