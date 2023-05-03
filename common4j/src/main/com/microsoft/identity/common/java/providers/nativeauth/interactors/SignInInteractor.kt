package com.microsoft.identity.common.java.providers.nativeauth.interactors

import com.microsoft.identity.common.internal.util.getEncodedRequest
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartWithPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthRequestProvider
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthResponseHandler
import com.microsoft.identity.common.java.providers.nativeauth.requests.signin.SignInChallengeRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.signin.SignInInitiateRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.signin.SignInTokenRequest
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInInitiateApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.util.ObjectMapper

class SignInInteractor(
    private val httpClient: UrlConnectionHttpClient,
    private val nativeAuthRequestProvider: NativeAuthRequestProvider,
    private val nativeAuthResponseHandler: NativeAuthResponseHandler
) {
    //region /oauth/v2.0/initiate
    fun performSignInInitiate(
        parameters: SignInStartCommandParameters
    ): SignInInitiateApiResult {
        val request = nativeAuthRequestProvider.createSignInInitiateRequest(
            parameters = parameters
        )
        return performSignInInitiate(request)
    }

    private fun performSignInInitiate(request: SignInInitiateRequest): SignInInitiateApiResult {
        val encodedRequest: String = request.parameters.getEncodedRequest()
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val rawApiResponse = nativeAuthResponseHandler.getSignInInitiateResultFromHttpResponse(
            response = response
        )
        return rawApiResponse.toResult()
    }
    //endregion

    //region /oauth/v2.0/challenge
    fun performSignInChallenge(
        credentialToken: String,
    ): SignInChallengeApiResult {
        val request = nativeAuthRequestProvider.createSignInChallengeRequest(
            credentialToken = credentialToken
        )
        return performSignInChallenge(request)
    }

    private fun performSignInChallenge(request: SignInChallengeRequest): SignInChallengeApiResult {
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
        return result.toResult()
    }
    //endregion

    //region /oauth/v2.0/token
    fun performROPCTokenRequest(
        parameters: SignInStartWithPasswordCommandParameters
    ): SignInTokenApiResult {
        val request = nativeAuthRequestProvider.createROPCTokenRequest(
            parameters = parameters
        )
        return performGetToken(request)
    }

    fun performOOBTokenRequest(
        parameters: SignInSubmitCodeCommandParameters
    ): SignInTokenApiResult {
        val request = nativeAuthRequestProvider.createOOBTokenRequest(
            parameters = parameters
        )
        return performGetToken(request)
    }

    private fun performGetToken(request: SignInTokenRequest): SignInTokenApiResult {
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
        return result.toResult()
    }
    //endregion
}
