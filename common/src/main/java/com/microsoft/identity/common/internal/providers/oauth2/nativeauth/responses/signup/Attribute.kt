package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup

import com.google.gson.annotations.SerializedName

data class Attribute(
    @SerializedName("name") val name: String?
)
