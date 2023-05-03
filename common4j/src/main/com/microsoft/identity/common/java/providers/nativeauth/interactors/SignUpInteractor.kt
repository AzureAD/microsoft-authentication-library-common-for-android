package com.microsoft.identity.common.java.providers.nativeauth.interactors

import com.microsoft.identity.common.internal.util.getEncodedRequest
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpContinueCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpStartCommandParameters
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthRequestProvider
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthResponseHandler
import com.microsoft.identity.common.java.providers.nativeauth.requests.signup.SignUpChallengeRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.signup.SignUpContinueRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.signup.SignUpStartRequest
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.challenge.SignUpChallengeResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.cont.SignUpContinueResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.start.SignUpStartResult
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
            signUpToken = signUpToken,
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

    //region /signup/continue
    fun performSignUpContinue(
        signUpToken: String,
        commandParameters: SignUpContinueCommandParameters
    ): SignUpContinueResult {
        val request = createSignUpContinueRequest(
            signUpToken = signUpToken,
            commandParameters = commandParameters
        )
        return performSignUpContinue(request)
    }

    private fun createSignUpContinueRequest(
        signUpToken: String,
        commandParameters: SignUpContinueCommandParameters
    ): SignUpContinueRequest {
        return nativeAuthRequestProvider.createSignUpContinueRequest(
            signUpToken = signUpToken,
            commandParameters = commandParameters
        )
    }

    private fun performSignUpContinue(request: SignUpContinueRequest): SignUpContinueResult {
        val encodedRequest: String = request.parameters.getEncodedRequest()
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val result = nativeAuthResponseHandler.getSignUpContinueResultFromHttpResponse(
            response = response
        )
        nativeAuthResponseHandler.validateApiResult(result)
        return result
    }
    //endregion
}
