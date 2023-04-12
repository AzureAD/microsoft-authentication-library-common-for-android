package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.signup

import androidx.annotation.VisibleForTesting
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.internal.commands.parameters.UserAttributes
import com.microsoft.identity.common.internal.commands.parameters.toJsonString
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.NativeAuthRequest
import com.microsoft.identity.common.internal.util.ArgUtils
import java.net.URL

data class SignUpContinueRequest @VisibleForTesting private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthRequestSignUpContinueParameters
) : NativeAuthRequest() {

    companion object {
        fun create(
            password: String? = null,
            attributes: UserAttributes? = null,
            oob: String? = null,
            clientId: String,
            signUpToken: String,
            grantType: String,
            requestUrl: String,
            headers: Map<String, String?>
        ): SignUpContinueRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(signUpToken, "signUpToken")
            ArgUtils.validateNonNullArg(grantType, "grantType")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")
            if (grantType == "oob") {
                ArgUtils.validateNonNullArg(oob, "oob")
            }
            if (grantType == "password") {
                ArgUtils.validateNonNullArg(password, "password")
            }

            return SignUpContinueRequest(
                parameters = NativeAuthRequestSignUpContinueParameters(
                    password = password,
                    attributes = attributes?.toJsonString(),
                    oob = oob,
                    clientId = clientId,
                    signUpToken = signUpToken,
                    grantType = grantType
                ),
                requestUrl = URL(requestUrl),
                headers = headers
            )
        }
    }

    data class NativeAuthRequestSignUpContinueParameters(
        val password: String?,
        val attributes: String?,
        val oob: String?,
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("signup_token") val signUpToken: String,
        @SerializedName("grant_type") val grantType: String
    ) : NativeAuthRequestParameters()
}
