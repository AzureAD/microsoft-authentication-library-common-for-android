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
package com.microsoft.identity.common.nativeauth.java.providers.requests.signin

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.nativeauth.java.providers.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import java.net.URL

/**
 * Represents a request to the OAuth /initiate endpoint, and provides a create() function to instantiate the request using the provided parameters
 */
data class SignInInitiateRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthRequestSignInInitiateParameters
) : NativeAuthRequest() {

    /**
     * Returns a request object using the provided parameters.
     * The request URL and headers passed will be set directly.
     * The username, challenge type, and client ID will be mapped to the NativeAuthRequestSignInInitiateParameters object.
     *
     * Parameters that are null or empty will throw a ClientException.
     * @see com.microsoft.identity.common.java.exception.ClientException
     */
    companion object {
        fun create(
            username: String,
            clientId: String,
            challengeType: String,
            requestUrl: String,
            headers: Map<String, String?>
        ): SignInInitiateRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(username, "username")
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(challengeType, "challengeType")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return SignInInitiateRequest(
                parameters = NativeAuthRequestSignInInitiateParameters(
                    username = username,
                    challengeType = challengeType,
                    clientId = clientId
                ),
                requestUrl = URL(requestUrl),
                headers = headers
            )
        }
    }

    data class NativeAuthRequestSignInInitiateParameters(
        val username: String,
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("challenge_type") val challengeType: String
    ) : NativeAuthRequestParameters()
}
