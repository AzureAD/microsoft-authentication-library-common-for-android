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
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.opentelemetry.api.trace.Span
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
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

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        // This test app is not a signed broker, so registration would otherwise be refused.
        FidoManagerFactory.isBrokerHosted = { true }
    }

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
        FidoManagerFactory.setProvider(context, null)
        FidoManagerFactory.isBrokerHosted = { false }
    }

    @Test
    fun testUsesCredentialManagerWhenNoProviderRegistered() {
        assertTrue(FidoManagerFactory.getFidoManager(activity, null) is CredManFidoManager)
    }

    @Test
    fun testUsesHostManagerWhenProviderSuppliesOne() {
        FidoManagerFactory.setProvider(context, providerReturning(hostManager))
        assertSame(hostManager, FidoManagerFactory.getFidoManager(activity, null))
    }

    @Test
    fun testFallsBackWhenProviderDeclines() {
        FidoManagerFactory.setProvider(context, providerReturning(null))
        assertTrue(FidoManagerFactory.getFidoManager(activity, null) is CredManFidoManager)
    }

    /**
     * A library embedded in a non-broker app must not be able to take over passkey handling, and
     * the caller has to be told, since a genuine broker treats refusal as a misconfiguration.
     */
    @Test
    fun testProviderFromANonBrokerAppIsIgnored() {
        FidoManagerFactory.isBrokerHosted = { false }
        assertFalse(FidoManagerFactory.setProvider(context, providerReturning(hostManager)))
        assertTrue(FidoManagerFactory.getFidoManager(activity, null) is CredManFidoManager)
    }

    @Test
    fun testProviderCanBeUnregistered() {
        FidoManagerFactory.setProvider(context, providerReturning(hostManager))
        FidoManagerFactory.setProvider(context, null)
        assertTrue(FidoManagerFactory.getFidoManager(activity, null) is CredManFidoManager)
    }
}
