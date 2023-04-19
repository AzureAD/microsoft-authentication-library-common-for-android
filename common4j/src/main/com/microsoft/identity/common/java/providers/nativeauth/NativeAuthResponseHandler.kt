package com.microsoft.identity.common.java.providers.nativeauth

import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInChallengeErrorResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInChallengeResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInChallengeSuccessResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInInitiateErrorResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInInitiateResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInInitiateSuccessResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInTokenErrorResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInTokenResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInTokenSuccessResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.challenge.SignUpChallengeErrorResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.challenge.SignUpChallengeResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.challenge.SignUpChallengeResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.cont.SignUpContinueErrorResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.cont.SignUpContinueResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.cont.SignUpContinueResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.start.SignUpStartErrorResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.start.SignUpStartResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.start.SignUpStartResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.challenge.SsprChallengeErrorResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.challenge.SsprChallengeResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.challenge.SsprChallengeResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.cont.SsprContinueErrorResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.cont.SsprContinueResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.cont.SsprContinueResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.pollcompletion.SsprPollCompletionErrorResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.pollcompletion.SsprPollCompletionResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.pollcompletion.SsprPollCompletionResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.start.SsprStartErrorResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.start.SsprStartResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.start.SsprStartResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.submit.SsprSubmitErrorResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.submit.SsprSubmitResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.submit.SsprSubmitResult
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.net.HttpResponse
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
            "Getting SignUpStartRequest from HttpResponse..."
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
            "Getting SignUpChallengeResult from HttpResponse..."
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
    internal fun getSsprStartResultFromHttpResponse(
        response: HttpResponse
    ): SsprStartResult {
        val methodName = ":getSsprStartResultFromHttpResponse"
        Logger.verbose(
            TAG + methodName,
            "Getting SsprStartResult from HttpResponse..."
        )

        val result = if (response.statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            val errorResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromUnsuccessfulResponse(response.body),
                SsprStartErrorResponse::class.java
            )
            errorResponse.statusCode = response.statusCode
            SsprStartResult.createError(errorResponse)
        } else {
            val successResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromSuccessfulResponse(response.body),
                SsprStartResponse::class.java
            )
            SsprStartResult.createSuccess(successResponse)
        }
        ResultUtil.logResult(TAG, result)
        return result
    }

    //region /resetpassword/challenge
    @Throws(ClientException::class)
    internal fun getSsprChallengeResultFromHttpResponse(
        response: HttpResponse
    ): SsprChallengeResult {
        val methodName = ":getSsprChallengeResultFromHttpResponse"
        Logger.verbose(
            TAG + methodName,
            "Getting SsprChallengeResult from HttpResponse..."
        )

        val result = if (response.statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            val errorResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromUnsuccessfulResponse(response.body),
                SsprChallengeErrorResponse::class.java
            )
            errorResponse.statusCode = response.statusCode
            SsprChallengeResult.createError(errorResponse)
        } else {
            val successResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromSuccessfulResponse(response.body),
                SsprChallengeResponse::class.java
            )
            SsprChallengeResult.createSuccess(successResponse)
        }
        ResultUtil.logResult(TAG, result)

        return result
    }

    //region /resetpassword/continue
    @Throws(ClientException::class)
    internal fun getSsprContinueResultFromHttpResponse(
        response: HttpResponse
    ): SsprContinueResult {
        val methodName = ":getSsprContinueResultFromHttpResponse"
        Logger.verbose(
            TAG + methodName,
            "Getting SsprContinueResult from HttpResponse..."
        )

        val result = if (response.statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            val errorResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromUnsuccessfulResponse(response.body),
                SsprContinueErrorResponse::class.java
            )
            errorResponse.statusCode = response.statusCode
            SsprContinueResult.createError(errorResponse)
        } else {
            val successResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromSuccessfulResponse(response.body),
                SsprContinueResponse::class.java
            )
            SsprContinueResult.createSuccess(successResponse)
        }
        ResultUtil.logResult(TAG, result)

        return result
    }

    //region /resetpassword/submit
    @Throws(ClientException::class)
    internal fun getSsprSubmitResultFromHttpResponse(
        response: HttpResponse
    ): SsprSubmitResult {
        val methodName = ":getSsprSubmitResultFromHttpResponse"
        Logger.verbose(
            TAG + methodName,
            "Getting SsprSubmitResult from HttpResponse..."
        )

        val result = if (response.statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            val errorResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromUnsuccessfulResponse(response.body),
                SsprSubmitErrorResponse::class.java
            )
            errorResponse.statusCode = response.statusCode
            SsprSubmitResult.createError(errorResponse)
        } else {
            val successResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromSuccessfulResponse(response.body),
                SsprSubmitResponse::class.java
            )
            SsprSubmitResult.createSuccess(successResponse)
        }
        ResultUtil.logResult(TAG, result)

        return result
    }

    //region /resetpassword/poll_completion
    @Throws(ClientException::class)
    internal fun getSsprPollCompletionResultFromHttpResponse(
        response: HttpResponse
    ): SsprPollCompletionResult {
        val methodName = ":getSsprPollCompletionResultFromHttpResponse"
        Logger.verbose(
            TAG + methodName,
            "Getting SsprPollCompletionResult from HttpResponse..."
        )

        val result = if (response.statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            val errorResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromUnsuccessfulResponse(response.body),
                SsprPollCompletionErrorResponse::class.java
            )
            errorResponse.statusCode = response.statusCode
            SsprPollCompletionResult.createError(errorResponse)
        } else {
            val successResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromSuccessfulResponse(response.body),
                SsprPollCompletionResponse::class.java
            )
            SsprPollCompletionResult.createSuccess(successResponse)
        }
        ResultUtil.logResult(TAG, result)

        return result
    }

    //region /oauth/v2.0/initiate
    @Throws(ClientException::class)
    internal fun getSignInInitiateResultFromHttpResponse(
        response: HttpResponse
    ): SignInInitiateResult {
        val methodName = ":getSignInInitiateResultFromHttpResponse"
        Logger.verbose(
            TAG + methodName,
            "Getting SignInInitiateResult from HttpResponse..."
        )

        val result = if (response.statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            val errorResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromUnsuccessfulResponse(response.body),
                SignInInitiateErrorResponse::class.java
            )
            errorResponse.statusCode = response.statusCode
            SignInInitiateResult.createError(errorResponse)
        } else {
            val successResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromSuccessfulResponse(response.body),
                SignInInitiateSuccessResponse::class.java
            )
            SignInInitiateResult.createSuccess(successResponse)
        }
        ResultUtil.logResult(TAG, result)

        // TODO: error handling (error code matching, etc.), JSON to object deserialization

        // TODO manage headers for telemetry
        return result
    }
    //endregion

    //region /oauth/v2.0/challenge
    @Throws(ClientException::class)
    internal fun getSignInChallengeResultFromHttpResponse(
        response: HttpResponse
    ): SignInChallengeResult {
        val methodName = ":getSignInChallengeResultFromHttpResponse"
        Logger.verbose(
            TAG + methodName,
            "Getting SignInChallengeResult from HttpResponse..."
        )

        val result = if (response.statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            val errorResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromUnsuccessfulResponse(response.body),
                SignInChallengeErrorResponse::class.java
            )
            errorResponse.statusCode = response.statusCode
            SignInChallengeResult.createError(errorResponse)
        } else {
            val successResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromSuccessfulResponse(response.body),
                SignInChallengeSuccessResponse::class.java
            )
            SignInChallengeResult.createSuccess(successResponse)
        }
        ResultUtil.logResult(TAG, result)

        // TODO: error handling (error code matching, etc.), JSON to object deserialization
        // TODO manage headers for telemetry

        return result
    }
    //endregion

    //region /oauth/v2.0/token
    @Throws(ClientException::class)
    internal fun getSignInTokenResultFromHttpResponse(
        response: HttpResponse
    ): SignInTokenResult {
        val methodName = ":getSignInTokenResultFromHttpResponse"
        Logger.verbose(
            TAG + methodName,
            "Getting SignInTokenResult from HttpResponse..."
        )

        val result = if (response.statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            val errorResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromUnsuccessfulResponse(response.body),
                SignInTokenErrorResponse::class.java
            )
            errorResponse.statusCode = response.statusCode
            SignInTokenResult.createError(errorResponse)
        } else {
            val successResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromSuccessfulResponse(response.body),
                SignInTokenSuccessResponse::class.java
            )
            SignInTokenResult.createSuccess(successResponse)
        }
        ResultUtil.logResult(TAG, result)

        // TODO: error handling (error code matching, etc.), JSON to object deserialization
        // TODO manage headers for telemetry

        return result
    }
    //endregion

    /**
     * Generic function that validates the API result. Will throw an exception if the validation
     * of required API response fields fails. Will log a warning if the validation of optional
     * API response fields fails.
     */
    internal fun validateApiResult(apiResult: IApiResult) {
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
