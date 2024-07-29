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
package com.microsoft.identity.common.java.nativeauth.providers.interactors

import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthResponseHandler
import com.microsoft.identity.common.java.nativeauth.providers.requests.resetpassword.ResetPasswordChallengeRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.resetpassword.ResetPasswordContinueRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.resetpassword.ResetPasswordPollCompletionRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.resetpassword.ResetPasswordStartRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.resetpassword.ResetPasswordSubmitRequest
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordContinueApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordPollCompletionApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordStartApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordSubmitApiResult
import com.microsoft.identity.common.java.util.ObjectMapper
import com.microsoft.identity.common.java.util.StringUtil

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
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.performResetPasswordStart(parameters: ResetPasswordStartCommandParameters)"
        )

        val request = nativeAuthRequestProvider.createResetPasswordStartRequest(commandParameters = parameters)

        Logger.infoWithObject(
            "${TAG}.performResetPasswordStart",
            parameters.getCorrelationId(),
            "request = ",
            request
        )

        return performResetPasswordStart(
            requestCorrelationId = parameters.getCorrelationId(),
            request = request
        )
    }

    private fun performResetPasswordStart(
        requestCorrelationId: String,
        request: ResetPasswordStartRequest
    ): ResetPasswordStartApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = requestCorrelationId,
            methodName = "${TAG}.performResetPasswordStart"
        )

        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val httpResponse = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )

        val apiResponse = nativeAuthResponseHandler.getResetPasswordStartApiResponseFromHttpResponse(
            requestCorrelationId = requestCorrelationId,
            response = httpResponse
        )

        Logger.infoWithObject(
            "${TAG}.rawResponseToResetPasswordStartApiResult",
            apiResponse.correlationId,
            "rawApiResponse = ",
            apiResponse
        )

        val result = apiResponse.toResult()

        Logger.infoWithObject(
            "${TAG}.rawResponseToResetPasswordStartApiResult",
            result.correlationId,
            "result = ",
            result
        )

        return result
    }
    //endregion

    //region /resetpassword/challenge
    fun performResetPasswordChallenge(
        continuationToken: String,
        correlationId: String
    ): ResetPasswordChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.performResetPasswordChallenge(continuationToken: String)"
        )

        val request = nativeAuthRequestProvider.createResetPasswordChallengeRequest(
            continuationToken = continuationToken,
            correlationId = correlationId
        )

        Logger.infoWithObject(
            "${TAG}.performResetPasswordChallenge",
            correlationId,
            "request = ",
            request
        )

        return performResetPasswordChallenge(
            requestCorrelationId = correlationId,
            request = request
        )
    }

    private fun performResetPasswordChallenge(
        requestCorrelationId: String,
        request: ResetPasswordChallengeRequest
    ): ResetPasswordChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = requestCorrelationId,
            methodName = "${TAG}.performResetPasswordChallenge"
        )

        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val httpResponse = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val apiResponse = nativeAuthResponseHandler.getResetPasswordChallengeApiResponseFromHttpResponse(
            response = httpResponse,
            requestCorrelationId = requestCorrelationId
        )

        Logger.infoWithObject(
            "${TAG}.rawResponseToResetPasswordChallengeApiResult",
            apiResponse.correlationId,
            "rawApiResponse = ",
            apiResponse
        )

        val result = apiResponse.toResult()

        Logger.infoWithObject(
            "${TAG}.rawResponseToResetPasswordChallengeApiResult",
            result.correlationId,
            "result = ",
            result
        )

        return result
    }
    //endregion

    //region /resetpassword/continue
    fun performResetPasswordContinue(
        parameters: ResetPasswordSubmitCodeCommandParameters
    ): ResetPasswordContinueApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.correlationId,
            methodName = "${TAG}.performResetPasswordContinue(parameters: ResetPasswordSubmitCodeCommandParameters)"
        )

        val request = nativeAuthRequestProvider.createResetPasswordContinueRequest(
            commandParameters = parameters
        )

        Logger.infoWithObject(
            "${TAG}.performResetPasswordContinue",
            parameters.getCorrelationId(),
            "request = ",
            request
        )

        return performResetPasswordContinue(
            requestCorrelationId = parameters.getCorrelationId(),
            request = request
        )
    }

    private fun performResetPasswordContinue(
        requestCorrelationId: String,
        request: ResetPasswordContinueRequest
    ): ResetPasswordContinueApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = requestCorrelationId,
            methodName = "${TAG}.performResetPasswordContinue"
        )

        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val httpResponse = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )

        val apiResponse = nativeAuthResponseHandler.getResetPasswordContinueApiResponseFromHttpResponse(
            requestCorrelationId = requestCorrelationId,
            response = httpResponse
        )

        Logger.infoWithObject(
            "${TAG}.rawResponseToResetPasswordContinueApiResult",
            apiResponse.correlationId,
            "rawApiResponse = ",
            apiResponse
        )

        val result = apiResponse.toResult()

        Logger.infoWithObject(
            "${TAG}.rawResponseToResetPasswordContinueApiResult",
            result.correlationId,
            "result = ",
            result
        )

        return result
    }
    //endregion

    //region /resetpassword/submit
    fun performResetPasswordSubmit(
        commandParameters: ResetPasswordSubmitNewPasswordCommandParameters
    ): ResetPasswordSubmitApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = commandParameters.correlationId,
            methodName = "${TAG}.performResetPasswordSubmit(commandParameters: ResetPasswordSubmitNewPasswordCommandParameters)"
        )

        val request = nativeAuthRequestProvider.createResetPasswordSubmitRequest(
            commandParameters = commandParameters
        )

        Logger.infoWithObject(
            "${TAG}.performResetPasswordSubmit",
            commandParameters.getCorrelationId(),
            "request = ",
            request
        )

        try {
            return performResetPasswordSubmit(
                requestCorrelationId = commandParameters.getCorrelationId(),
                request = request
            )
        } finally {
            StringUtil.overwriteWithNull(
                (request.parameters as ResetPasswordSubmitRequest.NativeAuthResetPasswordSubmitRequestParameters).newPassword)
        }
    }

    private fun performResetPasswordSubmit(
        requestCorrelationId: String,
        request: ResetPasswordSubmitRequest
    ): ResetPasswordSubmitApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = requestCorrelationId,
            methodName = "${TAG}.performResetPasswordSubmit"
        )

        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val httpResponse = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )

        val apiResponse = nativeAuthResponseHandler.getResetPasswordSubmitApiResponseFromHttpResponse(
            response = httpResponse,
            requestCorrelationId = requestCorrelationId
        )

        Logger.infoWithObject(
            "${TAG}.rawResponseToResetPasswordContinueApiResult",
            apiResponse.correlationId,
            "rawApiResponse = ",
            apiResponse
        )

        val result = apiResponse.toResult()

        Logger.infoWithObject(
            "${TAG}.rawResponseToResetPasswordSubmitApiResult",
            result.correlationId,
            "result = ",
            result
        )

        return result
    }
    //endregion

    //region /resetpassword/poll_completion
    fun performResetPasswordPollCompletion(
        continuationToken: String,
        correlationId: String
    ): ResetPasswordPollCompletionApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.performResetPasswordPollCompletion(continuationToken: String)"
        )

        val request = nativeAuthRequestProvider.createResetPasswordPollCompletionRequest(
            continuationToken = continuationToken,
            correlationId = correlationId
        )

        Logger.infoWithObject(
            "${TAG}.performResetPasswordPollCompletion",
            correlationId,
            "request = ",
            request
        )

        return performResetPasswordPollCompletion(
            requestCorrelationId = correlationId,
            request = request
        )
    }

    private fun performResetPasswordPollCompletion(
        requestCorrelationId: String,
        request: ResetPasswordPollCompletionRequest
    ): ResetPasswordPollCompletionApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = requestCorrelationId,
            methodName = "${TAG}.performResetPasswordPollCompletion"
        )

        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val httpResponse = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )

        val apiResponse = nativeAuthResponseHandler.getResetPasswordPollCompletionApiResponseFromHttpResponse(
            requestCorrelationId = requestCorrelationId,
            response = httpResponse
        )

        Logger.infoWithObject(
            "${TAG}.rawResponseToResetPasswordCompletionApiResult",
            apiResponse.correlationId,
            "rawApiResponse = ",
            apiResponse
        )

        val result = apiResponse.toResult()

        Logger.infoWithObject(
            "${TAG}.rawResponseToResetPasswordCompletionApiResult",
            result.correlationId,
            "result = ",
            result
        )

        return result
    }
    //endregion
}
