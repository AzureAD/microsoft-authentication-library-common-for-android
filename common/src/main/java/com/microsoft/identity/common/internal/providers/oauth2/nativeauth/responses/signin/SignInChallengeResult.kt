package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signin

import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.IApiResult

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
