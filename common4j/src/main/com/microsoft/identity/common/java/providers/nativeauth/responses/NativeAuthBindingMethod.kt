package com.microsoft.identity.common.java.providers.nativeauth.responses

import com.google.gson.annotations.SerializedName

enum class NativeAuthBindingMethod {
    @SerializedName("prompt")
    PROMPT,

    UNKNOWN
}
