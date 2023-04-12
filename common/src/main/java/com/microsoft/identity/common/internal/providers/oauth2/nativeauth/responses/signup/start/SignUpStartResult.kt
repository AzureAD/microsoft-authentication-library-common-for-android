package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.start

import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.IApiResult

class SignUpStartResult private constructor(
    override val successResponse: SignUpStartResponse?,
    override val errorResponse: SignUpStartErrorResponse?
) : IApiResult() {

    companion object {
        fun createSuccess(response: SignUpStartResponse): SignUpStartResult {
            return SignUpStartResult(response, null)
        }

        fun createError(errorResponse: SignUpStartErrorResponse?): SignUpStartResult {
            return SignUpStartResult(null, errorResponse)
        }
    }
}
