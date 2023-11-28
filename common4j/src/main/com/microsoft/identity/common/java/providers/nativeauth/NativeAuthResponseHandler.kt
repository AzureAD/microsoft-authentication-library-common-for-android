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
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.net.HttpResponse
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordChallengeApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordContinueApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordPollCompletionApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordStartApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordSubmitApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInChallengeApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInInitiateApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInTokenApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpChallengeApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpContinueApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpStartApiResponse
import com.microsoft.identity.common.java.util.ApiResultUtil
import com.microsoft.identity.common.java.util.ObjectMapper
import java.net.HttpURLConnection

/**
 * NativeAuthResponseHandler provides methods to transform the HTTP responses received
 * from various REST APIs to Java response objects.
 */
class NativeAuthResponseHandler {

    companion object {
        const val EMPTY_RESPONSE_ERROR = "empty_response_error"
        const val EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION = "API response body is empty"
    }

    private val TAG = NativeAuthResponseHandler::class.java.simpleName

    //region /signup/start
    /**
     * Converts the HTTP response for /signup/start API to [SignUpStartApiResponse] object
     * @param response : HTTP response received from the API
     * @return SignUpStartApiResponse object
     */
    @Throws(ClientException::class)
    fun getSignUpStartResultFromHttpResponse(
        response: HttpResponse
    ): SignUpStartApiResponse {
        LogSession.logMethodCall(TAG, "${TAG}.getSignUpStartResultFromHttpResponse")

        val result = if (response.body.isNullOrBlank()) {
            SignUpStartApiResponse(
                response.statusCode,
                EMPTY_RESPONSE_ERROR,
                EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                null,
                null,
                null,
                null,
                null,
                null
            )
        }
        else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                SignUpStartApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode

        ApiResultUtil.logResponse(TAG, result)

        return result
    }
    //endregion

