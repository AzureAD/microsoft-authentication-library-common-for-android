package com.microsoft.identity.common.java.providers.nativeauth.requests.signin

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.providers.nativeauth.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import java.net.URL

data class SignInTokenRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthRequestSignInTokenParameters
) : NativeAuthRequest() {

    companion object {
        fun create(
            username: String,
            clientId: String,
            grantType: String,
            password: String? = null,
            oob: String? = null,
            scope: String? = null,
            signInSlt: String? = null,
            credentialToken: String? = null,
            challengeType: String? = null,
            requestUrl: String,
            headers: Map<String, String?>
        ): SignInTokenRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(username, "username")
            ArgUtils.validateNonNullArg(grantType, "grantType")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            if (username.isEmpty() || password.isNullOrEmpty()) {
                ArgUtils.validateNonNullArg(credentialToken, "credentialToken")
            }
            if (grantType == "oob") {
                ArgUtils.validateNonNullArg(oob, "oob")
            }
            if (grantType == "password") {
                ArgUtils.validateNonNullArg(password, "password")
            }

            return SignInTokenRequest(
                parameters = NativeAuthRequestSignInTokenParameters(
                    clientId = clientId,
                    username = username,
                    grantType = grantType,
                    credentialToken = credentialToken,
                    challengeType = challengeType,
                    scope = scope,
                    password = password,
                    oob = oob,
                    signInSlt = signInSlt
                ),
                requestUrl = URL(requestUrl),
                headers = headers
            )
        }
    }

    data class NativeAuthRequestSignInTokenParameters(
        val username: String,
        val password: String?,
        val oob: String?,
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("grant_type") val grantType: String,
        @SerializedName("credential_token") val credentialToken: String?,
        @SerializedName("signin_slt") val signInSlt: String?,
        @SerializedName("scope") val scope: String?,
        @SerializedName("challenge_type") val challengeType: String?
    ) : NativeAuthRequestParameters()
}
