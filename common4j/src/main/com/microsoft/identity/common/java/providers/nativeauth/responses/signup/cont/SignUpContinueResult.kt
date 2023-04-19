package com.microsoft.identity.common.java.providers.nativeauth.responses.signup.cont

import com.microsoft.identity.common.java.providers.nativeauth.IApiResult

class SignUpContinueResult private constructor(
    override val successResponse: SignUpContinueResponse?,
    override val errorResponse: SignUpContinueErrorResponse?
) : IApiResult() {

    companion object {
        fun createSuccess(response: SignUpContinueResponse): SignUpContinueResult {
            return SignUpContinueResult(response, null)
        }

        fun createError(errorResponse: SignUpContinueErrorResponse?): SignUpContinueResult {
            return SignUpContinueResult(null, errorResponse)
        }
    }
}
