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
package com.microsoft.identity.common.java.providers.nativeauth

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.net.HttpResponse
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInChallengeApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInInitiateApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInTokenApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.util.ApiResultUtil
import com.microsoft.identity.common.java.util.ObjectMapper
import java.net.HttpURLConnection

class NativeAuthResponseHandler {

    companion object {
        const val DEFAULT_ERROR = "unknown_error"
        const val DEFAULT_ERROR_DESCRIPTION = "No error description received"
    }

    private val TAG = NativeAuthResponseHandler::class.java.simpleName

    //region /oauth/v2.0/initiate
    @Throws(ClientException::class)
    fun getSignInInitiateResultFromHttpResponse(
        response: HttpResponse
    ): SignInInitiateApiResponse {
        LogSession.logMethodCall(TAG, "${TAG}.getSignInInitiateResultFromHttpResponse")

        val result = if (response.body.isNullOrBlank()) {
            SignInInitiateApiResponse(
                response.statusCode,
                null,
                null,
                DEFAULT_ERROR,
                DEFAULT_ERROR_DESCRIPTION,
                null,
                null,
                null,
                null
            )
        }  else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                SignInInitiateApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode

        ApiResultUtil.logResponse(TAG, result)

        return result
    }
    //endregion

    //region /oauth/v2.0/challenge
    @Throws(ClientException::class)
    fun getSignInChallengeResultFromHttpResponse(
        response: HttpResponse
    ): SignInChallengeApiResponse {
        LogSession.logMethodCall(TAG, "${TAG}.getSignInChallengeResultFromHttpResponse")

        val result = if (response.body.isNullOrBlank()) {
            SignInChallengeApiResponse(
                response.statusCode,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                DEFAULT_ERROR,
                null,
                null,
                DEFAULT_ERROR_DESCRIPTION,
                null,
                null)

        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                SignInChallengeApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode

        ApiResultUtil.logResponse(TAG, result)

        return result
    }
    //endregion

    //region /oauth/v2.0/token
    @Throws(ClientException::class)
    fun getSignInTokenApiResultFromHttpResponse(
        response: HttpResponse
    ): SignInTokenApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.getSignInTokenApiResultFromHttpResponse")

        // Use native-auth specific class in case of API error response,
        // or standard MicrosoftStsTokenResponse in case of success response
        if (response.statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            val apiResponse = if (response.body.isNullOrBlank()) {
                SignInTokenApiResponse(
                    response.statusCode,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    DEFAULT_ERROR,
                    DEFAULT_ERROR_DESCRIPTION,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )
            } else {
                ObjectMapper.deserializeJsonStringToObject(
                    response.body,
                    SignInTokenApiResponse::class.java
                )
            }
            ApiResultUtil.logResponse(TAG, apiResponse)
            return apiResponse.toErrorResult()
        } else {
            val apiResponse = ObjectMapper.deserializeJsonStringToObject(
                                response.body,
                                MicrosoftStsTokenResponse::class.java
                            )

            // TODO logging

            return SignInTokenApiResult.Success(tokenResponse = apiResponse)
        }
    }
    //endregion
}
