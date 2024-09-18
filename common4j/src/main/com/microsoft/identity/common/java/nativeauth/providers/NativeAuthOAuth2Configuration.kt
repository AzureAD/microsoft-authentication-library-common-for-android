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

package com.microsoft.identity.common.java.nativeauth.providers

import com.microsoft.identity.common.java.nativeauth.BuildValues
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration
import com.microsoft.identity.common.java.util.UrlUtil
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL

/**
 * NativeAuthOAuth2Configuration stores the parameters used for creating various Native Auth APIs
 * for Signup, SignIn and SSPR scenarios. This class also provides helper methods to generate urls
 * for those scenarios.
 */
class NativeAuthOAuth2Configuration(
    private val authorityUrl: URL,
    val clientId: String,
    val challengeType: String,
    // Need this to decide whether or not to return mock api authority or actual authority supplied in configuration
    // Turn this on if you plan to use web auth and/or open id configuration
    val useMockApiForNativeAuth: Boolean = BuildValues.shouldUseMockApiForNativeAuth(),
    // Base url for the mock API to make Native Auth calls. See the swagger at
    // $(MOCK_API_URL)/doc#/ for all possible urls
    private val MOCK_API_URL_WITH_NATIVE_AUTH_TENANT: String = BuildValues.getMockApiUrl() + "lumonconvergedps.onmicrosoft.com"
) : MicrosoftStsOAuth2Configuration() {

    private val TAG = NativeAuthOAuth2Configuration::class.java.simpleName

    companion object {
        private const val SIGNUP_START_ENDPOINT_SUFFIX = "/signup/v1.0/start"
        private const val SIGNUP_CHALLENGE_ENDPOINT_SUFFIX = "/signup/v1.0/challenge"
        private const val SIGNUP_CONTINUE_ENDPOINT_SUFFIX = "/signup/v1.0/continue"
        private const val RESET_PASSWORD_START_ENDPOINT_SUFFIX = "/resetpassword/v1.0/start"
        private const val RESET_PASSWORD_CHALLENGE_ENDPOINT_SUFFIX = "/resetpassword/v1.0/challenge"
        private const val RESET_PASSWORD_CONTINUE_ENDPOINT_SUFFIX = "/resetpassword/v1.0/continue"
        private const val RESET_PASSWORD_SUBMIT_ENDPOINT_SUFFIX = "/resetpassword/v1.0/submit"
        private const val RESET_PASSWORD_COMPLETE_ENDPOINT_SUFFIX = "/resetpassword/v1.0/poll_completion"
        private const val SIGN_IN_INITIATE_ENDPOINT_SUFFIX = "/oauth2/v2.0/initiate"
        private const val SIGN_IN_INTROSPECT_ENDPOINT_SUFFIX = "/oauth2/v2.0/introspect"
        private const val SIGN_IN_CHALLENGE_ENDPOINT_SUFFIX = "/oauth2/v2.0/challenge"
        private const val SIGN_IN_TOKEN_ENDPOINT_SUFFIX = "/oauth2/v2.0/token"
    }

    override fun getAuthorityUrl(): URL {
        return if (useMockApiForNativeAuth) {
            URL(MOCK_API_URL_WITH_NATIVE_AUTH_TENANT)
        } else {
            authorityUrl
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
     * @return URL the reset password start endpoint
     */
    fun getResetPasswordStartEndpoint(): URL {
        return getEndpointUrlFromRootAndTenantAndSuffix(
            root = getAuthorityUrl(),
            endpointSuffix = RESET_PASSWORD_START_ENDPOINT_SUFFIX
        )
    }

    /**
     * Get the endpoint to be used for making a self service password reset (challenge) request.
     *
     * @return URL the reset password challenge endpoint
     */
    fun getResetPasswordChallengeEndpoint(): URL {
        return getEndpointUrlFromRootAndTenantAndSuffix(
            root = getAuthorityUrl(),
            endpointSuffix = RESET_PASSWORD_CHALLENGE_ENDPOINT_SUFFIX
        )
    }

    /**
     * Get the endpoint to be used for making a self service password reset (continue) request.
     *
     * @return URL the reset password continue endpoint
     */
    fun getResetPasswordContinueEndpoint(): URL {
        return getEndpointUrlFromRootAndTenantAndSuffix(
            root = getAuthorityUrl(),
            endpointSuffix = RESET_PASSWORD_CONTINUE_ENDPOINT_SUFFIX
        )
    }

    /**
     * Get the endpoint to be used for making a self service password reset (submit) request.
     *
     * @return URL the reset password submit endpoint
     */
    fun getResetPasswordSubmitEndpoint(): URL {
        return getEndpointUrlFromRootAndTenantAndSuffix(
            root = getAuthorityUrl(),
            endpointSuffix = RESET_PASSWORD_SUBMIT_ENDPOINT_SUFFIX
        )
    }

    /**
     * Get the endpoint to be used for making a self service password reset (poll completion) request.
     *
     * @return URL the reset password poll completion endpoint
     */
    fun getResetPasswordPollCompletionEndpoint(): URL {
        return getEndpointUrlFromRootAndTenantAndSuffix(
            root = getAuthorityUrl(),
            endpointSuffix = RESET_PASSWORD_COMPLETE_ENDPOINT_SUFFIX
        )
    }

    /**
     * Get the endpoint to be used for making an oauth2/v2.0/initiate request.
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
     * Get the endpoint to be used for making an oauth2/v2.0/challenge request.
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
     * Get the endpoint to be used for making an oauth2/v2.0/introspect request.
     *
     * @return URL the endpoint
     */
    fun getSignInIntrospectEndpoint(): URL {
        return getEndpointUrlFromRootAndTenantAndSuffix(
            root = getAuthorityUrl(),
            endpointSuffix = SIGN_IN_INTROSPECT_ENDPOINT_SUFFIX
        )
    }

    /**
     * Get the endpoint to be used for making an oauth2/v2.0/token request.
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
        return try {
//            UrlUtil.appendPathToURL(root, endpointSuffix)
            UrlUtil.appendPathAndQueryToURL(root, endpointSuffix, "dc=ESTS-PUB-NEULR1-AZ1-FD000-TEST1")
//            UrlUtil.appendPathAndQueryToURL(root, endpointSuffix, "dc=ESTS-PUB-NEULR1-AZ1-FD000-TEST1")
//            if (BuildValues.getDC().isNotEmpty()) {
//                UrlUtil.appendPathAndQueryToURL(root, endpointSuffix, "dc=ESTS-PUB-SCUS-LZ1-FD000-TEST1")
//            } else {
//                UrlUtil.appendPathToURL(root, endpointSuffix)
//            }
        } catch (e: URISyntaxException) {
            Logger.error(TAG, "appendPathToURL failed", e)
            throw e
        } catch (e: MalformedURLException) {
            Logger.error(TAG, "appendPathToURL failed", e)
            throw e
        }
    }
}
