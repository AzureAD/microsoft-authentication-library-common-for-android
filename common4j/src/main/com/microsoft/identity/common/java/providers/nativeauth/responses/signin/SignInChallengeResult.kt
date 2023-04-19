package com.microsoft.identity.common.java.providers.nativeauth.responses.signin

import com.microsoft.identity.common.java.providers.nativeauth.IApiResult

class SignInChallengeResult private constructor(
    override val successResponse: SignInChallengeSuccessResponse?,
    override val errorResponse: SignInChallengeErrorResponse?
) : IApiResult() {
    companion object {
        fun createSuccess(response: SignInChallengeSuccessResponse): SignInChallengeResult {
            return SignInChallengeResult(response, null)
        }

        fun createError(errorResponse: SignInChallengeErrorResponse?): SignInChallengeResult {
            return SignInChallengeResult(null, errorResponse)
        }
    }
}
