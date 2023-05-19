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

package com.microsoft.identity.common.java.providers.nativeauth

import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration
import com.microsoft.identity.common.java.util.UrlUtil
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL

class NativeAuthOAuth2Configuration(
    private val authorityUrl: URL,
    val clientId: String,
    val challengeType: String,
    // Need this to decide whether or not to return mock api authority or actual authority supplied in configuration
    // Turn this on if you plan to use web auth and/or open id configuration
    // TODO remove this post-mock API
    val useRealAuthority: Boolean = false
) : MicrosoftStsOAuth2Configuration() {

    private val TAG = NativeAuthOAuth2Configuration::class.java.simpleName

    companion object {
        private const val SIGNUP_START_ENDPOINT_SUFFIX = "/signup/start"
        private const val SIGNUP_CHALLENGE_ENDPOINT_SUFFIX = "/signup/challenge"
        private const val SIGNUP_CONTINUE_ENDPOINT_SUFFIX = "/signup/continue"
        private const val SSPR_START_ENDPOINT_SUFFIX = "/resetpassword/start"
        private const val SSPR_CHALLENGE_ENDPOINT_SUFFIX = "/resetpassword/challenge"
        private const val SSPR_CONTINUE_ENDPOINT_SUFFIX = "/resetpassword/continue"
        private const val SSPR_SUBMIT_ENDPOINT_SUFFIX = "/resetpassword/submit"
        private const val SSPR_COMPLETE_ENDPOINT_SUFFIX = "/resetpassword/poll_completion"
        private const val SIGN_IN_INITIATE_ENDPOINT_SUFFIX = "/oauth2/v2.0/initiate"
        private const val SIGN_IN_CHALLENGE_ENDPOINT_SUFFIX = "/oauth2/v2.0/challenge"
        private const val SIGN_IN_TOKEN_ENDPOINT_SUFFIX = "/oauth2/v2.0/token"
    }

    override fun getAuthorityUrl(): URL {
        return if (useRealAuthority) {
            authorityUrl
        } else {
            // TODO return real authorityUrl once we move away from using mock APIs
            URL("https://native-ux-mock-api.azurewebsites.net/lumonconvergedps.onmicrosoft.com")
        }
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

    /**
     * Get the endpoint to be used for making a signup/continue request.
     *
     * @return URL the endpoint
     */
    fun getSignUpContinueEndpoint(): URL {
        return getEndpointUrlFromRootAndTenantAndSuffix(
            root = getAuthorityUrl(),
            endpointSuffix = SIGNUP_CONTINUE_ENDPOINT_SUFFIX
        )
    }

    /**
     * Get the endpoint to be used for making a self service password reset (start) request.
     *
     * @return URL the sspr start endpoint
     */
    fun getSsprStartEndpoint(): URL {
        return getEndpointUrlFromRootAndTenantAndSuffix(
            root = getAuthorityUrl(),
            endpointSuffix = SSPR_START_ENDPOINT_SUFFIX
        )
    }

    /**
     * Get the endpoint to be used for making a self service password reset (challenge) request.
     *
     * @return URL the sspr challenge endpoint
     */
    fun getSsprChallengeEndpoint(): URL {
        return getEndpointUrlFromRootAndTenantAndSuffix(
            root = getAuthorityUrl(),
            endpointSuffix = SSPR_CHALLENGE_ENDPOINT_SUFFIX
        )
    }

    /**
     * Get the endpoint to be used for making a self service password reset (continue) request.
     *
     * @return URL the sspr continue endpoint
     */
    fun getSsprContinueEndpoint(): URL {
        return getEndpointUrlFromRootAndTenantAndSuffix(
            root = getAuthorityUrl(),
            endpointSuffix = SSPR_CONTINUE_ENDPOINT_SUFFIX
        )
    }

    /**
     * Get the endpoint to be used for making a self service password reset (submit) request.
     *
     * @return URL the sspr submit endpoint
     */
    fun getSsprSubmitEndpoint(): URL {
        return getEndpointUrlFromRootAndTenantAndSuffix(
            root = getAuthorityUrl(),
            endpointSuffix = SSPR_SUBMIT_ENDPOINT_SUFFIX
        )
    }

    /**
     * Get the endpoint to be used for making a self service password reset (poll completion) request.
     *
     * @return URL the sspr poll completion endpoint
     */
    fun getSsprPollCompletionEndpoint(): URL {
        return getEndpointUrlFromRootAndTenantAndSuffix(
            root = getAuthorityUrl(),
            endpointSuffix = SSPR_COMPLETE_ENDPOINT_SUFFIX
        )
    }

    /**
     * Get the endpoint to be used for making a signin/initiate request.
     *
     * @return URL the endpoint
     */
    fun getSignInInitiateEndpoint(): URL {
        return getEndpointUrlFromRootAndTenantAndSuffix(
            root = getAuthorityUrl(),
            endpointSuffix = SIGN_IN_INITIATE_ENDPOINT_SUFFIX
        )
    }

    /**
     * Get the endpoint to be used for making a signin/challenge request.
     *
     * @return URL the endpoint
     */
    fun getSignInChallengeEndpoint(): URL {
        return getEndpointUrlFromRootAndTenantAndSuffix(
            root = getAuthorityUrl(),
            endpointSuffix = SIGN_IN_CHALLENGE_ENDPOINT_SUFFIX
        )
    }

    /**
     * Get the endpoint to be used for making a signin/token request.
     *
     * @return URL the endpoint
     */
    fun getSignInTokenEndpoint(): URL {
        return getEndpointUrlFromRootAndTenantAndSuffix(
            root = getAuthorityUrl(),
            endpointSuffix = SIGN_IN_TOKEN_ENDPOINT_SUFFIX
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
