package com.microsoft.identity.common.java.providers.nativeauth.responses.signup.challenge

import com.microsoft.identity.common.java.providers.nativeauth.IApiResult

class SignUpChallengeResult private constructor(
    override val successResponse: SignUpChallengeResponse?,
    override val errorResponse: SignUpChallengeErrorResponse?
) : IApiResult() {

    companion object {
        fun createSuccess(response: SignUpChallengeResponse): SignUpChallengeResult {
            return SignUpChallengeResult(response, null)
        }

        fun createError(errorResponse: SignUpChallengeErrorResponse?): SignUpChallengeResult {
            return SignUpChallengeResult(null, errorResponse)
        }
    }
}
