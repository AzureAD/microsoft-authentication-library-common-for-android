package com.microsoft.identity.common.java.logging

import com.microsoft.identity.common.java.opentelemetry.LogAttribute
import com.microsoft.identity.common.java.opentelemetry.OTelUtility

class OtelAppender : IAppender{


    override fun append(logRecord: LogRecord) {
        OTelUtility.createLogBuilder()
            .setBody(logRecord.message ?: "no boby")
            .setAttribute(LogAttribute.authority, "modera")
            .emit()
    }

    override fun appendError(logRecord: LogRecord, throwable: Throwable) {
        // do nothing
    }
}