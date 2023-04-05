package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses

import com.google.gson.annotations.SerializedName

enum class NativeAuthPollCompletionStatus {
    @SerializedName("succeeded")
    SUCCEEDED,

    @SerializedName("in_progress")
    IN_PROGRESS,

    @SerializedName("failed")
    FAILED,

    UNKNOWN
}
