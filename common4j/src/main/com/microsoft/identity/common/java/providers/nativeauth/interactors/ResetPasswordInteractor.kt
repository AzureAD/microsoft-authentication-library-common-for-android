//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.providers.nativeauth.interactors

import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthRequestProvider
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthResponseHandler
import com.microsoft.identity.common.java.providers.nativeauth.requests.resetpassword.ResetPasswordChallengeRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.resetpassword.ResetPasswordContinueRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.resetpassword.ResetPasswordPollCompletionRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.resetpassword.ResetPasswordStartRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.resetpassword.ResetPasswordSubmitRequest
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordContinueApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordPollCompletionApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordStartApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordSubmitApiResult
import com.microsoft.identity.common.java.util.ObjectMapper

/**
 * Acts as a binding layer between the request providers and response handlers for a given request.
 * The ResetPasswordInteractor constructs a request for a given endpoint using the command parameters passed into the method,
 * passes that request to the provided HTTP client, and maps the response from that request.
 * @param UrlConnectionHttpClient Used for making HTTP requests with the request object returned from the NativeAuthRequestProvider
 * @param NativeAuthRequestProvider Constructs a request for a given endpoint using the passed command parameters
 * @param NativeAuthResponseHandler Maps the raw HTTP response into a Kotlin class, handling any errors present in the response
 *
 * Used for performing requests to the /start, /challenge, /continue, /submit, and /poll_completion Reset Password endpoints.
 */
class ResetPasswordInteractor(
    private val httpClient: UrlConnectionHttpClient,
    private val nativeAuthRequestProvider: NativeAuthRequestProvider,
    private val nativeAuthResponseHandler: NativeAuthResponseHandler
) {
    private val TAG:String = ResetPasswordInteractor::class.java.simpleName
    //region /resetpassword/start
    fun performResetPasswordStart(
        parameters: ResetPasswordStartCommandParameters
    ): ResetPasswordStartApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performResetPasswordStart(parameters: ResetPasswordStartCommandParameters)")

        val request = nativeAuthRequestProvider.createResetPasswordStartRequest(parameters = parameters)
        return performResetPasswordStart(request)
    }

    private fun performResetPasswordStart(request: ResetPasswordStartRequest): ResetPasswordStartApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performResetPasswordStart")

        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val httpResponse = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val apiResponse = nativeAuthResponseHandler.getResetPasswordStartApiResponseFromHttpResponse(
            response = httpResponse
        )
        return apiResponse.toResult()
    }
    //endregion

    //region /resetpassword/challenge
    fun performResetPasswordChallenge(
        passwordResetToken: String
    ): ResetPasswordChallengeApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performResetPasswordChallenge(passwordResetToken: String)")

        val request = nativeAuthRequestProvider.createResetPasswordChallengeRequest(
            passwordResetToken = passwordResetToken
        )
        return performResetPasswordChallenge(request)
    }

    private fun performResetPasswordChallenge(request: ResetPasswordChallengeRequest): ResetPasswordChallengeApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performResetPasswordChallenge")

        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val httpResponse = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val apiResponse = nativeAuthResponseHandler.getResetPasswordChallengeApiResponseFromHttpResponse(
            response = httpResponse
        )
        return apiResponse.toResult()
    }
    //endregion

    //region /resetpassword/continue
    fun performResetPasswordContinue(
        parameters: ResetPasswordSubmitCodeCommandParameters
    ): ResetPasswordContinueApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performResetPasswordContinue(parameters: ResetPasswordSubmitCodeCommandParameters)")

        val request = nativeAuthRequestProvider.createResetPasswordContinueRequest(
            parameters = parameters
        )
        return performResetPasswordContinue(request)
    }

    private fun performResetPasswordContinue(request: ResetPasswordContinueRequest): ResetPasswordContinueApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performResetPasswordContinue")

        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val httpResponse = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val apiResponse = nativeAuthResponseHandler.getResetPasswordContinueApiResponseFromHttpResponse(
            response = httpResponse
        )
        return apiResponse.toResult()
    }
    //endregion

    //region /resetpassword/submit
    fun performResetPasswordSubmit(
        commandParameters: ResetPasswordSubmitNewPasswordCommandParameters
    ): ResetPasswordSubmitApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performResetPasswordSubmit(commandParameters: ResetPasswordSubmitNewPasswordCommandParameters)")

        val request = nativeAuthRequestProvider.createResetPasswordSubmitRequest(
            commandParameters = commandParameters
        )
        return performResetPasswordSubmit(request)
    }

    private fun performResetPasswordSubmit(request: ResetPasswordSubmitRequest): ResetPasswordSubmitApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performResetPasswordSubmit")

        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val httpResponse = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val apiResponse = nativeAuthResponseHandler.getResetPasswordSubmitApiResponseFromHttpResponse(
            response = httpResponse
        )
        return apiResponse.toResult()
    }
    //endregion

    //region /resetpassword/poll_completion
    fun performResetPasswordPollCompletion(
        passwordResetToken: String
    ): ResetPasswordPollCompletionApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performResetPasswordPollCompletion(passwordResetToken: String)")

        val request = nativeAuthRequestProvider.createResetPasswordPollCompletionRequest(
            passwordResetToken = passwordResetToken
        )
        return performResetPasswordPollCompletion(request)
    }

    private fun performResetPasswordPollCompletion(request: ResetPasswordPollCompletionRequest): ResetPasswordPollCompletionApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performResetPasswordPollCompletion")

        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val httpResponse = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val apiResponse = nativeAuthResponseHandler.getResetPasswordPollCompletionApiResponseFromHttpResponse(
            response = httpResponse
        )
        return apiResponse.toResult()
    }
    //endregion
}
