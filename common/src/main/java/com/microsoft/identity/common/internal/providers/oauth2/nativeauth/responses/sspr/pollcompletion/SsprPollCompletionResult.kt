package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.sspr.pollcompletion

import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.IApiResult

class SsprPollCompletionResult private constructor(
    override val successResponse: SsprPollCompletionResponse?,
    override val errorResponse: SsprPollCompletionErrorResponse?
) : IApiResult() {

    companion object {
        fun createSuccess(response: SsprPollCompletionResponse): SsprPollCompletionResult {
            return SsprPollCompletionResult(response, null)
        }

        fun createError(errorResponse: SsprPollCompletionErrorResponse?): SsprPollCompletionResult {
            return SsprPollCompletionResult(null, errorResponse)
        }
    }
}
