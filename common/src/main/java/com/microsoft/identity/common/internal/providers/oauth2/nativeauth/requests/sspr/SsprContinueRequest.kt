package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.sspr

import androidx.annotation.VisibleForTesting
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.NativeAuthRequest
import com.microsoft.identity.common.internal.util.ArgUtils
import java.net.URL

class SsprContinueRequest @VisibleForTesting private constructor(
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
            ArgUtils.validateNonNullArg(oob, "oob")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

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
