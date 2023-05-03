package com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.pollcompletion

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.nativeauth.IApiSuccessResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.NativeAuthPollCompletionStatus
import com.microsoft.identity.common.java.providers.oauth2.ISuccessResponse

class SsprPollCompletionResponse(
    @SerializedName("status")val status: NativeAuthPollCompletionStatus?
) : ISuccessResponse, IApiSuccessResponse {
    @Transient
    private var extraParameters: Iterable<Map.Entry<String, String>>? = null

    override fun getExtraParameters(): Iterable<Map.Entry<String, String>>? {
        return extraParameters
    }

    override fun setExtraParameters(params: Iterable<Map.Entry<String, String>>) {
        this.extraParameters = params
    }

    override fun validateRequiredFields() {
        if (status == null || status == NativeAuthPollCompletionStatus.UNKNOWN) {
            throw ClientException("SsprPollCompletionResponse.status can't be null or empty in success state.")
        }
        if (!checkStatusValidity(status)) {
            throw ClientException("SsprPollCompletionResponse.status=$status is invalid status in success state.")
        }
    }

    override fun validateOptionalFields() {}

    private fun checkStatusValidity(status: NativeAuthPollCompletionStatus?): Boolean {
        return NativeAuthPollCompletionStatus.values().contains(status)
    }
}
