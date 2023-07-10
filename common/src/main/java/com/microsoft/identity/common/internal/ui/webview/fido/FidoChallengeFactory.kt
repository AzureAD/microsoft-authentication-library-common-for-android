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
package com.microsoft.identity.common.internal.ui.webview.fido

import com.microsoft.identity.common.java.exception.ClientException

/**
 * Instantiates FidoChallenge objects.
 */
class FidoChallengeFactory {
    /**
     * Creates a FidoChallenge from a WebView passkey redirect url.
     * @param redirectUri passkey protocol redirect url.
     * @throws ClientException if a required parameter is missing.
     */
    @Throws(ClientException::class)
    fun createFidoChallengeFromRedirect(redirectUri: String): AbstractFidoChallenge {
        //Get a map of parameters from redirectUri using UrlUtil
        //Check size of Map. If there are more than 8 entries, then it's a reg request. Otherwise, check for an auth request.
        //(This is assuming that the server will always return the same number of parameters... if this isn't the case, we'll instead have to go by the presence of certain fields.)
        //Return <create Reg/AuthFidoChallenge>
    }

    /**
     * Creates a RegFidoChallenge.
     * @param parameters fields from redirect url.
     * @return RegFidoChallenge
     * @throws ClientException if a required parameter is missing.
     */
    @Throws(ClientException::class)
    private fun createRegFidoChallenge(parameters: Map<String, String>): RegFidoChallenge {
        validateMainParameters(parameters)
        //Build challenge. Start with the "main" parameters, then validate and add the other ones.
        //Make use of RegFidoRequestField here.
        //Throw a ClientException if one of the required parameters is missing.
    }

    /**
     * Creates an AuthFidoChallenge.
     * @param parameters fields from redirect url.
     * @return AuthFidoChallenge
     * @throws ClientException if a required parameter is missing.
     */
    @Throws(ClientException::class)
    private fun createAuthFidoChallenge(parameters: Map<String, String>): AuthFidoChallenge {
        validateMainParameters(parameters)
        //Build challenge. Start with the "main" parameters, then validate and add the other ones.
        //Make use of AuthFidoRequestField here.
        //Throw a ClientException if one of the required parameters is missing.
    }

    /**
     * Validates that the fields enumerated in FidoRequestField are present.
     * @param parameters fields from redirect url.
     * @throws ClientException if a required parameter is missing.
     */
    @Throws(ClientException::class)
    private fun validateMainParameters(parameters: Map<String, String>) {
        //Make use of FidoRequestField here.
        //Throw a ClientException if one of the required parameters is missing.
    }
}