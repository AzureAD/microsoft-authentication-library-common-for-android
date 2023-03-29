package com.microsoft.identity.common.internal.util

import com.microsoft.identity.common.java.exception.ClientException

object ArgUtils {
    @Throws(ClientException::class)
    fun validateNonNullArg(
        o: Any?,
        argName: String
    ) {
        if (null == o ||
            o is CharSequence && o.isBlank() ||
            o is String && o.isBlank() ||
            o is List<*> && o.isEmpty() ||
            o is Map<*, *> && o.isEmpty()
        ) {
            throw ClientException(
                argName,
                "$argName cannot be null or empty"
            )
        }
    }
}
