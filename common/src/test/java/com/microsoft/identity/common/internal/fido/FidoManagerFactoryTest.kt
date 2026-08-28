// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.fido

import android.app.Activity
import io.opentelemetry.api.trace.Span
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * This routing decides which backend fulfils a passkey challenge, so a regression here either
 * disables a host's ceremony or sends every consumer down a path meant for one host.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FidoManagerFactoryTest {

    private val activity: Activity =
        Robolectric.buildActivity(Activity::class.java).setup().get()

    private val hostManager = object : IFidoManager {
        override suspend fun authenticate(
            challenge: String,
            relyingPartyIdentifier: String,
            allowedCredentials: List<String>?,
            userVerificationPolicy: String,
            correlationId: String?,
            span: Span
        ): String = ""
    }

    private fun providerReturning(manager: IFidoManager?) = object : IFidoManagerProvider {
        override fun getFidoManager(activity: Activity): IFidoManager? = manager
    }

    @After
    fun tearDown() {
        FidoManagerFactory.setProvider(null)
    }

    @Test
    fun testUsesCredentialManagerWhenNoProviderRegistered() {
        assertTrue(FidoManagerFactory.getFidoManager(activity, null) is CredManFidoManager)
    }

    @Test
    fun testUsesHostManagerWhenProviderSuppliesOne() {
        FidoManagerFactory.setProvider(providerReturning(hostManager))
        assertSame(hostManager, FidoManagerFactory.getFidoManager(activity, null))
    }

    @Test
    fun testFallsBackWhenProviderDeclines() {
        FidoManagerFactory.setProvider(providerReturning(null))
        assertTrue(FidoManagerFactory.getFidoManager(activity, null) is CredManFidoManager)
    }

    @Test
    fun testProviderCanBeUnregistered() {
        FidoManagerFactory.setProvider(providerReturning(hostManager))
        FidoManagerFactory.setProvider(null)
        assertTrue(FidoManagerFactory.getFidoManager(activity, null) is CredManFidoManager)
    }
}
