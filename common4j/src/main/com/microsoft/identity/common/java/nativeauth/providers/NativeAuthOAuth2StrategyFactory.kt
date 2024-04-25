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

package com.microsoft.identity.common.java.nativeauth.providers

import com.microsoft.identity.common.java.nativeauth.providers.interactors.ResetPasswordInteractor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.SignInInteractor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.SignUpInteractor
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters

/**
 * Factory class that takes care of the creation of NativeAuthOAuth2Strategy.
 */
class NativeAuthOAuth2StrategyFactory {
    companion object {
        fun createStrategy(
            config: NativeAuthOAuth2Configuration,
            strategyParameters: OAuth2StrategyParameters,
        ): NativeAuthOAuth2Strategy {
            return NativeAuthOAuth2Strategy(
                strategyParameters = strategyParameters,
                config = config,
                signInInteractor = SignInInteractor(
                    httpClient = UrlConnectionHttpClient.getDefaultInstance(),
                    nativeAuthRequestProvider = NativeAuthRequestProvider(config = config),
                    nativeAuthResponseHandler = NativeAuthResponseHandler()
                ),
                signUpInteractor = SignUpInteractor(
                    httpClient = UrlConnectionHttpClient.getDefaultInstance(),
                    nativeAuthRequestProvider = NativeAuthRequestProvider(config = config),
                    nativeAuthResponseHandler = NativeAuthResponseHandler()
                ),
                resetPasswordInteractor = ResetPasswordInteractor(
                    httpClient = UrlConnectionHttpClient.getDefaultInstance(),
                    nativeAuthRequestProvider = NativeAuthRequestProvider(config = config),
                    nativeAuthResponseHandler = NativeAuthResponseHandler()
                )
            )
        }
    }
}
