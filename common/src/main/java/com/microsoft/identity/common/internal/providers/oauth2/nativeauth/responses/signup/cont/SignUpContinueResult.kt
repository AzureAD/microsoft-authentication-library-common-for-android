package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.cont

import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.IApiResult

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
