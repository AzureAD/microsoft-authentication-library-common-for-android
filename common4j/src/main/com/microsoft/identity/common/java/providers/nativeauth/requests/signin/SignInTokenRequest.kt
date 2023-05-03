package com.microsoft.identity.common.java.providers.nativeauth.requests.signin

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthConstants
import com.microsoft.identity.common.java.providers.nativeauth.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import com.microsoft.identity.common.java.util.StringUtil
import java.net.URL

data class SignInTokenRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthRequestSignInTokenParameters
) : NativeAuthRequest() {

    companion object {
        fun createROPCTokenRequest(
            username: String,
            password: String,
            clientId: String,
            scopes: List<String>? = null,
            challengeType: String? = null,
            requestUrl: String,
            headers: Map<String, String?>
        ): SignInTokenRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(username, "username")
            ArgUtils.validateNonNullArg(password, "password")
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(challengeType, "challengeType")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return SignInTokenRequest(
                parameters = NativeAuthRequestSignInTokenParameters(
                    username = username,
                    password = password,
                    clientId = clientId,
                    grantType = NativeAuthConstants.GrantType.PASSWORD,
                    challengeType = challengeType,
                    scope = if (scopes != null) StringUtil.join(" ", scopes) else null
                ),
                requestUrl = URL(requestUrl),
                headers = headers,
            )
        }

        fun createOOBTokenRequest(
            oob: String,
            credentialToken: String,
            clientId: String,
            scopes: List<String>? = null,
            challengeType: String? = null,
            requestUrl: String,
            headers: Map<String, String?>
        ): SignInTokenRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(oob, "oob")
            ArgUtils.validateNonNullArg(credentialToken, "credentialToken")
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(challengeType, "challengeType")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")


            return SignInTokenRequest(
                parameters = NativeAuthRequestSignInTokenParameters(
                    oob = oob,
                    credentialToken = credentialToken,
                    clientId = clientId,
                    grantType = NativeAuthConstants.GrantType.OOB,
                    challengeType = challengeType,
                    scope  = if (scopes != null) StringUtil.join(" ", scopes) else null
                ),
                requestUrl = URL(requestUrl),
                headers = headers,
            )
        }

        fun createSltTokenRequest(
            signInSlt: String,
            clientId: String,
            scope: String? = null,
            challengeType: String? = null,
            requestUrl: String,
            headers: Map<String, String?>
        ): SignInTokenRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(signInSlt, "signInSlt")
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(challengeType, "challengeType")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")


            return SignInTokenRequest(
                parameters = NativeAuthRequestSignInTokenParameters(
                    signInSlt = signInSlt,
                    clientId = clientId,
                    grantType = NativeAuthConstants.GrantType.OOB,
                    challengeType = challengeType,
                    scope = scope
                ),
                requestUrl = URL(requestUrl),
                headers = headers
            )
        }
    }

    data class NativeAuthRequestSignInTokenParameters(
        val username: String? = null,
        val password: String? = null,
        val oob: String? = null,
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("grant_type") val grantType: String,
        @SerializedName("credential_token") val credentialToken: String? = null,
        @SerializedName("signin_slt") val signInSlt: String? = null,
        @SerializedName("scope") val scope: String?,
        @SerializedName("challenge_type") val challengeType: String?
    ) : NativeAuthRequestParameters()
}
