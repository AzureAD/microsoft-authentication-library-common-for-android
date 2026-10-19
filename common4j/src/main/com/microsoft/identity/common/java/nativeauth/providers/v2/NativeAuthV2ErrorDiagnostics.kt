// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.common.java.nativeauth.providers.v2

import java.math.BigDecimal

/** Shared diagnostic decoding for V2 HAL and OAuth error envelopes. */
internal object NativeAuthV2ErrorDiagnostics {
    private val aadstsCodeRegex = Regex("AADSTS(\\d+)")

    /** A malformed array is rejected as a whole; an explicit empty array remains authoritative. */
    fun readErrorCodes(value: Any?): List<Int>? {
        val values = value as? List<*> ?: return null
        return values.map {
            val number = it as? Number ?: return null
            try {
                BigDecimal(number.toString()).intValueExact()
            } catch (e: NumberFormatException) {
                return null
            } catch (e: ArithmeticException) {
                return null
            }
        }
    }

    fun codesFromMessage(message: String?): List<Int>? =
        message?.let {
            aadstsCodeRegex.findAll(it).mapNotNull { match ->
                match.groupValues[1].toIntOrNull()
            }.toList().ifEmpty { null }
        }
}
