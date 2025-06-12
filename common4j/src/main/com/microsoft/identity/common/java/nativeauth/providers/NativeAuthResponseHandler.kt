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
import com.microsoft.identity.common.java.nativeauth.providers.responses.jit.JITChallengeNativeAuthApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.jit.JITContinueNativeAuthApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.jit.JITIntrospectNativeAuthApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordChallengeNativeAuthApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordContinueNativeAuthApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordPollCompletionNativeAuthApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordStartNativeAuthApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordSubmitNativeAuthApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.NativeAuthMicrosoftStsTokenResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInChallengeNativeAuthApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInInitiateNativeAuthApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInIntrospectNativeAuthApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenNativeAuthApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpChallengeNativeAuthApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpContinueNativeAuthApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpStartNativeAuthApiResponse
import com.microsoft.identity.common.java.nativeauth.util.ApiResultUtil
import com.microsoft.identity.common.java.nativeauth.util.isRedirect
import com.microsoft.identity.common.java.net.HttpResponse
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
     * Converts the HTTP response for /signup/start API to [SignUpStartNativeAuthApiResponse] object
     * @param response : HTTP response received from the API
     * @return SignUpStartApiResponse object
     */
    @Throws(ClientException::class)
    fun getSignUpStartResultFromHttpResponse(
        requestCorrelationId: String,
        response: HttpResponse
    ): SignUpStartNativeAuthApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getSignUpStartResultFromHttpResponse"
        )

        val correlationId = retrieveCorrelationId(response, requestCorrelationId)

        val result = if (response.body.isNullOrBlank()) {
            SignUpStartNativeAuthApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorCodes = null,
                continuationToken = null,
                unverifiedAttributes = null,
                invalidAttributes = null,
                challengeType = null,
                redirectReason = null,
                subError = null,
                correlationId = correlationId
            )
        }
        else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                SignUpStartNativeAuthApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

        ApiResultUtil.logResponse(TAG, result)

        return result
    }
    //endregion

    //region signup/challenge
    /**
     * Converts the HTTP response for /signup/challenge API to [SignUpChallengeNativeAuthApiResponse] object
     * @param response : HTTP response received from the API
     * @return SignUpChallengeApiResponse object
     */
    @Throws(ClientException::class)
    fun getSignUpChallengeResultFromHttpResponse(
        requestCorrelationId: String,
        response: HttpResponse
    ): SignUpChallengeNativeAuthApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName ="${TAG}.getSignUpChallengeResultFromHttpResponse"
        )

        val correlationId = retrieveCorrelationId(response, requestCorrelationId)

        val result = if (response.body.isNullOrBlank()) {
            SignUpChallengeNativeAuthApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                continuationToken = null,
                challengeType = null,
                redirectReason = null,
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
                SignUpChallengeNativeAuthApiResponse::class.java
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
     * Converts the HTTP response for /signup/continue API to [SignUpContinueNativeAuthApiResponse] object
     * @param response : HTTP response received from the API
     * @return SignUpContinueApiResponse object
     */
    @Throws(ClientException::class)
    fun getSignUpContinueResultFromHttpResponse(
        requestCorrelationId: String,
        response: HttpResponse
    ): SignUpContinueNativeAuthApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName ="${TAG}.getSignUpContinueResultFromHttpResponse"
        )

        val correlationId = retrieveCorrelationId(response, requestCorrelationId)

        val result = if (response.body.isNullOrBlank()) {
            SignUpContinueNativeAuthApiResponse(
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
                correlationId = correlationId,
                challengeType = null,
                redirectReason = null
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                SignUpContinueNativeAuthApiResponse::class.java
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
    ): SignInInitiateNativeAuthApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getSignInInitiateResultFromHttpResponse"
        )

        val correlationId = retrieveCorrelationId(response, requestCorrelationId)

        val result = if (response.body.isNullOrBlank()) {
            SignInInitiateNativeAuthApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorUri = null,
                errorCodes = null,
                continuationToken = null,
                challengeType = null,
                redirectReason = null,
                correlationId = correlationId
            )
        }  else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                SignInInitiateNativeAuthApiResponse::class.java
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
    ): SignInChallengeNativeAuthApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getSignInChallengeResultFromHttpResponse"
        )

        val correlationId = retrieveCorrelationId(response, requestCorrelationId)

        val result = if (response.body.isNullOrBlank()) {
            SignInChallengeNativeAuthApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorCodes = null,
                errorUri = null,
                subError = null,
                continuationToken = null,
                challengeType = null,
                redirectReason = null,
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
                SignInChallengeNativeAuthApiResponse::class.java
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
    ): SignInIntrospectNativeAuthApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getSignInIntrospectResultFromHttpResponse"
        )

        val correlationId = retrieveCorrelationId(response, requestCorrelationId)

        val result = if (response.body.isNullOrBlank()) {
            SignInIntrospectNativeAuthApiResponse(
                statusCode = response.statusCode,
                challengeType = null,
                redirectReason = null,
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
                SignInIntrospectNativeAuthApiResponse::class.java
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

        val correlationId = retrieveCorrelationId(response, requestCorrelationId)

        // Use native-auth specific class in case of API error response,
        // or standard MicrosoftStsTokenResponse in case of success response
        if (response.statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            val apiResponse = if (response.body.isNullOrBlank()) {
                SignInTokenNativeAuthApiResponse(
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
                    SignInTokenNativeAuthApiResponse::class.java
                )
            }
            apiResponse.statusCode = response.statusCode
            apiResponse.correlationId = correlationId

            ApiResultUtil.logResponse(TAG, apiResponse)
            return apiResponse.toErrorResult()
        } else {
            val apiResponse = ObjectMapper.deserializeJsonStringToObject(
                response.body,
                NativeAuthMicrosoftStsTokenResponse::class.java  // Extended class from MicrosoftStsTokenResponse
            )
            return if (apiResponse.challengeType.isRedirect()) {
                SignInTokenApiResult.Redirect(
                    correlationId = correlationId,
                    redirectReason = apiResponse.redirectReason.orEmpty()
                )
            } else {
                SignInTokenApiResult.Success(
                    tokenResponse = apiResponse,
                    correlationId = correlationId
                )
            }
        }
    }
    //endregion

    //region /resetpassword/start
    /**
     * Converts the HTTP response for /resetpassword/start API to [ResetPasswordStartNativeAuthApiResponse] object
     * @param response : HTTP response received from the API
     * @return ResetPasswordStartApiResponse object
     */
    @Throws(ClientException::class)
    fun getResetPasswordStartApiResponseFromHttpResponse(
        requestCorrelationId: String,
        response: HttpResponse
    ): ResetPasswordStartNativeAuthApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getResetPasswordStartApiResponseFromHttpResponse"
        )

        val correlationId = retrieveCorrelationId(response, requestCorrelationId)

        val result = if (response.body.isNullOrBlank()) {
            ResetPasswordStartNativeAuthApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorUri = null,
                continuationToken = null,
                challengeType = null,
                redirectReason = null,
                correlationId = correlationId
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                ResetPasswordStartNativeAuthApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

        ApiResultUtil.logResponse(TAG, result)

        return result
    }
    //endregion

    //region /resetpassword/challenge
    /**
     * Converts the HTTP response for /resetpassword/challenge API to [ResetPasswordChallengeNativeAuthApiResponse] object
     * @param response : HTTP response received from the API
     * @return ResetPasswordChallengeApiResponse object
     */
    @Throws(ClientException::class)
    fun getResetPasswordChallengeApiResponseFromHttpResponse(
        requestCorrelationId: String,
        response: HttpResponse
    ): ResetPasswordChallengeNativeAuthApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getResetPasswordChallengeApiResponseFromHttpResponse"
        )

        val correlationId = retrieveCorrelationId(response, requestCorrelationId)

        val result = if (response.body.isNullOrBlank()) {
            ResetPasswordChallengeNativeAuthApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorUri = null,
                continuationToken = null,
                challengeType = null,
                redirectReason = null,
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
                ResetPasswordChallengeNativeAuthApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

        ApiResultUtil.logResponse(TAG, result)

        return result
    }
    //endregion

    //region /resetpassword/continue
    /**
     * Converts the HTTP response for /resetpassword/continue API to [ResetPasswordContinueNativeAuthApiResponse] object
     * @param response : HTTP response received from the API
     * @return ResetPasswordContinueApiResponse object
     */
    @Throws(ClientException::class)
    fun getResetPasswordContinueApiResponseFromHttpResponse(
        requestCorrelationId: String,
        response: HttpResponse
    ): ResetPasswordContinueNativeAuthApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getResetPasswordContinueApiResponseFromHttpResponse"
        )

        val correlationId = retrieveCorrelationId(response, requestCorrelationId)

        val result = if (response.body.isNullOrBlank()) {
            ResetPasswordContinueNativeAuthApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription =  EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorUri = null,
                continuationToken = null,
                challengeType = null,
                redirectReason = null,
                expiresIn = null,
                subError = null,
                correlationId = correlationId
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                ResetPasswordContinueNativeAuthApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

        ApiResultUtil.logResponse(TAG, result)
        return result
    }
    //endregion

    //region /resetpassword/submit
    /**
     * Converts the HTTP response for /resetpassword/submit API to [ResetPasswordSubmitNativeAuthApiResponse] object
     * @param response : HTTP response received from the API
     * @return ResetPasswordSubmitApiResponse object
     */
    @Throws(ClientException::class)
    fun getResetPasswordSubmitApiResponseFromHttpResponse(
        requestCorrelationId: String,
        response: HttpResponse
    ): ResetPasswordSubmitNativeAuthApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getResetPasswordSubmitApiResponseFromHttpResponse"
        )

        val correlationId = retrieveCorrelationId(response, requestCorrelationId)

        val result = if (response.body.isNullOrBlank()) {
            ResetPasswordSubmitNativeAuthApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorUri = null,
                continuationToken = null,
                pollInterval = null,
                subError = null,
                correlationId = correlationId,
                challengeType = null,
                redirectReason = null
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                ResetPasswordSubmitNativeAuthApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

        ApiResultUtil.logResponse(TAG, result)

        return result
    }
    //endregion

    //region /resetpassword/poll_completion
    /**
     * Converts the HTTP response for /resetpassword/poll_completion API to [ResetPasswordPollCompletionNativeAuthApiResponse] object
     * @param response : HTTP response received from the API
     * @return ResetPasswordPollCompletionApiResponse object
     */
    @Throws(ClientException::class)
    fun getResetPasswordPollCompletionApiResponseFromHttpResponse(
        requestCorrelationId: String,
        response: HttpResponse
    ): ResetPasswordPollCompletionNativeAuthApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getResetPasswordPollCompletionApiResponseFromHttpResponse"
        )

        val correlationId = retrieveCorrelationId(response, requestCorrelationId)

        val result = if (response.body.isNullOrBlank()) {
            ResetPasswordPollCompletionNativeAuthApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorUri = null,
                status = null,
                continuationToken = null,
                expiresIn = null,
                subError = null,
                correlationId = correlationId,
                challengeType = null,
                redirectReason = null
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                ResetPasswordPollCompletionNativeAuthApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

        ApiResultUtil.logResponse(TAG, result)

        return result
    }
    //endregion

    //region /register/introspect
    /**
     * Converts the HTTP response for /register/introspect API to [JITIntrospectResponse] object
     * @param response : HTTP response received from the API
     * @return JITIntrospectApiResponse object
     */
    @Throws(ClientException::class)
    fun getJITIntrospectApiResponseFromHttpResponse(
        requestCorrelationId: String,
        response: HttpResponse
    ): JITIntrospectNativeAuthApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getJITIntrospectApiResponseFromHttpResponse"
        )

        val correlationId = retrieveCorrelationId(response, requestCorrelationId)

        val result = if (response.body.isNullOrBlank()) {
            JITIntrospectNativeAuthApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorUri = null,
                continuationToken = null,
                correlationId = correlationId,
                challengeType = null,
                redirectReason = null,
                methods = null,
                errorCodes = null
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                JITIntrospectNativeAuthApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

        ApiResultUtil.logResponse(TAG, result)

        return result
    }
    //endregion

    //region /register/challenge
    /**
     * Converts the HTTP response for /register/challenge API to [JITChallengeResponse] object
     * @param response : HTTP response received from the API
     * @return JITChallengeApiResponse object
     */
    @Throws(ClientException::class)
    fun getJITChallengeApiResponseFromHttpResponse(
        requestCorrelationId: String,
        response: HttpResponse
    ): JITChallengeNativeAuthApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getJITChallengeApiResponseFromHttpResponse"
        )

        val correlationId = retrieveCorrelationId(response, requestCorrelationId)

        val result = if (response.body.isNullOrBlank()) {
            JITChallengeNativeAuthApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                errorUri = null,
                continuationToken = null,
                challengeType = null,
                redirectReason = null,
                bindingMethod = null,
                challengeTarget = null,
                challengeChannel = null,
                codeLength = null,
                interval = null,
                errorCodes = null,
                correlationId = correlationId
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                JITChallengeNativeAuthApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

        ApiResultUtil.logResponse(TAG, result)

        return result
    }
    //endregion

    //region /register/continue
    /**
     * Converts the HTTP response for /register/continue API to [JITContinueNativeAuthApiResponse] object
     * @param response : HTTP response received from the API
     * @return JITContinueApiResponse object
     */
    @Throws(ClientException::class)
    fun getJITContinueApiResponseFromHttpResponse(
        requestCorrelationId: String,
        response: HttpResponse
    ): JITContinueNativeAuthApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getJITContinueApiResponseFromHttpResponse"
        )

        val correlationId = retrieveCorrelationId(response, requestCorrelationId)

        val result = if (response.body.isNullOrBlank()) {
            JITContinueNativeAuthApiResponse(
                statusCode = response.statusCode,
                error = EMPTY_RESPONSE_ERROR,
                errorDescription = EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION,
                continuationToken = null,
                subError = null,
                errorCodes = null,
                correlationId = correlationId,
                challengeType = null,
                redirectReason = null
            )
        } else {
            ObjectMapper.deserializeJsonStringToObject(
                response.body,
                JITContinueNativeAuthApiResponse::class.java
            )
        }
        result.statusCode = response.statusCode
        result.correlationId = correlationId

        ApiResultUtil.logResponse(TAG, result)

        return result
    }
    //endregion

    /**
     * If the API doesn't return a correlation ID header value, use the correlation ID of the original API request
     */
    private fun retrieveCorrelationId(
        response: HttpResponse,
        requestCorrelationId: String
    ): String {
        return response.getHeaderValue(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, 0).let {responseCorrelationId ->
            if (responseCorrelationId.isNullOrBlank()) {
                requestCorrelationId
            } else {
                responseCorrelationId
            }
        }
    }
}
