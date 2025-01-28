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
package com.microsoft.identity.common.internal.msafederation.google

import android.app.Activity
import com.microsoft.identity.common.internal.msafederation.FederatedSignInProviderFactory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch

/**
 * Tests for [SignInWithGoogleApi].
 */
@RunWith(RobolectricTestRunner::class)
class SignInWithGoogleApiTest {
    private lateinit var mockFederatedSignInProviderFactory: FederatedSignInProviderFactory
    private lateinit var mockGoogleSignInProvider: MockGoogleSignInProvider
    private lateinit var signInWithGoogleApi: SignInWithGoogleApi
    private lateinit var mockActivity: Activity
    private lateinit var mockParameters: SignInWithGoogleParameters

    @Before
    fun setUp() {
        mockGoogleSignInProvider = MockGoogleSignInProvider()
        mockActivity = Robolectric.buildActivity(Activity::class.java).get()
        mockParameters = SignInWithGoogleParameters(mockActivity)
        mockFederatedSignInProviderFactory = mockk()
        every { mockFederatedSignInProviderFactory.getProvider(any()) } returns mockGoogleSignInProvider
        signInWithGoogleApi = SignInWithGoogleApi(mockFederatedSignInProviderFactory)
    }

    @Test
    fun testSignIn() {
        val credential = runBlocking {
            signInWithGoogleApi.signIn(mockParameters)
        }

        assertNotNull(credential)
    }

    @Test
    fun testSignInSync() {
        val credential = signInWithGoogleApi.signInSync(mockParameters)

        assertNotNull(credential)
    }

    @Test
    fun testSignInAsync() {
        val latch = CountDownLatch(1)
        var result: SignInWithGoogleCredential? = null
        val callback = object : ISignInWithGoogleCredentialCallback {
            override fun onSuccess(credential: SignInWithGoogleCredential) {
                result = credential
                latch.countDown()
            }
        }
        signInWithGoogleApi.signInAsync(mockParameters, callback)
        latch.await()
        assertNotNull(result)
    }
}
