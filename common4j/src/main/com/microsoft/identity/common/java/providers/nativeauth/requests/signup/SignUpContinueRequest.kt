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
package com.microsoft.identity.common.java.providers.nativeauth.requests.signup

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.providers.nativeauth.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import java.net.URL

/**
 * Represents a request to the Sign Up /continue endpoint, and provides a create() function to instantiate the request using the provided parameters.
 */
data class SignUpContinueRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthRequestSignUpContinueParameters
) : NativeAuthRequest() {

    companion object {
        /**
         * Returns a request object using the provided parameters.
         * The request URL and headers passed will be set directly.
         * The provided parameters based on the grant type will be mapped to the NativeAuthRequestSignUpContinueParameters object.
         *
         * Based on the grant type, the respective parameter (password, attributes, oob) will be validated,
         * and if null or empty will throw a ClientException.
         * @see com.microsoft.identity.common.java.exception.ClientException
         */
        fun create(
            password: String? = null,
            attributes: Map<String, String>? = null,
            oob: String? = null,
            clientId: String,
            signUpToken: String,
            grantType: String,
            requestUrl: String,
            headers: Map<String, String?>
        ): SignUpContinueRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(signUpToken, "signUpToken")
            ArgUtils.validateNonNullArg(grantType, "grantType")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")
            if (grantType == "oob") {
                ArgUtils.validateNonNullArg(oob, "oob")
            }
            if (grantType == "password") {
                ArgUtils.validateNonNullArg(password, "password")
            }
            if (grantType == "attributes") {
                ArgUtils.validateNonNullArg(attributes, "attributes")
            }

            return SignUpContinueRequest(
                parameters = NativeAuthRequestSignUpContinueParameters(
                    password = password,
                    attributes = attributes?.toJsonString(attributes),
                    oob = oob,
                    clientId = clientId,
                    signUpToken = signUpToken,
                    grantType = grantType
                ),
                requestUrl = URL(requestUrl),
                headers = headers
            )
        }
    }

    data class NativeAuthRequestSignUpContinueParameters(
        val password: String?,
        val attributes: String? = null,
        val oob: String?,
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("signup_token") val signUpToken: String,
        @SerializedName("grant_type") val grantType: String
    ) : NativeAuthRequestParameters()
}
