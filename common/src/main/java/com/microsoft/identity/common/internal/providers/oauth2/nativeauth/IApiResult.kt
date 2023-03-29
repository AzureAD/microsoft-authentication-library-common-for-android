package com.microsoft.identity.common.internal.providers.oauth2.nativeauth

import com.microsoft.identity.common.java.providers.oauth2.IErrorResponse
import com.microsoft.identity.common.java.providers.oauth2.IResult
import com.microsoft.identity.common.java.providers.oauth2.ISuccessResponse

abstract class IApiResult : IResult {
    abstract val successResponse: IApiSuccessResponse?
    abstract val errorResponse: IApiErrorResponse?

    override fun getSuccess() = successResponse != null

    override fun getErrorResponse(): IErrorResponse? {
        return errorResponse
    }

    override fun getSuccessResponse(): ISuccessResponse? {
        return successResponse
    }
}
