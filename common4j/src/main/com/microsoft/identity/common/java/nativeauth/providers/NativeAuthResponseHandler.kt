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
package com.microsoft.identity.common.java.nativeauth.providers

import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.net.HttpResponse
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordChallengeApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordContinueApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordPollCompletionApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordStartApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordSubmitApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInChallengeApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInInitiateApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInIntrospectApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInIntrospectApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpChallengeApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpContinueApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpStartApiResponse
import com.microsoft.identity.common.java.nativeauth.util.ApiResultUtil
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
        requestCorrelationId: String,
        response: HttpResponse
    ): SignUpStartApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getSignUpStartResultFromHttpResponse"
        )

        // If the API doesn't return a correlation ID header value, use the correlation ID of the original API request
        val correlationId: String = response.getHeaderValue(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, 0).let {responseCorrelationId ->
            if (responseCorrelationId.isNullOrBlank()) {
                requestCorrelationId
            } else {
                responseCorrelationId
            }
        }

        val result = if (response.body.isNullOrBlank()) {
            SignUpStartApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorCodes = null,
                continuationToken = null,
                unverifiedAttributes = null,
                invalidAttributes = null,
                challengeType = null,
                subError = null,
                correlationId = correlationId
            )
        }
        else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                SignUpStartApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

        // TODO add correlation ID
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
        requestCorrelationId: String,
        response: HttpResponse
    ): SignUpChallengeApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName ="${TAG}.getSignUpChallengeResultFromHttpResponse"
        )

        // If the API doesn't return a correlation ID header value, use the correlation ID of the original API request
        val correlationId: String = response.getHeaderValue(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, 0).let {responseCorrelationId ->
            if (responseCorrelationId.isNullOrBlank()) {
                requestCorrelationId
            } else {
                responseCorrelationId
            }
        }

        val result = if (response.body.isNullOrBlank()) {
            SignUpChallengeApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                continuationToken = null,
                challengeType = null,
                challengeTargetLabel = null,
                codeLength = null,
                bindingMethod = null,
                interval = null,
                challengeChannel = null,
                correlationId = correlationId
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                SignUpChallengeApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

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
        requestCorrelationId: String,
        response: HttpResponse
    ): SignUpContinueApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName ="${TAG}.getSignUpContinueResultFromHttpResponse"
        )

        // If the API doesn't return a correlation ID header value, use the correlation ID of the original API request
        val correlationId: String = response.getHeaderValue(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, 0).let {responseCorrelationId ->
            if (responseCorrelationId.isNullOrBlank()) {
                requestCorrelationId
            } else {
                responseCorrelationId
            }
        }

        val result = if (response.body.isNullOrBlank()) {
            SignUpContinueApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorCodes = null,
                continuationToken = null,
                invalidAttributes = null,
                unverifiedAttributes = null,
                requiredAttributes = null,
                expiresIn = null,
                subError = null,
                correlationId = correlationId
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                SignUpContinueApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

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
        requestCorrelationId: String,
        response: HttpResponse
    ): SignInInitiateApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getSignInInitiateResultFromHttpResponse"
        )

        // If the API doesn't return a correlation ID header value, use the correlation ID of the original API request
        val correlationId: String = response.getHeaderValue(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, 0).let {responseCorrelationId ->
            if (responseCorrelationId.isNullOrBlank()) {
                requestCorrelationId
            } else {
                responseCorrelationId
            }
        }

        val result = if (response.body.isNullOrBlank()) {
            SignInInitiateApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorUri = null,
                errorCodes = null,
                continuationToken = null,
                challengeType = null,
                correlationId = correlationId
            )
        }  else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                SignInInitiateApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

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
        requestCorrelationId: String,
        response: HttpResponse
    ): SignInChallengeApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getSignInChallengeResultFromHttpResponse"
        )

        // If the API doesn't return a correlation ID header value, use the correlation ID of the original API request
        val correlationId: String = response.getHeaderValue(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, 0).let {responseCorrelationId ->
            if (responseCorrelationId.isNullOrBlank()) {
                requestCorrelationId
            } else {
                responseCorrelationId
            }
        }

        val result = if (response.body.isNullOrBlank()) {
            SignInChallengeApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorCodes = null,
                errorUri = null,
                subError = null,
                continuationToken = null,
                challengeType = null,
                bindingMethod = null,
                challengeTargetLabel = null,
                challengeChannel = null,
                codeLength = null,
                interval = null,
                correlationId = correlationId
            )

        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                SignInChallengeApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

        ApiResultUtil.logResponse(TAG, result)

        return result
    }
    //endregion

    //region /oauth/v2.0/introspect
    /**
     * Converts the response for /oauth/v2.0/introspect REST API to Java object
     * @param response HTTP response received from REST API
     */
    @Throws(ClientException::class)
    fun getSignInIntrospectResultFromHttpResponse(
        requestCorrelationId: String,
        response: HttpResponse
    ): SignInIntrospectApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getSignInIntrospectResultFromHttpResponse"
        )

        // If the API doesn't return a correlation ID header value, use the correlation ID of the original API request
        val correlationId: String = response.getHeaderValue(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, 0).let {responseCorrelationId ->
            if (responseCorrelationId.isNullOrBlank()) {
                requestCorrelationId
            } else {
                responseCorrelationId
            }
        }

        val result = if (response.body.isNullOrBlank()) {
            SignInIntrospectApiResponse(
                statusCode = response.statusCode,
                challengeType = null,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorCodes = null,
                continuationToken = null,
                methods = null,
                correlationId = correlationId,
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                SignInIntrospectApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

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
        requestCorrelationId: String,
        response: HttpResponse
    ): SignInTokenApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getSignInTokenApiResultFromHttpResponse"
        )

        // If the API doesn't return a correlation ID header value, use the correlation ID of the original API request
        val correlationId: String = response.getHeaderValue(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, 0).let {responseCorrelationId ->
            if (responseCorrelationId.isNullOrBlank()) {
                requestCorrelationId
            } else {
                responseCorrelationId
            }
        }

        // Use native-auth specific class in case of API error response,
        // or standard MicrosoftStsTokenResponse in case of success response
        if (response.statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            val apiResponse = if (response.body.isNullOrBlank()) {
                SignInTokenApiResponse(
                    statusCode = response.statusCode,
                    error = null,
                    errorDescription = null,
                    errorUri = null,
                    errorCodes = null,
                    subError = null,
                    continuationToken = null,
                    correlationId = correlationId
                )
            } else {
                ObjectMapper.deserializeJsonStringToObject(
                    response.body,
                    SignInTokenApiResponse::class.java
                )
            }
            apiResponse.statusCode = response.statusCode
            apiResponse.correlationId = correlationId

            ApiResultUtil.logResponse(TAG, apiResponse)
            return apiResponse.toErrorResult()
        } else {
            val apiResponse = ObjectMapper.deserializeJsonStringToObject(
                response.body,
                MicrosoftStsTokenResponse::class.java
            )
            return SignInTokenApiResult.Success(
                tokenResponse = apiResponse,
                correlationId = correlationId
            )
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
        requestCorrelationId: String,
        response: HttpResponse
    ): ResetPasswordStartApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getResetPasswordStartApiResponseFromHttpResponse"
        )

        // If the API doesn't return a correlation ID header value, use the correlation ID of the original API request
        val correlationId: String = response.getHeaderValue(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, 0).let {responseCorrelationId ->
            if (responseCorrelationId.isNullOrBlank()) {
                requestCorrelationId
            } else {
                responseCorrelationId
            }
        }

        val result = if (response.body.isNullOrBlank()) {
            ResetPasswordStartApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorUri = null,
                continuationToken = null,
                challengeType = null,
                correlationId = correlationId
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                ResetPasswordStartApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

        ApiResultUtil.logResponse(TAG, result)

        return result
    }

    //region /resetpassword/challenge
    /**
     * Converts the HTTP response for /resetpassword/challenge API to [ResetPasswordChallengeApiResponse] object
     * @param response : HTTP response received from the API
     * @return ResetPasswordChallengeApiResponse object
     */
    @Throws(ClientException::class)
    fun getResetPasswordChallengeApiResponseFromHttpResponse(
        requestCorrelationId: String,
        response: HttpResponse
    ): ResetPasswordChallengeApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getResetPasswordChallengeApiResponseFromHttpResponse"
        )

        // If the API doesn't return a correlation ID header value, use the correlation ID of the original API request
        val correlationId: String = response.getHeaderValue(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, 0).let {responseCorrelationId ->
            if (responseCorrelationId.isNullOrBlank()) {
                requestCorrelationId
            } else {
                responseCorrelationId
            }
        }

        val result = if (response.body.isNullOrBlank()) {
            ResetPasswordChallengeApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorUri = null,
                continuationToken = null,
                challengeType = null,
                bindingMethod = null,
                challengeTargetLabel = null,
                challengeChannel = null,
                codeLength = null,
                interval = null,
                correlationId = correlationId
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                ResetPasswordChallengeApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

        ApiResultUtil.logResponse(TAG, result)

        return result
    }

    //region /resetpassword/continue
    /**
     * Converts the HTTP response for /resetpassword/continue API to [ResetPasswordContinueApiResponse] object
     * @param response : HTTP response received from the API
     * @return ResetPasswordContinueApiResponse object
     */
    @Throws(ClientException::class)
    fun getResetPasswordContinueApiResponseFromHttpResponse(
        requestCorrelationId: String,
        response: HttpResponse
    ): ResetPasswordContinueApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getResetPasswordContinueApiResponseFromHttpResponse"
        )

        // If the API doesn't return a correlation ID header value, use the correlation ID of the original API request
        val correlationId: String = response.getHeaderValue(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, 0).let {responseCorrelationId ->
            if (responseCorrelationId.isNullOrBlank()) {
                requestCorrelationId
            } else {
                responseCorrelationId
            }
        }

        val result = if (response.body.isNullOrBlank()) {
            ResetPasswordContinueApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription =  EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorUri = null,
                continuationToken = null,
                challengeType = null,
                expiresIn = null,
                subError = null,
                correlationId = correlationId
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                ResetPasswordContinueApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

        ApiResultUtil.logResponse(TAG, result)
        return result
    }

    //region /resetpassword/submit
    /**
     * Converts the HTTP response for /resetpassword/submit API to [ResetPasswordSubmitApiResponse] object
     * @param response : HTTP response received from the API
     * @return ResetPasswordSubmitApiResponse object
     */
    @Throws(ClientException::class)
    fun getResetPasswordSubmitApiResponseFromHttpResponse(
        requestCorrelationId: String,
        response: HttpResponse
    ): ResetPasswordSubmitApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getResetPasswordSubmitApiResponseFromHttpResponse"
        )

        // If the API doesn't return a correlation ID header value, use the correlation ID of the original API request
        val correlationId: String = response.getHeaderValue(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, 0).let {responseCorrelationId ->
            if (responseCorrelationId.isNullOrBlank()) {
                requestCorrelationId
            } else {
                responseCorrelationId
            }
        }

        val result = if (response.body.isNullOrBlank()) {
            ResetPasswordSubmitApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorUri = null,
                continuationToken = null,
                pollInterval = null,
                subError = null,
                correlationId = correlationId
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                ResetPasswordSubmitApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

        ApiResultUtil.logResponse(TAG, result)

        return result
    }

    //region /resetpassword/poll_completion
    /**
     * Converts the HTTP response for /resetpassword/poll_completion API to [ResetPasswordPollCompletionApiResponse] object
     * @param response : HTTP response received from the API
     * @return ResetPasswordPollCompletionApiResponse object
     */
    @Throws(ClientException::class)
    fun getResetPasswordPollCompletionApiResponseFromHttpResponse(
        requestCorrelationId: String,
        response: HttpResponse
    ): ResetPasswordPollCompletionApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getResetPasswordPollCompletionApiResponseFromHttpResponse"
        )

        // If the API doesn't return a correlation ID header value, use the correlation ID of the original API request
        val correlationId: String = response.getHeaderValue(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, 0).let {responseCorrelationId ->
            if (responseCorrelationId.isNullOrBlank()) {
                requestCorrelationId
            } else {
                responseCorrelationId
            }
        }

        val result = if (response.body.isNullOrBlank()) {
            ResetPasswordPollCompletionApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorUri = null,
                status = null,
                continuationToken = null,
                expiresIn = null,
                subError = null,
                correlationId = correlationId
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                ResetPasswordPollCompletionApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

        ApiResultUtil.logResponse(TAG, result)

        return result
    }
}
