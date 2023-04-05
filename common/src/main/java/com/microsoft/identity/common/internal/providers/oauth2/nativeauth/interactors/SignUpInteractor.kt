package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.interactors

import com.microsoft.identity.common.internal.commands.parameters.SignUpStartCommandParameters
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.NativeAuthRequestProvider
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.NativeAuthResponseHandler
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.signup.SignUpChallengeRequest
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.signup.SignUpStartRequest
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpChallengeResult
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpStartResult
import com.microsoft.identity.common.internal.util.getEncodedRequest
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.util.ObjectMapper

class SignUpInteractor(
    private val httpClient: UrlConnectionHttpClient,
    private val nativeAuthRequestProvider: NativeAuthRequestProvider,
    private val nativeAuthResponseHandler: NativeAuthResponseHandler
) {
    //region /signup/start
    fun performSignUpStart(
        commandParameters: SignUpStartCommandParameters
    ): SignUpStartResult {
        val request = createSignUpStartRequest(commandParameters)
        return performSignUpStart(request)
    }

    private fun createSignUpStartRequest(
        commandParameters: SignUpStartCommandParameters
    ): SignUpStartRequest {
        return nativeAuthRequestProvider.createSignUpStartRequest(
            commandParameters = commandParameters
        )
    }

    private fun performSignUpStart(request: SignUpStartRequest): SignUpStartResult {
        val encodedRequest: String = request.parameters.getEncodedRequest()
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val result = nativeAuthResponseHandler.getSignUpStartResultFromHttpResponse(
            response = response
        )
        nativeAuthResponseHandler.validateApiResult(result)
        return result
    }
    //endregion

    //region /signup/challenge
    fun performSignUpChallenge(
        signUpToken: String
    ): SignUpChallengeResult {
        val request = createSignUpChallengeRequest(
            signUpToken = signUpToken
        )
        return performSignUpChallenge(request)
    }

    private fun createSignUpChallengeRequest(
        signUpToken: String
    ): SignUpChallengeRequest {
        return nativeAuthRequestProvider.createSignUpChallengeRequest(
            signUpToken = signUpToken
        )
    }

    private fun performSignUpChallenge(request: SignUpChallengeRequest): SignUpChallengeResult {
        val encodedRequest: String = request.parameters.getEncodedRequest()
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val result = nativeAuthResponseHandler.getSignUpChallengeResultFromHttpResponse(
            response = response
        )
        nativeAuthResponseHandler.validateApiResult(result)
        return result
    }
    //endregion
}
