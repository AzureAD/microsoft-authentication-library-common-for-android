package com.microsoft.identity.common.java.providers.nativeauth.responses.signup

import com.google.gson.annotations.SerializedName

data class Attribute(
    @SerializedName("name") val name: String?
)
