package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.interactors

import com.microsoft.identity.common.internal.commands.parameters.SignInCommandParameters
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.NativeAuthRequestProvider
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.NativeAuthResponseHandler
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.signin.SignInChallengeRequest
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.signin.SignInInitiateRequest
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.signin.SignInTokenRequest
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signin.SignInChallengeResult
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signin.SignInInitiateResult
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signin.SignInTokenResult
import com.microsoft.identity.common.internal.util.getEncodedRequest
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.util.ObjectMapper

class SignInInteractor(
    private val httpClient: UrlConnectionHttpClient,
    private val nativeAuthRequestProvider: NativeAuthRequestProvider,
    private val nativeAuthResponseHandler: NativeAuthResponseHandler
) {
    //region /oauth/v2.0/initiate
    fun performSignInInitiate(
        commandParameters: SignInCommandParameters
    ): SignInInitiateResult {
        val request = createSignInInitiateRequest(commandParameters)
        return performSignInInitiate(request)
    }

    private fun createSignInInitiateRequest(
        commandParameters: SignInCommandParameters
    ): SignInInitiateRequest {
        return nativeAuthRequestProvider.createSignInInitiateRequest(
            commandParameters = commandParameters
        )
    }

    private fun performSignInInitiate(request: SignInInitiateRequest): SignInInitiateResult {
        val encodedRequest: String = request.parameters.getEncodedRequest()
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val result = nativeAuthResponseHandler.getSignInInitiateResultFromHttpResponse(
            response = response
        )
        nativeAuthResponseHandler.validateApiResult(result)
        return result
    }
    //endregion

    //region /oauth/v2.0/challenge
    fun performSignInChallenge(
        credentialToken: String,
    ): SignInChallengeResult {
        val request = createSignInChallengeRequest(credentialToken)
        return performSignInChallenge(request)
    }

    private fun createSignInChallengeRequest(
        credentialToken: String
    ): SignInChallengeRequest {
        return nativeAuthRequestProvider.createSignInChallengeRequest(
            credentialToken = credentialToken,
        )
    }

    private fun performSignInChallenge(request: SignInChallengeRequest): SignInChallengeResult {
        val encodedRequest: String = request.parameters.getEncodedRequest()
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val result = nativeAuthResponseHandler.getSignInChallengeResultFromHttpResponse(
            response = response
        )
        nativeAuthResponseHandler.validateApiResult(result)
        return result
    }
    //endregion

    //region /oauth/v2.0/token
    fun performGetToken(
        signInSlt: String? = null,
        credentialToken: String? = null,
        signInCommandParameters: SignInCommandParameters
    ): SignInTokenResult {
        val request = createSignInTokenRequest(signInSlt, credentialToken, signInCommandParameters)
        return performGetToken(request)
    }

    private fun createSignInTokenRequest(
        signInSlt: String? = null,
        credentialToken: String? = null,
        signInCommandParameters: SignInCommandParameters
    ): SignInTokenRequest {
        return nativeAuthRequestProvider.createTokenRequest(
            signInSlt = signInSlt,
            credentialToken = credentialToken,
            signInCommandParameters = signInCommandParameters
        )
    }

    private fun performGetToken(request: SignInTokenRequest): SignInTokenResult {
        val encodedRequest: String = request.parameters.getEncodedRequest()
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val result = nativeAuthResponseHandler.getSignInTokenResultFromHttpResponse(
            response = response
        )
        nativeAuthResponseHandler.validateApiResult(result)
        return result
    }
    //endregion
}
