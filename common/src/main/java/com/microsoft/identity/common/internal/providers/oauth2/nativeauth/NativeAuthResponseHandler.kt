package com.microsoft.identity.common.internal.providers.oauth2.nativeauth

import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpChallengeErrorResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpChallengeResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpChallengeResult
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpStartErrorResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpStartResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpStartResult
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
            // An error occurred
            val errorResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromUnsuccessfulResponse(response.body),
                SignUpStartErrorResponse::class.java
            )
            errorResponse.statusCode = response.statusCode
            // TODO
//            if (null != response.headers) {
//                errorResponse.setResponseHeadersJson(
//                    HeaderSerializationUtil.toJson(response.headers)
//                )
//            }
//            errorResponse.setResponseBody(response.body)
            SignUpStartResult.createError(errorResponse)
        } else {
            val successResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromSuccessfulResponse(response.body),
                SignUpStartResponse::class.java
            )
            SignUpStartResult.createSuccess(successResponse)
        }
        ResultUtil.logResult(TAG, result)

        // TODO: error handling (error code matching, etc.), JSON to object deserialisation

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
            // An error occurred
            val errorResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromUnsuccessfulResponse(response.body),
                SignUpChallengeErrorResponse::class.java
            )
            errorResponse.statusCode = response.statusCode
            // TODO
//            if (null != response.headers) {
//                errorResponse.setResponseHeadersJson(
//                    HeaderSerializationUtil.toJson(response.headers)
//                )
//            }
//            errorResponse.setResponseBody(response.body)
            SignUpChallengeResult.createError(errorResponse)
        } else {
            val successResponse = ObjectMapper.deserializeJsonStringToObject(
                getBodyFromSuccessfulResponse(response.body),
                SignUpChallengeResponse::class.java
            )
            SignUpChallengeResult.createSuccess(successResponse)
        }
        ResultUtil.logResult(TAG, result)

        // TODO: error handling (error code matching, etc.), JSON to object deserialisation
        // TODO manage headers for telemetry

        return result
    }
    //endregion

    /**
     * Generic function that validates the API result. Will throw an exception if the validation
     * of required API response fields fails. Will log a warning if the validation of optional
     * API response fields fails.
     */
    internal fun validateApiResult(apiResult: com.microsoft.identity.common.internal.providers.oauth2.nativeauth.IApiResult) {
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

    private fun validateSuccessfulResponse(successResponse: com.microsoft.identity.common.internal.providers.oauth2.nativeauth.IApiSuccessResponse?) {
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

    private fun validateUnsuccessfulResponse(errorResponse: com.microsoft.identity.common.internal.providers.oauth2.nativeauth.IApiErrorResponse?) {
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
