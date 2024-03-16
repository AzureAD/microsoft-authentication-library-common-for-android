package com.microsoft.identity.common.java.opentelemetry

import io.opentelemetry.api.common.AttributeKey


class LogAttribute {

    companion object {
        const val AUTHORITY = "authority"
        @JvmStatic
        val authority: AttributeKey<String> = AttributeKey.stringKey(AUTHORITY)
    }
}