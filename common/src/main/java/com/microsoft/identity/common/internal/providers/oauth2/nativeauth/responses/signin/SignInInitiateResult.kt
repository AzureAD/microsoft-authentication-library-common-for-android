package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signin

import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.IApiResult

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
