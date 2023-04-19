package com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.start

import com.microsoft.identity.common.java.providers.nativeauth.IApiResult

class SsprStartResult private constructor(
    override val successResponse: SsprStartResponse?,
    override val errorResponse: SsprStartErrorResponse?
) : IApiResult() {

    companion object {
        fun createSuccess(response: SsprStartResponse): SsprStartResult {
            return SsprStartResult(response, null)
        }

        fun createError(errorResponse: SsprStartErrorResponse?): SsprStartResult {
            return SsprStartResult(null, errorResponse)
        }
    }
}
