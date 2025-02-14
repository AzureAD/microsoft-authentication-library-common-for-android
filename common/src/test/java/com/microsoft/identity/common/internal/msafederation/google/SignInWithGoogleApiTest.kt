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
import com.microsoft.identity.common.internal.msafederation.MsaFederatedSignInProviderFactory
import com.microsoft.identity.common.java.exception.BaseException
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    private val mockActivity: Activity = Robolectric.buildActivity(Activity::class.java).get()
    private val mockParameters: SignInWithGoogleParameters = SignInWithGoogleParameters(mockActivity)

    @Test
    fun testSignIn() {
        val signInWithGoogleApi = createSignInWithGoogleApi()
        val credential = runBlocking {
            signInWithGoogleApi.signIn(mockParameters)
        }

        assertNotNull(credential)
    }

    @Test
    fun testSignInSync() {
        val signInWithGoogleApi = createSignInWithGoogleApi()
        val credential = signInWithGoogleApi.signInSync(mockParameters)
        assertNotNull(credential)
    }

    @Test
    fun testSignInAsync() {
        val signInWithGoogleApi = createSignInWithGoogleApi()
        val latch = CountDownLatch(1)
        var result: SignInWithGoogleCredential? = null
        signInWithGoogleApi.signInAsync(mockParameters).whenComplete { credential, exception ->
            result = credential
            latch.countDown()
        }
        latch.await()
        assertNotNull(result)
    }

    @Test
    fun testSignInAsyncFailure() {
        val mockResult: Result<SignInWithGoogleCredential> = Result.failure(BaseException("Mock exception"))
        val signInWithGoogleApi = createSignInWithGoogleApi(MockGoogleSignInProvider(mockResult))
        val latch = CountDownLatch(1)
        var result: SignInWithGoogleCredential? = null
        var exception: Exception? = null
        signInWithGoogleApi.signInAsync(mockParameters).whenComplete { credential, e ->
            result = credential
            exception = e as Exception?
            latch.countDown()
        }
        latch.await()
        assertNull(result)
        assertNotNull(exception)
        assertTrue(exception is BaseException)
    }

    private fun createSignInWithGoogleApi(
        mockGoogleSignInProvider: MockGoogleSignInProvider = MockGoogleSignInProvider()) : SignInWithGoogleApi {
        val mockFederatedSignInProviderFactory: MsaFederatedSignInProviderFactory = mockk()
        every { mockFederatedSignInProviderFactory.getProvider(any()) } returns mockGoogleSignInProvider
        return SignInWithGoogleApi(mockFederatedSignInProviderFactory)
    }
}