    //region signup/challenge
    /**
     * Converts the HTTP response for /signup/challenge API to [SignUpChallengeApiResponse] object
     * @param response : HTTP response received from the API
     * @return SignUpChallengeApiResponse object
     */
    @Throws(ClientException::class)
    fun getSignUpChallengeResultFromHttpResponse(
        response: HttpResponse
    ): SignUpChallengeApiResponse {
        LogSession.logMethodCall(TAG, "${TAG}.getSignUpChallengeResultFromHttpResponse")

        val result = if (response.body.isNullOrBlank()) {
            SignUpChallengeApiResponse(
                response.statusCode,
                null,
                null,
                null,
                null,
                null,
                null,
                EMPTY_RESPONSE_ERROR,
                EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                null,
                null
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                SignUpChallengeApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode

        ApiResultUtil.logResponse(TAG, result)

        return result
    }
    //endregion

    //region /signup/continue
    /**
     * Converts the HTTP response for /signup/continue API to [SignUpContinueApiResponse] object
     * @param response : HTTP response received from the API
     * @return SignUpContinueApiResponse object
     */
    @Throws(ClientException::class)
    fun getSignUpContinueResultFromHttpResponse(
        response: HttpResponse
    ): SignUpContinueApiResponse {
        LogSession.logMethodCall(TAG, "${TAG}.getSignUpContinueResultFromHttpResponse")

        val result = if (response.body.isNullOrBlank()) {
            SignUpContinueApiResponse(
                response.statusCode,
                null,
                null,
                EMPTY_RESPONSE_ERROR,
                null,
                EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                null,
                null,
                null,
                null,
                null
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                SignUpContinueApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode

        ApiResultUtil.logResponse(TAG, result)

        return result
    }

    //region /oauth/v2.0/initiate
    /**
     * Converts the response for /oauth/v2.0/initiate REST API to Java object
     * @param response HTTP response received from REST API
     */
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
                EMPTY_RESPONSE_ERROR,
                EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
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
    /**
     * Converts the response for /oauth/v2.0/challenge REST API to Java object
     * @param response HTTP response received from REST API
     */
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
                EMPTY_RESPONSE_ERROR,
                null,
                null,
                EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
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
    /**
     * Converts the response for /oauth/v2.0/token REST API to Java object
     * @param response HTTP response received from REST API
     */
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
                    EMPTY_RESPONSE_ERROR,
                    EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
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
            Logger.info(TAG, "MicrosoftStsTokenResponse authority:$apiResponse.authority" +
                    " cloud_instance_host_name:${apiResponse.refreshTokenExpiresIn}" +
                    " isMsaAccount:$apiResponse.isMsaAccount() tenantId $apiResponse.tenantId" +
                    " cloudInstanceHostName $apiResponse.cloudInstanceHostName")

            return SignInTokenApiResult.Success(tokenResponse = apiResponse)
        }
    }
    //endregion

    //region /resetpassword/start
    /**
     * Converts the HTTP response for /resetpassword/start API to [ResetPasswordStartApiResponse] object
     * @param response : HTTP response received from the API
     * @return ResetPasswordStartApiResponse object
     */
    @Throws(ClientException::class)
    fun getResetPasswordStartApiResponseFromHttpResponse(
        response: HttpResponse
    ): ResetPasswordStartApiResponse {
        LogSession.logMethodCall(TAG, "${TAG}.getResetPasswordStartApiResponseFromHttpResponse")

        val apiResponse = if (response.body.isNullOrBlank()) {
            ResetPasswordStartApiResponse(
                response.statusCode,
                null,
                null,
                EMPTY_RESPONSE_ERROR,
                EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                null,
                null,
                null
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                ResetPasswordStartApiResponse::class.java
            )
        }
        apiResponse.statusCode = response.statusCode

        ApiResultUtil.logResponse(TAG, apiResponse)

        return apiResponse
    }

    //region /resetpassword/challenge
    /**
     * Converts the HTTP response for /resetpassword/challenge API to [ResetPasswordChallengeApiResponse] object
     * @param response : HTTP response received from the API
     * @return ResetPasswordChallengeApiResponse object
     */
    @Throws(ClientException::class)
    fun getResetPasswordChallengeApiResponseFromHttpResponse(
        response: HttpResponse
    ): ResetPasswordChallengeApiResponse {
        LogSession.logMethodCall(TAG, "${TAG}.getResetPasswordChallengeApiResponseFromHttpResponse")

        val apiResponse = if (response.body.isNullOrBlank()) {
            ResetPasswordChallengeApiResponse(
                response.statusCode,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                EMPTY_RESPONSE_ERROR,
                null,
                EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                null,
                null
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                ResetPasswordChallengeApiResponse::class.java
            )
        }
        apiResponse.statusCode = response.statusCode

        ApiResultUtil.logResponse(TAG, apiResponse)

        return apiResponse
    }

    //region /resetpassword/continue
    /**
     * Converts the HTTP response for /resetpassword/continue API to [ResetPasswordContinueApiResponse] object
     * @param response : HTTP response received from the API
     * @return ResetPasswordContinueApiResponse object
     */
    @Throws(ClientException::class)
    fun getResetPasswordContinueApiResponseFromHttpResponse(
        response: HttpResponse
    ): ResetPasswordContinueApiResponse {
        LogSession.logMethodCall(TAG, "${TAG}.getResetPasswordContinueApiResponseFromHttpResponse")

        val apiResponse = if (response.body.isNullOrBlank()) {
            ResetPasswordContinueApiResponse(
                response.statusCode,
                null,
                null,
                null,
                null,
                EMPTY_RESPONSE_ERROR,
                EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                null,
                null
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                ResetPasswordContinueApiResponse::class.java
            )
        }
        apiResponse.statusCode = response.statusCode

        ApiResultUtil.logResponse(TAG, apiResponse)
        return apiResponse
    }

    //region /resetpassword/submit
    /**
     * Converts the HTTP response for /resetpassword/submit API to [ResetPasswordSubmitApiResponse] object
     * @param response : HTTP response received from the API
     * @return ResetPasswordSubmitApiResponse object
     */
    @Throws(ClientException::class)
    fun getResetPasswordSubmitApiResponseFromHttpResponse(
        response: HttpResponse
    ): ResetPasswordSubmitApiResponse {
        LogSession.logMethodCall(TAG, "${TAG}.getResetPasswordSubmitApiResponseFromHttpResponse")

        val apiResponse = if (response.body.isNullOrBlank()) {
            ResetPasswordSubmitApiResponse(
                response.statusCode,
                null,
                null,
                EMPTY_RESPONSE_ERROR,
                EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                null,
                null,
                null
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                ResetPasswordSubmitApiResponse::class.java
            )
        }
        apiResponse.statusCode = response.statusCode

        ApiResultUtil.logResponse(TAG, apiResponse)

        return apiResponse
    }

    //region /resetpassword/poll_completion
    /**
     * Converts the HTTP response for /resetpassword/poll_completion API to [ResetPasswordPollCompletionApiResponse] object
     * @param response : HTTP response received from the API
     * @return ResetPasswordPollCompletionApiResponse object
     */
    @Throws(ClientException::class)
    fun getResetPasswordPollCompletionApiResponseFromHttpResponse(
        response: HttpResponse
    ): ResetPasswordPollCompletionApiResponse {
        LogSession.logMethodCall(TAG, "${TAG}.getResetPasswordPollCompletionApiResponseFromHttpResponse")

        val apiResponse = if (response.body.isNullOrBlank()) {
            ResetPasswordPollCompletionApiResponse(
                response.statusCode,
                null,
                null,
                EMPTY_RESPONSE_ERROR,
                EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                null,
                null,
                null
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                ResetPasswordPollCompletionApiResponse::class.java
            )
        }
        apiResponse.statusCode = response.statusCode

        ApiResultUtil.logResponse(TAG, apiResponse)

        return apiResponse
    }
}
