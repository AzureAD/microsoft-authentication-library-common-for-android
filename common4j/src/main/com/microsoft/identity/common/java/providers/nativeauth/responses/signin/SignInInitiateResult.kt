package com.microsoft.identity.common.java.providers.nativeauth.responses.signin

import com.microsoft.identity.common.java.providers.nativeauth.IApiResult

class SignInInitiateResult private constructor(
    override val successResponse: SignInInitiateSuccessResponse?,
    override val errorResponse: SignInInitiateErrorResponse?
) : IApiResult() {
    companion object {
        fun createSuccess(response: SignInInitiateSuccessResponse): SignInInitiateResult {
            return SignInInitiateResult(response, null)
        }

        fun createError(errorResponse: SignInInitiateErrorResponse?): SignInInitiateResult {
            return SignInInitiateResult(null, errorResponse)
        }
    }
}
