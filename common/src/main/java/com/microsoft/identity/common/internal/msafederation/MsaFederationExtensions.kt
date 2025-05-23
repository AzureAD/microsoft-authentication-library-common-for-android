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
@file:JvmName("MsaFederationExtensions")
package com.microsoft.identity.common.internal.msafederation

import com.microsoft.identity.common.internal.msafederation.google.SignInWithGoogleCredential
import com.microsoft.identity.common.java.commands.parameters.BrokerInteractiveTokenCommandParameters
import java.util.AbstractMap

/**
 * Helper/Extension method to create header that can be used in MSA authorization
 */
fun SignInWithGoogleCredential.getIdProviderHeadersForAuthorization(): Map<String, String> {
    return mapOf(MsaFederationConstants.MSA_ID_TOKEN_HEADER_KEY to this.idToken)
}

/**
 * Helper/Extension method to create query parameter that can be used in MSA authorization
 */
fun SignInWithGoogleCredential.getIdProviderExtraQueryParamForAuthorization(): Map.Entry<String, String> {
    return AbstractMap.SimpleEntry(MsaFederationConstants.MSA_ID_PROVIDER_EXTRA_QUERY_PARAM_KEY, signInProviderName.getIdProviderName())
}

/**
 * Helper/Extension method to check if the interactive flow is using Sign-in With Google.
 */
fun BrokerInteractiveTokenCommandParameters.isSignInWithGoogleFlow(): Boolean {
    return this.requestHeaders?.containsKey(MsaFederationConstants.MSA_ID_TOKEN_HEADER_KEY) ?: false
}
