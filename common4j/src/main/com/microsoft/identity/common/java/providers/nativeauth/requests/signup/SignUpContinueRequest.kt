package com.microsoft.identity.common.java.providers.nativeauth.requests.signup

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.providers.nativeauth.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import java.net.URL

data class SignUpContinueRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthRequestSignUpContinueParameters
) : NativeAuthRequest() {

    companion object {
        fun create(
                password: String? = null,
                attributes: Map<String, String>? = null,
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
//                    attributes = attributes?.toJsonString(), // TODO
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
        val attributes: String? = null,
        val oob: String?,
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("signup_token") val signUpToken: String,
        @SerializedName("grant_type") val grantType: String
    ) : NativeAuthRequestParameters()
}
