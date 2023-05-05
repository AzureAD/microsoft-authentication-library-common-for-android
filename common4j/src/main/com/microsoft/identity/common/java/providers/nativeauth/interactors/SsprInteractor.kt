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

import com.microsoft.identity.common.internal.util.getEncodedRequest
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthRequestProvider
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthResponseHandler
import com.microsoft.identity.common.java.providers.nativeauth.requests.sspr.SsprChallengeRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.sspr.SsprContinueRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.sspr.SsprPollCompletionRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.sspr.SsprStartRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.sspr.SsprSubmitRequest
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprContinueApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprPollCompletionApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprStartApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprSubmitApiResult
import com.microsoft.identity.common.java.util.ObjectMapper

class SsprInteractor(
    private val httpClient: UrlConnectionHttpClient,
    private val nativeAuthRequestProvider: NativeAuthRequestProvider,
    private val nativeAuthResponseHandler: NativeAuthResponseHandler
) {
    //region /resetpassword/start
    fun performSsprStart(
        parameters: SsprStartCommandParameters
    ): SsprStartApiResult {
        val request = nativeAuthRequestProvider.createSsprStartRequest(parameters = parameters)
        return performSsprStart(request)
    }

    private fun performSsprStart(request: SsprStartRequest): SsprStartApiResult {
        val encodedRequest: String = request.parameters.getEncodedRequest()
        val headers = request.headers
        val requestUrl = request.requestUrl

        val httpResponse = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val apiResponse = nativeAuthResponseHandler.getSsprStartApiResponseFromHttpResponse(
            response = httpResponse
        )
        return apiResponse.toResult()
    }
    //endregion

    //region /resetpassword/challenge
    fun performSsprChallenge(
        passwordResetToken: String
    ): SsprChallengeApiResult {
        val request = nativeAuthRequestProvider.createSsprChallengeRequest(
            passwordResetToken = passwordResetToken
        )
        return performSsprChallenge(request)
    }

    private fun performSsprChallenge(request: SsprChallengeRequest): SsprChallengeApiResult {
        val encodedRequest: String = request.parameters.getEncodedRequest()
        val headers = request.headers
        val requestUrl = request.requestUrl

        val httpResponse = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val apiResponse = nativeAuthResponseHandler.getSsprChallengeApiResponseFromHttpResponse(
            response = httpResponse
        )
        return apiResponse.toResult()
    }
    //endregion

    //region /resetpassword/continue
    fun performSsprContinue(
        parameters: SsprSubmitCodeCommandParameters
    ): SsprContinueApiResult {
        val request = nativeAuthRequestProvider.createSsprContinueRequest(
            parameters = parameters
        )
        return performSsprContinue(request)
    }

    private fun performSsprContinue(request: SsprContinueRequest): SsprContinueApiResult {
        val encodedRequest: String = request.parameters.getEncodedRequest()
        val headers = request.headers
        val requestUrl = request.requestUrl

        val httpResponse = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val apiResponse = nativeAuthResponseHandler.getSsprContinueApiResponseFromHttpResponse(
            response = httpResponse
        )
        return apiResponse.toResult()
    }
    //endregion

    //region /resetpassword/submit
    fun performSsprSubmit(
        commandParameters: SsprSubmitNewPasswordCommandParameters
    ): SsprSubmitApiResult {
        val request = nativeAuthRequestProvider.createSsprSubmitRequest(
            commandParameters = commandParameters
        )
        return performSsprSubmit(request)
    }

    private fun performSsprSubmit(request: SsprSubmitRequest): SsprSubmitApiResult {
        val encodedRequest: String = request.parameters.getEncodedRequest()
        val headers = request.headers
        val requestUrl = request.requestUrl

        val httpResponse = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val apiResponse = nativeAuthResponseHandler.getSsprSubmitApiResponseFromHttpResponse(
            response = httpResponse
        )
        return apiResponse.toResult()
    }
    //endregion

    //region /resetpassword/poll_completion
    fun performSsprPollCompletion(
        passwordResetToken: String
    ): SsprPollCompletionApiResult {
        val request = nativeAuthRequestProvider.createSsprPollCompletionRequest(
            passwordResetToken = passwordResetToken
        )
        return performSsprPollCompletion(request)
    }

    private fun performSsprPollCompletion(request: SsprPollCompletionRequest): SsprPollCompletionApiResult {
        val encodedRequest: String = request.parameters.getEncodedRequest()
        val headers = request.headers
        val requestUrl = request.requestUrl

        val httpResponse = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val apiResponse = nativeAuthResponseHandler.getSsprPollCompletionApiResponseFromHttpResponse(
            response = httpResponse
        )
        return apiResponse.toResult()
    }
    //endregion
}
