package com.microsoft.identity.common.java.opentelemetry

import io.opentelemetry.api.common.AttributeKey


class LogAttribute {

    companion object {
        @JvmStatic
        val authority: AttributeKey<String> = AttributeKey.stringKey("authority")
        @JvmStatic
        val error: AttributeKey<String> = AttributeKey.stringKey("error")
    }
}