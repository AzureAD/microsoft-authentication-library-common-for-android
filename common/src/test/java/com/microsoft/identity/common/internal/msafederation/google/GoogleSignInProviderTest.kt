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
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.microsoft.identity.common.internal.msafederation.MsaFederationConstants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [GoogleSignInProvider].
 */
@RunWith(RobolectricTestRunner::class)
class GoogleSignInProviderTest {

    @Test
    fun testSignIn() {
        val mockCredentialManager = mockk<CredentialManager>()
        val mockActivity = mockk<Activity>()
        val mockParameters = SignInWithGoogleParameters(mockActivity)
        val webClientId = MsaFederationConstants.GOOGLE_MSA_WEB_CLIENT_ID
        val mockIdToken = "mockIdToken"
        val googleSignInProvider = GoogleSignInProvider(mockCredentialManager, mockParameters, webClientId)

        val mockCredential = GoogleIdTokenCredential(
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
            mockIdToken,
            null,
            null,
            null,
            null,
            null)
        val mockGetCredentialResponse = GetCredentialResponse(mockCredential)
        val getCredentialRequestSlot = slot<GetCredentialRequest>()
        val activitySlot = slot<Activity>()
        coEvery { mockCredentialManager.getCredential(capture(activitySlot), capture(getCredentialRequestSlot)) } returns mockGetCredentialResponse

        val result = runBlocking { googleSignInProvider.signIn() }
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals(mockIdToken, result.getOrNull()!!.idToken)
        val capturedRequest = getCredentialRequestSlot.captured
        assertNotNull(capturedRequest)
        assertEquals(1, capturedRequest.credentialOptions.size)
        assertEquals(webClientId, (capturedRequest.credentialOptions[0] as GetSignInWithGoogleOption).serverClientId)
        val capturedActivity = activitySlot.captured
        assertSame(mockActivity, capturedActivity)
    }

    @Test
    fun testSignInBottomSheet() {
        val mockCredentialManager = mockk<CredentialManager>()
        val mockActivity = mockk<Activity>()
        val mockParameters = SignInWithGoogleParameters(mockActivity, true)
        val webClientId = MsaFederationConstants.GOOGLE_MSA_WEB_CLIENT_ID
        val mockIdToken = "mockIdToken"
        val googleSignInProvider = GoogleSignInProvider(mockCredentialManager, mockParameters, webClientId)

        val mockCredential = GoogleIdTokenCredential(
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
            mockIdToken,
            null,
            null,
            null,
            null,
            null)
        val mockGetCredentialResponse = GetCredentialResponse(mockCredential)
        val getCredentialRequestSlot = slot<GetCredentialRequest>()
        val activitySlot = slot<Activity>()
        coEvery { mockCredentialManager.getCredential(capture(activitySlot), capture(getCredentialRequestSlot)) } returns mockGetCredentialResponse

        val result = runBlocking { googleSignInProvider.signIn() }
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals(mockIdToken, result.getOrNull()!!.idToken)
        val capturedRequest = getCredentialRequestSlot.captured
        assertNotNull(capturedRequest)
        assertEquals(1, capturedRequest.credentialOptions.size)

        val capturedGoogleIdOption = capturedRequest.credentialOptions[0] as GetGoogleIdOption
        assertEquals(webClientId, capturedGoogleIdOption.serverClientId)
        assertEquals(false, capturedGoogleIdOption.filterByAuthorizedAccounts)
        assertEquals(false, capturedGoogleIdOption.autoSelectEnabled)
        val capturedActivity = activitySlot.captured
        assertSame(mockActivity, capturedActivity)
    }

    @Test
    fun testSignOut() {
        val mockCredentialManager = mockk<CredentialManager>()
        val mockActivity = mockk<Activity>()
        val mockParameters = SignInWithGoogleParameters(mockActivity)
        val webClientId = MsaFederationConstants.GOOGLE_MSA_WEB_CLIENT_ID
        val googleSignInProvider = GoogleSignInProvider(mockCredentialManager, mockParameters, webClientId)

        coEvery { mockCredentialManager.clearCredentialState(any()) } returns Unit
        runBlocking { googleSignInProvider.signOut() }
        coVerify { mockCredentialManager.clearCredentialState(any<ClearCredentialStateRequest>()) }
    }
}
