//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts

import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration
import com.microsoft.identity.common.java.util.UrlUtil
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL

class NativeAuthOAuth2Configuration(
    private val authorityUrl: URL,
    val clientId: String,
    val challengeTypes: String = "oob password redirect" // TODO hardcoded for now
) : MicrosoftStsOAuth2Configuration() {

    private val TAG = NativeAuthOAuth2Configuration::class.java.simpleName

    companion object {
        private const val SIGNUP_START_ENDPOINT_SUFFIX = "/signup/start"
        private const val SIGNUP_CHALLENGE_ENDPOINT_SUFFIX = "/signup/challenge"
    }

    override fun getAuthorityUrl(): URL {
        // TODO return real authorityUrl once we move away from using mock APIs
        return URL("https://devexclientauthsdkmockapi.azure-api.net/v1.0/lumonconvergedps.onmicrosoft.com")
    }

    /**
     * Get the endpoint to be used for making a signup/start request.
     *
     * @return URL the endpoint
     */
    fun getSignUpStartEndpoint(): URL {
        return getEndpointUrlFromRootAndTenantAndSuffix(
            root = getAuthorityUrl(),
            endpointSuffix = SIGNUP_START_ENDPOINT_SUFFIX
        )
    }

    /**
     * Get the endpoint to be used for making a signup/challenge request.
     *
     * @return URL the endpoint
     */
    fun getSignUpChallengeEndpoint(): URL {
        return getEndpointUrlFromRootAndTenantAndSuffix(
            root = getAuthorityUrl(),
            endpointSuffix = SIGNUP_CHALLENGE_ENDPOINT_SUFFIX
        )
    }

    private fun getEndpointUrlFromRootAndTenantAndSuffix(root: URL, endpointSuffix: String): URL {
        val methodName = ":getEndpointUrlFromRootAndClientIdAndSuffix"
        return try {
            UrlUtil.appendPathToURL(root, endpointSuffix)
        } catch (e: URISyntaxException) {
            Logger.error(
                TAG + methodName,
                "Unable to create URL from provided root and suffix.",
                null
            )
            Logger.errorPII(
                TAG + methodName,
                e.message +
                    " Unable to create URL from provided root and suffix." +
                    " root = $root suffix = $endpointSuffix",
                e
            )
            throw e
        } catch (e: MalformedURLException) {
            Logger.error(
                TAG + methodName,
                "Unable to create URL from provided root and suffix.",
                null
            )
            Logger.errorPII(
                TAG + methodName,
                (
                    e.message +
                        " Unable to create URL from provided root, tenant and suffix." +
                        " root = $root suffix = $endpointSuffix"
                    ),
                e
            )
            throw e
        }
    }
}
