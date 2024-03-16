package com.microsoft.identity.common.java.logging


interface IAppender {


    fun append(logRecord: LogRecord)
    fun appendError(logRecord: LogRecord, throwable: Throwable)
}