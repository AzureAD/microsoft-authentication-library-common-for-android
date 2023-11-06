package com.microsoft.identity.common.java.providers.nativeauth.interactors

import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpStartUsingPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitUserAttributesCommandParameters
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthRequestProvider
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthResponseHandler
import com.microsoft.identity.common.java.providers.nativeauth.requests.signup.SignUpChallengeRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.signup.SignUpContinueRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.signup.SignUpStartRequest
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpContinueApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpStartApiResult
import com.microsoft.identity.common.java.util.ObjectMapper
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitPasswordCommandParameters as SignUpSubmitPasswordCommandParameters1

/**
 * Acts as a binding layer between the request providers and response handlers for a given request.
 * The SignUpInteractor constructs a request for a given endpoint using the command parameters passed into the method,
 * passes that request to the provided HTTP client, and maps the response from that request.
 * @param UrlConnectionHttpClient Used for making HTTP requests with the request object returned from the NativeAuthRequestProvider
 * @param NativeAuthRequestProvider Constructs a request for a given endpoint using the passed command parameters
 * @param NativeAuthResponseHandler Maps the raw HTTP response into a Kotlin class, handling any errors present in the response
 *
 * Used for performing requests to the /start, /challenge, and /continue Sign Up endpoints.
 */
class SignUpInteractor(
    private val httpClient: UrlConnectionHttpClient,
    private val nativeAuthRequestProvider: NativeAuthRequestProvider,
    private val nativeAuthResponseHandler: NativeAuthResponseHandler
) {
    private val TAG:String = SignUpInteractor::class.java.simpleName

    //region /signup/start
    fun performSignUpStart(
        commandParameters: SignUpStartCommandParameters
    ): SignUpStartApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSignUpStart")

        val request = nativeAuthRequestProvider.createSignUpStartRequest(
            commandParameters = commandParameters
        )
        try {
            return performSignUpStart(request)
        } finally {
            StringUtil.overwriteWithNull(request.parameters.password)
        }
    }

    fun performSignUpStartUsingPassword(
        commandParameters: SignUpStartUsingPasswordCommandParameters
    ): SignUpStartApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSignUpStartUsingPassword")
        val request = nativeAuthRequestProvider.createSignUpUsingPasswordStartRequest(
            commandParameters = commandParameters
        )
        try {
            return performSignUpStart(request)
        } finally {
            StringUtil.overwriteWithNull(request.parameters.password)
        }
    }

    private fun performSignUpStart(request: SignUpStartRequest): SignUpStartApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSignUpStart")

        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val rawApiResponse = nativeAuthResponseHandler.getSignUpStartResultFromHttpResponse(
            response = response
        )
        return rawApiResponse.toResult()
    }
    //endregion

    //region /signup/challenge
    fun performSignUpChallenge(
        signUpToken: String
    ): SignUpChallengeApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSignUpChallenge")

        val request = nativeAuthRequestProvider.createSignUpChallengeRequest(
            signUpToken = signUpToken
        )
        return performSignUpChallenge(request)
    }

    private fun performSignUpChallenge(request: SignUpChallengeRequest): SignUpChallengeApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSignUpChallenge")

        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val rawApiResponse = nativeAuthResponseHandler.getSignUpChallengeResultFromHttpResponse(
            response = response
        )
        return rawApiResponse.toResult()
    }
    //endregion

    //region /signup/continue
    fun performSignUpSubmitCode(commandParameters: SignUpSubmitCodeCommandParameters): SignUpContinueApiResult {
        val request = nativeAuthRequestProvider.createSignUpSubmitCodeRequest(
            commandParameters = commandParameters
        )

        return performSignUpContinue(request)
    }

    fun performSignUpSubmitPassword(commandParameters: SignUpSubmitPasswordCommandParameters1): SignUpContinueApiResult {
        val request = nativeAuthRequestProvider.createSignUpSubmitPasswordRequest(
            commandParameters = commandParameters
        )

        try {
            return performSignUpContinue(request)
        } finally {
            StringUtil.overwriteWithNull(request.parameters.password)
        }
    }

    fun performSignUpSubmitUserAttributes(commandParameters: SignUpSubmitUserAttributesCommandParameters): SignUpContinueApiResult {
        val request = nativeAuthRequestProvider.createSignUpSubmitUserAttributesRequest(
            commandParameters = commandParameters
        )

        return performSignUpContinue(request)
    }

    private fun performSignUpContinue(request: SignUpContinueRequest): SignUpContinueApiResult {
        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val rawApiResponse = nativeAuthResponseHandler.getSignUpContinueResultFromHttpResponse(
            response = response
        )
        return rawApiResponse.toResult()
    }
    //endregion
}
