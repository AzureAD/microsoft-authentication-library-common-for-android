package com.microsoft.identity.common.java.providers.nativeauth.requests.sspr

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.providers.nativeauth.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import java.net.URL

class SsprContinueRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override var parameters: NativeAuthRequestParameters
) : NativeAuthRequest() {

    companion object {
        fun create(
            clientId: String,
            grantType: String,
            passwordResetToken: String,
            oob: String,
            requestUrl: String,
            headers: Map<String, String?>
        ): SsprContinueRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(grantType, "grantType")
            ArgUtils.validateNonNullArg(passwordResetToken, "passwordResetToken")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            if (grantType == "oob") {
                ArgUtils.validateNonNullArg(oob, "oob")
            }

            return SsprContinueRequest(
                requestUrl = URL(requestUrl),
                headers = headers,
                parameters = NativeAuthSsprContinueRequestBody(
                    clientId = clientId,
                    grantType = grantType,
                    passwordResetToken = passwordResetToken,
                    oob = oob
                )
            )
        }
    }

    data class NativeAuthSsprContinueRequestBody(
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("grant_type") val grantType: String,
        @SerializedName("password_reset_token") val passwordResetToken: String,
        @SerializedName("oob") val oob: String
    ) : NativeAuthRequestParameters()
}
