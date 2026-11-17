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
package com.microsoft.identity.common.java.nativeauth.commands.parameters

import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationStateTestFactory
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class NativeAuthV2SelectAuthMethodCommandParametersTest {
    @Test
    fun toBuilder_preservesContinuationAndTokenRequestContext() {
        val components = mockk<IPlatformComponents>()
        val claims = """{"access_token":{"xms_cc":{"values":["cp1"]}}}"""
        val state = NativeAuthV2ContinuationStateTestFactory.create(
            "correlation", listOf("User.Read"), NativeAuthV2LinkRelation.SIGN_IN,
            NativeAuthV2FlowScenario.SIGN_IN
        )
        val original = NativeAuthV2SelectAuthMethodCommandParameters.builder()
            .platformComponents(components)
            .continuationState(state)
            .methodId("email-1")
            .clientId("client")
            .correlationId("correlation")
            .scopes(listOf("User.Read"))
            .claimsRequestJson(claims)
            .build()

        val copy = original.toBuilder().methodId("password-1").build()

        assertSame(components, copy.platformComponents)
        assertSame(state, copy.continuationState)
        assertEquals("password-1", copy.methodId)
        assertEquals("email-1", original.methodId)
        assertEquals(original.clientId, copy.clientId)
        assertEquals(original.correlationId, copy.correlationId)
        assertEquals(original.scopes, copy.scopes)
        assertEquals(claims, copy.claimsRequestJson)
        assertFalse(copy.toString().contains(claims))
        assertFalse(copy.toUnsanitizedString().contains(claims))
        assertFalse(copy.containsPii())
    }

    @Test
    fun builder_rejectsMissingContinuationOrMethod() {
        val components = mockk<IPlatformComponents>()
        val state = NativeAuthV2ContinuationStateTestFactory.create("correlation")

        assertThrows(NullPointerException::class.java) {
            NativeAuthV2SelectAuthMethodCommandParameters.builder()
                .platformComponents(components).methodId("email-1").build()
        }
        assertThrows(NullPointerException::class.java) {
            NativeAuthV2SelectAuthMethodCommandParameters.builder()
                .platformComponents(components).continuationState(state).build()
        }
    }
}
