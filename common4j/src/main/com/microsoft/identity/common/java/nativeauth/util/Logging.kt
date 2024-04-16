package com.microsoft.identity.common.java.nativeauth.util

interface Logging {
    fun toSafeString(mayContainPii: Boolean): String

    fun containsPii(): Boolean
}
