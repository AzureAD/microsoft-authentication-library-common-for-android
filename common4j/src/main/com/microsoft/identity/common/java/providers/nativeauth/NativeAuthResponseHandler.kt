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
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.net.HttpResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInChallengeApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInInitiateApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInTokenApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.challenge.SignUpChallengeErrorResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.challenge.SignUpChallengeResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.challenge.SignUpChallengeResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.cont.SignUpContinueErrorResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.cont.SignUpContinueResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.cont.SignUpContinueResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.start.SignUpStartErrorResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.start.SignUpStartResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.start.SignUpStartResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprChallengeApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprContinueApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprPollCompletionApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprStartApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprSubmitApiResponse
import com.microsoft.identity.common.java.util.ApiResultUtil
import com.microsoft.identity.common.java.util.ObjectMapper
import com.microsoft.identity.common.java.util.ResultUtil
import java.net.HttpURLConnection

class NativeAuthResponseHandler {
    private val TAG = NativeAuthResponseHandler::class.java.simpleName

    //region signup/start
    @Throws(ClientException::class)
    internal fun getSignUpStartResultFromHttpResponse(
        response: HttpResponse
    ): SignUpStartResult {
        val methodName = ":getSignUpStartResponseFromHttpResponse"
        Logger.verbose(
            TAG + methodName,
            "Creating SignUpStartRequest from HttpResponse..."
        )

        val result = if (response.statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            val errorResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromUnsuccessfulResponse(response.body),
                SignUpStartErrorResponse::class.java
            )
            errorResponse.statusCode = response.statusCode
            SignUpStartResult.createError(errorResponse)
        } else {
            val successResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromSuccessfulResponse(response.body),
                SignUpStartResponse::class.java
            )
            SignUpStartResult.createSuccess(successResponse)
        }
        ResultUtil.logResult(TAG, result)

        // TODO manage headers for telemetry
        return result
    }
    //endregion

    //region signup/challenge
    @Throws(ClientException::class)
    internal fun getSignUpChallengeResultFromHttpResponse(
        response: HttpResponse
    ): SignUpChallengeResult {
        val methodName = ":getSignUpChallengeResultFromHttpResponse"
        Logger.verbose(
            TAG + methodName,
            "Creating SignUpChallengeResult from HttpResponse..."
        )

        val result = if (response.statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            val errorResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromUnsuccessfulResponse(response.body),
                SignUpChallengeErrorResponse::class.java
            )
            errorResponse.statusCode = response.statusCode
            SignUpChallengeResult.createError(errorResponse)
        } else {
            val successResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromSuccessfulResponse(response.body),
                SignUpChallengeResponse::class.java
            )
            SignUpChallengeResult.createSuccess(successResponse)
        }
        ResultUtil.logResult(TAG, result)

        // TODO manage headers for telemetry

        return result
    }
    //endregion

    //region /oauth/v2.0/initiate
    @Throws(ClientException::class)
    internal fun getSignInInitiateResultFromHttpResponse(
        response: HttpResponse
    ): SignInInitiateApiResponse {
        val methodName = ":getSignInInitiateResultFromHttpResponse"
        Logger.verbose(
            TAG + methodName,
            "Creating SignInInitiateResult from HttpResponse..."
        )

        val result = ObjectMapper.deserializeJsonStringToObject(
            response.body,
            SignInInitiateApiResponse::class.java
        )
        result.statusCode = response.statusCode

        ApiResultUtil.logResponse(TAG, result)

        // TODO manage headers for telemetry
        return result
    }
    //endregion

    //region /oauth/v2.0/challenge
    @Throws(ClientException::class)
    internal fun getSignInChallengeResultFromHttpResponse(
        response: HttpResponse
    ): SignInChallengeApiResponse {
        val methodName = ":getSignInChallengeResultFromHttpResponse"
        Logger.verbose(
            TAG + methodName,
            "Creating SignInChallengeResult from HttpResponse..."
        )

        val result = ObjectMapper.deserializeJsonStringToObject(
            response.body,
            SignInChallengeApiResponse::class.java
        )
        result.statusCode = response.statusCode

        ApiResultUtil.logResponse(TAG, result)

        // TODO manage headers for telemetry

        return result
    }
    //endregion

    //region /oauth/v2.0/token
    @Throws(ClientException::class)
    internal fun getSignInTokenResultFromHttpResponse(
        response: HttpResponse
    ): SignInTokenApiResponse {
        val methodName = ":getSignInTokenResultFromHttpResponse"
        Logger.verbose(
            TAG + methodName,
            "Creating SignInTokenResult from HttpResponse..."
        )

        val result = ObjectMapper.deserializeJsonStringToObject(
            response.body,
            SignInTokenApiResponse::class.java
        )
        result.statusCode = response.statusCode

        ApiResultUtil.logResponse(TAG, result)

        // TODO manage headers for telemetry

        return result
    }
    //endregion

    //region /signup/continue
    @Throws(ClientException::class)
    internal fun getSignUpContinueResultFromHttpResponse(
        response: HttpResponse
    ): SignUpContinueResult {
        val methodName = ":getSignUpContinueResultFromHttpResponse"
        Logger.verbose(
            TAG + methodName,
            "Getting SignUpContinueResult from HttpResponse..."
        )

        val result = if (response.statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            // An error occurred
            val errorResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromUnsuccessfulResponse(response.body),
                SignUpContinueErrorResponse::class.java
            )
            errorResponse.statusCode = response.statusCode
            SignUpContinueResult.createError(errorResponse)
        } else {
            val successResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromSuccessfulResponse(response.body),
                SignUpContinueResponse::class.java
            )
            SignUpContinueResult.createSuccess(successResponse)
        }
        ResultUtil.logResult(TAG, result)

        // TODO manage headers for telemetry

        return result
    }

    //region /resetpassword/start
    @Throws(ClientException::class)
    internal fun getSsprStartApiResponseFromHttpResponse(
        response: HttpResponse
    ): SsprStartApiResponse {
        val methodName = ":getSsprStartApiResponseFromHttpResponse"
        Logger.verbose(
            TAG + methodName,
            "Creating SsprStartApiResponse from HttpResponse..."
        )

        val apiResponse = ObjectMapper.deserializeJsonStringToObject(
            response.body,
            SsprStartApiResponse::class.java
        )
        apiResponse.statusCode = response.statusCode

        ApiResultUtil.logResponse(TAG, apiResponse)

        // TODO manage headers for telemetry
        return apiResponse
    }

    //region /resetpassword/challenge
    @Throws(ClientException::class)
    internal fun getSsprChallengeApiResponseFromHttpResponse(
        response: HttpResponse
    ): SsprChallengeApiResponse {
        val methodName = ":getSsprChallengeApiResponseFromHttpResponse"
        Logger.verbose(
            TAG + methodName,
            "Creating SsprChallengeApiResponse from HttpResponse..."
        )

        val apiResponse = ObjectMapper.deserializeJsonStringToObject(
            response.body,
            SsprChallengeApiResponse::class.java
        )
        apiResponse.statusCode = response.statusCode

        ApiResultUtil.logResponse(TAG, apiResponse)

        // TODO manage headers for telemetry
        return apiResponse
    }

    //region /resetpassword/continue
    @Throws(ClientException::class)
    internal fun getSsprContinueApiResponseFromHttpResponse(
        response: HttpResponse
    ): SsprContinueApiResponse {
        val methodName = ":getSsprContinueApiResponseFromHttpResponse"
        Logger.verbose(
            TAG + methodName,
            "Creating SsprContinueApiResponse from HttpResponse..."
        )

        val apiResponse = ObjectMapper.deserializeJsonStringToObject(
            response.body,
            SsprContinueApiResponse::class.java
        )
        apiResponse.statusCode = response.statusCode

        ApiResultUtil.logResponse(TAG, apiResponse)

        // TODO manage headers for telemetry
        return apiResponse
    }

    //region /resetpassword/submit
    @Throws(ClientException::class)
    internal fun getSsprSubmitApiResponseFromHttpResponse(
        response: HttpResponse
    ): SsprSubmitApiResponse {
        val methodName = ":getSsprSubmitApiResponseFromHttpResponse"
        Logger.verbose(
            TAG + methodName,
            "Creating SsprSubmitApiResponse from HttpResponse..."
        )

        val apiResponse = ObjectMapper.deserializeJsonStringToObject(
            response.body,
            SsprSubmitApiResponse::class.java
        )
        apiResponse.statusCode = response.statusCode

        ApiResultUtil.logResponse(TAG, apiResponse)

        // TODO manage headers for telemetry
        return apiResponse
    }

    //region /resetpassword/poll_completion
    @Throws(ClientException::class)
    internal fun getSsprPollCompletionApiResponseFromHttpResponse(
        response: HttpResponse
    ): SsprPollCompletionApiResponse {
        val methodName = ":getSsprPollCompletionApiResponseFromHttpResponse"
        Logger.verbose(
            TAG + methodName,
            "Creating SsprPollCompletionApiResponse from HttpResponse..."
        )

        val apiResponse = ObjectMapper.deserializeJsonStringToObject(
            response.body,
            SsprPollCompletionApiResponse::class.java
        )
        apiResponse.statusCode = response.statusCode

        ApiResultUtil.logResponse(TAG, apiResponse)

        // TODO manage headers for telemetry
        return apiResponse
    }

    /**
     * Generic function that validates the API result. Will throw an exception if the validation
     * of required API response fields fails. Will log a warning if the validation of optional
     * API response fields fails.
     */
     fun validateApiResult(apiResult: IApiResult) {
        val methodName = ":validateApiResult"
        Logger.verbose(
            TAG + methodName,
            "Validating API result..."
        )
        if (apiResult.success) {
            validateSuccessfulResponse(
                apiResult.successResponse
            )
        } else {
            validateUnsuccessfulResponse(
                apiResult.errorResponse
            )
        }
    }

    private fun validateSuccessfulResponse(successResponse: IApiSuccessResponse?) {
        try {
            if (successResponse == null) {
                throw ClientException("SuccessResponse can't be null in success state")
            }
            successResponse.validateRequiredFields()
            successResponse.validateOptionalFields()
        } catch (e: ClientException) {
            Logger.error(TAG, e.message, e)
            throw e
        }
    }

    private fun validateUnsuccessfulResponse(errorResponse: IApiErrorResponse?) {
//        try {
        if (errorResponse == null) {
            throw ClientException("ErrorResponse can't be null in error state")
        }
        errorResponse.validateRequiredFields()
        errorResponse.validateOptionalFields()
//        } catch (e: ClientException) {
//            Logger.error(TAG, e.message, e)
//            throw e
//        }
    }

    @Throws(ClientException::class)
    private fun getBodyFromSuccessfulResponse(responseBody: String): String {
        return responseBody
    }

    @Throws(ClientException::class)
    private fun getBodyFromUnsuccessfulResponse(responseBody: String): String {
        val EMPTY_JSON_OBJECT = "{}"
        return responseBody.ifEmpty { EMPTY_JSON_OBJECT }
    }
}
