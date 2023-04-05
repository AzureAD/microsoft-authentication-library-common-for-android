package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.sspr

import androidx.annotation.VisibleForTesting
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.NativeAuthRequest
import com.microsoft.identity.common.internal.util.ArgUtils
import java.net.URL

class SsprPollCompletionRequest @VisibleForTesting private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthRequestParameters
) : NativeAuthRequest() {

    companion object {
        fun create(
            clientId: String,
            passwordResetToken: String,
            requestUrl: String,
            headers: Map<String, String?>
        ): SsprPollCompletionRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(passwordResetToken, "passwordResetToken")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return SsprPollCompletionRequest(
                requestUrl = URL(requestUrl),
                headers = headers,
                parameters = NativeAuthSsprPollCompletionRequestBody(
                    clientId = clientId,
                    passwordSubmitToken = passwordResetToken
                )
            )
        }
    }

    data class NativeAuthSsprPollCompletionRequestBody(
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("password_submit_token") val passwordSubmitToken: String
    ) : NativeAuthRequestParameters()
}
