package com.microsoft.identity.common.java.controllers.results

import com.microsoft.identity.common.java.util.CommonUtils

interface ICommandResult {

    data class UnknownError(
        override val error: String?,
        override val errorDescription: String?,
        override val details: List<Map<String, String>>? = null,
        override val correlationId: String = CommonUtils.getCurrentThreadCorrelationId(),
        override val errorCodes: List<Int>? = null,
        val exception: Exception? = null
    ): Error(error, errorDescription, details, correlationId, errorCodes), ICommandResult

    open class Error(
        open val error: String?,
        open val errorDescription: String?,
        open val details: List<Map<String, String>>? = null,
        open val correlationId: String = CommonUtils.getCurrentThreadCorrelationId(),
        open val errorCodes: List<Int>? = null
    )
}
