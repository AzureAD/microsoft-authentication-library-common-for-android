package com.microsoft.identity.common.java.providers.nativeauth.responses

import com.microsoft.identity.common.java.util.CommonUtils

open class ApiErrorResult(
    open val error: String?,
    open val errorDescription: String?,
    open val details: List<Map<String, String>>? = null,
    open val correlationId: String = CommonUtils.getCurrentThreadCorrelationId(),
    open val errorCodes: List<Int>? = null
)