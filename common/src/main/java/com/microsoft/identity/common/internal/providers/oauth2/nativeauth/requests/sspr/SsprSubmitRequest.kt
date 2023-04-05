package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.sspr

import androidx.annotation.VisibleForTesting
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.NativeAuthRequest
import com.microsoft.identity.common.internal.util.ArgUtils
import java.net.URL

class SsprSubmitRequest @VisibleForTesting private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthRequestParameters
) : NativeAuthRequest() {

    companion object {
        fun create(
            clientId: String,
            passwordSubmitToken: String,
            newPassword: String,
            requestUrl: String,
            headers: Map<String, String?>
        ): SsprSubmitRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(passwordSubmitToken, "passwordSubmitToken")
            ArgUtils.validateNonNullArg(newPassword, "newPassword")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return SsprSubmitRequest(
                requestUrl = URL(requestUrl),
                headers = headers,
                parameters = NativeAuthSsprSubmitRequestBody(
                    clientId = clientId,
                    passwordSubmitToken = passwordSubmitToken,
                    newPassword = newPassword
                )
            )
        }
    }

    data class NativeAuthSsprSubmitRequestBody(
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("password_submit_token") val passwordSubmitToken: String,
        @SerializedName("new_password") val newPassword: String
    ) : NativeAuthRequestParameters()
}
