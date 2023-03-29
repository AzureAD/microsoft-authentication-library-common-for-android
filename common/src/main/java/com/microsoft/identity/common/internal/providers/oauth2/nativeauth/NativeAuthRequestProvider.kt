package com.microsoft.identity.common.internal.providers.oauth2.nativeauth

import com.microsoft.identity.common.internal.commands.parameters.SignUpChallengeCommandParameters
import com.microsoft.identity.common.internal.commands.parameters.SignUpStartCommandParameters
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.signup.SignUpChallengeRequest
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.signup.SignUpStartRequest
import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.net.HttpConstants
import java.util.TreeMap

class NativeAuthRequestProvider(private val config: NativeAuthOAuth2Configuration) {
    private val TAG = NativeAuthRequestProvider::class.java.simpleName

    private val signUpStartEndpoint = config.getSignUpStartEndpoint().toString()
    private val signUpChallengeEndpoint = config.getSignUpChallengeEndpoint().toString()

    //region /signup/start
    fun createSignUpStartRequest(
        commandParameters: SignUpStartCommandParameters
    ): SignUpStartRequest {
        val methodName = ":createSignUpStartRequest"
        Logger.verbose(
            TAG + methodName,
            "Creating SignUpStartRequest..."
        )

        return SignUpStartRequest.create(
            username = commandParameters.email,
            password = commandParameters.password,
            attributes = commandParameters.userAttributes,
            challengeTypes = config.challengeTypes,
            clientId = config.clientId,
            requestUrl = signUpStartEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion

    //region /signup/challenge
    fun createSignUpChallengeRequest(
        signUpToken: String,
        commandParameters: SignUpChallengeCommandParameters
    ): SignUpChallengeRequest {
        val methodName = ":createSignUpChallengeRequest"
        Logger.verbose(
            TAG + methodName,
            "Creating SignUpChallengeRequest..."
        )

        return SignUpChallengeRequest.create(
            signUpToken = signUpToken,
            clientId = config.clientId,
            challengeTypes = config.challengeTypes,
            requestUrl = signUpChallengeEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion

    //region helpers
    private fun getRequestHeaders(): Map<String, String?> {
        // TODO why a treemap, why sorted headers?
        val headers: MutableMap<String, String?> = TreeMap()
        headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID] =
            DiagnosticContext.INSTANCE.requestContext[DiagnosticContext.CORRELATION_ID]
        headers[HttpConstants.HeaderField.CONTENT_TYPE] = "application/x-www-form-urlencoded"
        return headers
    }
    //endregion
}
