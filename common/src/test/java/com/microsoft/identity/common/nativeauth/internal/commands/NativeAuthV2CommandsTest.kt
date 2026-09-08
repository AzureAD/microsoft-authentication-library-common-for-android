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
package com.microsoft.identity.common.nativeauth.internal.commands

import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2ResendCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SignInAfterResetPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordV2StartCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2ResendCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2ResetPasswordStartCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SignInAfterResetPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SubmitCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SubmitNewPasswordCommandResult
import com.microsoft.identity.common.nativeauth.internal.controllers.v2.NativeAuthV2FlowController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertSame
import org.junit.Test

class NativeAuthV2CommandsTest {
    private val controller = mockk<NativeAuthV2FlowController>(relaxed = true)

    @Test
    fun resetPasswordStart_executeDelegatesAndReturnsResult() {
        val parameters = parameters<ResetPasswordV2StartCommandParameters>()
        val result = mockk<NativeAuthV2ResetPasswordStartCommandResult>()
        every { controller.resetPasswordStart(parameters) } returns result

        val actual = NativeAuthV2ResetPasswordStartCommand(parameters, controller, API_ID).execute()

        assertSame(result, actual)
        verify(exactly = 1) { controller.resetPasswordStart(parameters) }
    }

    @Test
    fun submitCode_executeDelegatesAndReturnsResult() {
        val parameters = parameters<NativeAuthV2SubmitCodeCommandParameters>()
        val result = mockk<NativeAuthV2SubmitCodeCommandResult>()
        every { controller.submitCode(parameters) } returns result

        val actual = NativeAuthV2SubmitCodeCommand(parameters, controller, API_ID).execute()

        assertSame(result, actual)
        verify(exactly = 1) { controller.submitCode(parameters) }
    }

    @Test
    fun resendCode_executeDelegatesAndReturnsResult() {
        val parameters = parameters<NativeAuthV2ResendCodeCommandParameters>()
        val result = mockk<NativeAuthV2ResendCodeCommandResult>()
        every { controller.resendCode(parameters) } returns result

        val actual = NativeAuthV2ResendCodeCommand(parameters, controller, API_ID).execute()

        assertSame(result, actual)
        verify(exactly = 1) { controller.resendCode(parameters) }
    }

    @Test
    fun submitNewPassword_executeDelegatesAndReturnsResult() {
        val parameters = parameters<NativeAuthV2SubmitNewPasswordCommandParameters>()
        val result = mockk<NativeAuthV2SubmitNewPasswordCommandResult>()
        every { controller.submitNewPassword(parameters) } returns result

        val actual = NativeAuthV2SubmitNewPasswordCommand(parameters, controller, API_ID).execute()

        assertSame(result, actual)
        verify(exactly = 1) { controller.submitNewPassword(parameters) }
    }

    @Test
    fun signInAfterResetPassword_executeDelegatesAndReturnsResult() {
        val parameters = parameters<NativeAuthV2SignInAfterResetPasswordCommandParameters>()
        val result = mockk<NativeAuthV2SignInAfterResetPasswordCommandResult>()
        every { controller.signInAfterResetPassword(parameters) } returns result

        val actual =
            NativeAuthV2SignInAfterResetPasswordCommand(parameters, controller, API_ID).execute()

        assertSame(result, actual)
        verify(exactly = 1) { controller.signInAfterResetPassword(parameters) }
    }

    private inline fun <reified T : Any> parameters(): T = mockk(relaxed = true)

    private companion object {
        private const val API_ID = "public-api-id"
    }
}
