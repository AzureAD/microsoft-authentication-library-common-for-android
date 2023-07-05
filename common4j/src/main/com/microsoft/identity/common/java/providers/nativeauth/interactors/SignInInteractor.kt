package com.microsoft.identity.common.java.providers.nativeauth.interactors

import com.microsoft.identity.common.internal.util.getEncodedRequest
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInWithSLTCommandParameters
import com.microsoft.identity.common.java.logging.LogSession
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
    private val TAG:String = SignInInteractor::class.java.simpleName
    //region /oauth/v2.0/initiate
    fun performSignInInitiate(
        parameters: SignInStartCommandParameters
    ): SignInInitiateApiResult {
        LogSession.logMethodCall(tag = TAG)
        val request = nativeAuthRequestProvider.createSignInInitiateRequest(
            parameters = parameters
        )
        return performSignInInitiate(request)
    }

    private fun performSignInInitiate(request: SignInInitiateRequest): SignInInitiateApiResult {
        LogSession.logMethodCall(tag = TAG)
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
        LogSession.logMethodCall(tag = TAG)
        val request = nativeAuthRequestProvider.createSignInChallengeRequest(
            credentialToken = credentialToken
        )
        return performSignInChallenge(request)
    }

    private fun performSignInChallenge(request: SignInChallengeRequest): SignInChallengeApiResult {
        LogSession.logMethodCall(tag = TAG)
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
    fun performOOBTokenRequest(
        parameters: SignInSubmitCodeCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(tag = TAG)
        val request = nativeAuthRequestProvider.createOOBTokenRequest(
            parameters = parameters
        )
        return performGetToken(request)
    }

    fun performSLTTokenRequest(
        parameters: SignInWithSLTCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(tag = TAG)
        val request = nativeAuthRequestProvider.createSLTTokenRequest(
            parameters = parameters
        )
        return performGetToken(request)
    }

    fun performPasswordTokenRequest(
        parameters: SignInSubmitPasswordCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(tag = TAG)
        val request = nativeAuthRequestProvider.createPasswordTokenRequest(
            parameters = parameters
        )
        return performGetToken(request)
    }

    private fun performGetToken(request: SignInTokenRequest): SignInTokenApiResult {
        LogSession.logMethodCall(tag = TAG)
        val encodedRequest: String = request.parameters.getEncodedRequest()
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        return nativeAuthResponseHandler.getSignInTokenApiResultFromHttpResponse(
            response = response
        )
    }
    //endregion
}
