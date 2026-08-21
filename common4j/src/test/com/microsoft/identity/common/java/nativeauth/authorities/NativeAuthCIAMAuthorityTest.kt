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
package com.microsoft.identity.common.java.nativeauth.authorities

import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthV2OAuth2Strategy
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import org.junit.Assert.assertEquals
import org.junit.Test

class NativeAuthCIAMAuthorityTest {

    @Test
    fun createOAuth2Strategy_returnsV1Strategy() {
        val strategy = authority().createOAuth2Strategy(strategyParameters())

        assertEquals(NativeAuthOAuth2Strategy::class.java, strategy.javaClass)
    }

    @Test
    fun createOAuth2StrategyV2_returnsV2Strategy() {
        val strategy = authority().createOAuth2StrategyV2(strategyParameters())

        assertEquals(NativeAuthV2OAuth2Strategy::class.java, strategy.javaClass)
    }

    private fun authority() = NativeAuthCIAMAuthority(
        authorityUrl = "https://login.contoso.com/tenant",
        clientId = "client-id"
    )

    private fun strategyParameters() = OAuth2StrategyParameters.builder().build()
}
