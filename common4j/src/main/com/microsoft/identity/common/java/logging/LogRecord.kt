package com.microsoft.identity.common.java.logging

import java.util.Date

data class LogRecord(
    val tag: String,
    val logLevel: Int,
    val timeStamp: Date,
    val message: String?,
    val attributes: MutableMap<String, out Any>?
)
