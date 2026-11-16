//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.nativeauth.controllers.results

import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2AuthMethod
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAuthV2CommandResultTest {
    private val state = mockk<NativeAuthV2ContinuationState>()

    @Test
    fun incorrectPassword_preservesDiagnosticsWithoutExposingThemInSanitizedString() {
        val details = listOf(mapOf("attributeIds" to listOf("password"), "code" to "policy"))
        val result = NativeAuthV2CommandResult.IncorrectPassword(
            CORRELATION_ID, ERROR, ERROR_DESCRIPTION, SUB_ERROR,
            listOf(50126), "trace-id", "timestamp", details
        )

        assertEquals(CORRELATION_ID, result.correlationId)
        assertEquals(ERROR, result.error)
        assertEquals(ERROR_DESCRIPTION, result.errorDescription)
        assertEquals(SUB_ERROR, result.subError)
        assertEquals(listOf(50126), result.errorCodes)
        assertEquals("trace-id", result.traceId)
        assertEquals("timestamp", result.timestamp)
        assertEquals(details, result.attributeValidationDetails)
        for (value in listOf(ERROR, ERROR_DESCRIPTION, SUB_ERROR, "trace-id", "timestamp", "policy")) {
            assertFalse(result.toString().contains(value))
        }
        assertTrue(result.toUnsanitizedString().contains(ERROR_DESCRIPTION))
    }

    @Test
    fun passwordNotAccepted_preservesPolicyDetailsWithoutExposingThemInSanitizedString() {
        val details = listOf(mapOf("attributeIds" to listOf("password"), "code" to "policy"))
        val result = NativeAuthV2CommandResult.PasswordNotAccepted(
            CORRELATION_ID, ERROR, ERROR_DESCRIPTION, SUB_ERROR,
            listOf(120002), "trace-id", "timestamp", details
        )

        assertEquals(CORRELATION_ID, result.correlationId)
        assertEquals(ERROR, result.error)
        assertEquals(ERROR_DESCRIPTION, result.errorDescription)
        assertEquals(SUB_ERROR, result.subError)
        assertEquals(listOf(120002), result.errorCodes)
        assertEquals("trace-id", result.traceId)
        assertEquals("timestamp", result.timestamp)
        assertEquals(details, result.attributeValidationDetails)
        for (value in listOf(ERROR, ERROR_DESCRIPTION, SUB_ERROR, "trace-id", "timestamp", "policy")) {
            assertFalse(result.toString().contains(value))
        }
        assertTrue(result.toUnsanitizedString().contains(ERROR_DESCRIPTION))
    }

    @Test
    fun resultStrings_redactUserFacingAndServiceErrorDetails() {
        val sensitiveValues = listOf(
            TARGET,
            ERROR,
            ERROR_DESCRIPTION,
            SUB_ERROR
        )
        val results = listOf<INativeAuthCommandResult>(
            NativeAuthV2CommandResult.CodeRequired(
                CORRELATION_ID,
                state,
                6,
                TARGET,
                "email"
            ),
            NativeAuthV2CommandResult.AuthMethodSelectionRequired(
                CORRELATION_ID,
                state,
                listOf(NativeAuthV2AuthMethod("sms-1", "sms", TARGET))
            ),
            NativeAuthV2CommandResult.IncorrectCode(
                CORRELATION_ID,
                ERROR,
                ERROR_DESCRIPTION,
                SUB_ERROR
            ),
            NativeAuthV2CommandResult.PasswordNotAccepted(
                CORRELATION_ID,
                ERROR,
                ERROR_DESCRIPTION,
                SUB_ERROR
            ),
            NativeAuthV2CommandResult.PasswordResetFailed(
                CORRELATION_ID,
                ERROR,
                ERROR_DESCRIPTION
            ),
            NativeAuthV2CommandResult.UserNotFound(
                CORRELATION_ID,
                ERROR,
                ERROR_DESCRIPTION
            ),
            NativeAuthV2CommandResult.NotImplemented(
                CORRELATION_ID,
                ERROR,
                ERROR_DESCRIPTION
            )
        )

        results.forEach { result ->
            sensitiveValues.forEach { sensitive ->
                assertFalse(result.toString(), result.toString().contains(sensitive))
            }
            assertTrue(result.toString().contains(CORRELATION_ID))
            assertTrue(result.toUnsanitizedString().contains(CORRELATION_ID))
        }
    }

    @Test
    fun continuationAndCompleteResults_haveStableDiagnosticStrings() {
        val results = listOf<INativeAuthCommandResult>(
            NativeAuthV2CommandResult.NewPasswordRequired(CORRELATION_ID, state),
            NativeAuthV2CommandResult.SignInAfterResetPasswordRequired(CORRELATION_ID, state),
            NativeAuthV2CommandResult.Complete(
                correlationId = CORRELATION_ID,
                authenticationResult = null,
                continuationToken = "token",
                expiresIn = 60
            )
        )

        results.forEach { result ->
            assertTrue(result.toString().contains(CORRELATION_ID))
            assertTrue(result.toUnsanitizedString().contains(CORRELATION_ID))
            assertFalse(result.toString().contains("token"))
        }
    }

    private companion object {
        private const val CORRELATION_ID = "correlation-id"
        private const val TARGET = "a***@contoso.com"
        private const val ERROR = "invalid_grant"
        private const val ERROR_DESCRIPTION = "Sensitive service description"
        private const val SUB_ERROR = "password_too_weak"
    }
}
