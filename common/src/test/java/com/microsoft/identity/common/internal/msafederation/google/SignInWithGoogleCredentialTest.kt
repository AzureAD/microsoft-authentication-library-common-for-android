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

import com.microsoft.identity.common.internal.msafederation.MsaFederationConstants
import com.microsoft.identity.common.internal.msafederation.FederatedSignInProviderName
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * Tests for [SignInWithGoogleCredential].
 */
@RunWith(JUnit4::class)
class SignInWithGoogleCredentialTest {

    @Test
    fun testSignInWithGoogleCredential() {
        val testIdToken = "test-id-token"
        val credential = SignInWithGoogleCredential(testIdToken)
        assertEquals(FederatedSignInProviderName.GOOGLE, credential.signInProviderName)
        assertEquals(testIdToken, credential.idToken)

        val headers = credential.getIdProviderHeaders();
        assertEquals(1, headers.size)
        assertEquals(testIdToken, headers[MsaFederationConstants.MSA_ID_TOKEN_HEADER_KEY])

        val idProviderExtraQueryParam = credential.getIdProviderExtraQueryParam()
        assertEquals(MsaFederationConstants.MSA_ID_PROVIDER_EXTRA_QUERY_PARAM_KEY, idProviderExtraQueryParam.key)
        assertEquals(FederatedSignInProviderName.GOOGLE.getIdProviderName(), idProviderExtraQueryParam.value)

        // serialize and deserialize credential using gson
        val gson = Gson()
        val json = gson.toJson(credential)
        val deserializedCredential = gson.fromJson(json, SignInWithGoogleCredential::class.java)
        assertEquals(credential, deserializedCredential)
    }
}
