package com.microsoft.identity.common.java.nativeauth.util

internal fun List<ILoggable>.toUnsanitizedString(): String =
    this.joinToString(", ") { it.toUnsanitizedString() }
