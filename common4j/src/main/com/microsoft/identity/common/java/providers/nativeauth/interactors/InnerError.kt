package com.microsoft.identity.common.java.providers.nativeauth.interactors

import com.google.gson.annotations.SerializedName

data class InnerError(
    @SerializedName("inner_error") val innerError: String?,
    @SerializedName("error_description") val errorDescription: String?
)
