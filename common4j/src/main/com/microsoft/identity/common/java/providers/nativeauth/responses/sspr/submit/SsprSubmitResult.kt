package com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.submit

import com.microsoft.identity.common.java.providers.nativeauth.IApiResult

class SsprSubmitResult private constructor(
    override val successResponse: SsprSubmitResponse?,
    override val errorResponse: SsprSubmitErrorResponse?
) : IApiResult() {

    companion object {
        fun createSuccess(response: SsprSubmitResponse): SsprSubmitResult {
            return SsprSubmitResult(response, null)
        }

        fun createError(errorResponse: SsprSubmitErrorResponse?): SsprSubmitResult {
            return SsprSubmitResult(null, errorResponse)
        }
    }
}
