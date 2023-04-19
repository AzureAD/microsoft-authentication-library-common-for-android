package com.microsoft.identity.common.java.providers.nativeauth.responses.signin

import com.microsoft.identity.common.java.providers.nativeauth.IApiResult

class SignInTokenResult private constructor(
    override val successResponse: SignInTokenSuccessResponse?,
    override val errorResponse: SignInTokenErrorResponse?
) : IApiResult() {
    companion object {
        fun createSuccess(response: SignInTokenSuccessResponse): SignInTokenResult {
            return SignInTokenResult(response, null)
        }

        fun createError(errorResponse: SignInTokenErrorResponse?): SignInTokenResult {
            return SignInTokenResult(null, errorResponse)
        }
    }
}
