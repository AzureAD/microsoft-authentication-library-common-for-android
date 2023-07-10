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

import com.microsoft.identity.common.java.logging.LogSession
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
        private const val SIGNUP_START_ENDPOINT_SUFFIX = "/signup/v1.0/start"
        private const val SIGNUP_CHALLENGE_ENDPOINT_SUFFIX = "/signup/v1.0/challenge"
        private const val SIGNUP_CONTINUE_ENDPOINT_SUFFIX = "/signup/v1.0/continue"
        private const val RESET_PASSWORD_START_ENDPOINT_SUFFIX = "/resetpassword/v1.0/start"
        private const val RESET_PASSWORD_CHALLENGE_ENDPOINT_SUFFIX = "/resetpassword/v1.0/challenge"
        private const val RESET_PASSWORD_CONTINUE_ENDPOINT_SUFFIX = "/resetpassword/v1.0/continue"
        private const val RESET_PASSWORD_SUBMIT_ENDPOINT_SUFFIX = "/resetpassword/v1.0/submit"
        private const val RESET_PASSWORD_COMPLETE_ENDPOINT_SUFFIX = "/resetpassword/v1.0/poll_completion"
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
        LogSession.logMethodCall(tag = TAG)
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
        LogSession.logMethodCall(tag = TAG)
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
        LogSession.logMethodCall(tag = TAG)
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
        LogSession.logMethodCall(tag = TAG)
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
        LogSession.logMethodCall(tag = TAG)
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
        LogSession.logMethodCall(tag = TAG)
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
        LogSession.logMethodCall(tag = TAG)
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
        LogSession.logMethodCall(tag = TAG)
        return getEndpointUrlFromRootAndTenantAndSuffix(
            root = getAuthorityUrl(),
            endpointSuffix = RESET_PASSWORD_COMPLETE_ENDPOINT_SUFFIX
        )
    }

    /**
     * Get the endpoint to be used for making a signin/initiate request.
     *
     * @return URL the endpoint
     */
    fun getSignInInitiateEndpoint(): URL {
        LogSession.logMethodCall(tag = TAG)
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
        LogSession.logMethodCall(tag = TAG)
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
        LogSession.logMethodCall(tag = TAG)
        return getEndpointUrlFromRootAndTenantAndSuffix(
            root = getAuthorityUrl(),
            endpointSuffix = SIGN_IN_TOKEN_ENDPOINT_SUFFIX
        )
    }

    private fun getEndpointUrlFromRootAndTenantAndSuffix(root: URL, endpointSuffix: String): URL {
        LogSession.logMethodCall(tag = TAG)
        return try {
            // TODO remove query param post TEST SLICE
            UrlUtil.appendPathToURL(root, endpointSuffix, "dc=ESTS-PUB-WUS3-AZ1-FD000-TEST1")
        } catch (e: URISyntaxException) {
            LogSession.logException(tag = TAG, throwable = e)
            throw e
        } catch (e: MalformedURLException) {
            LogSession.logException(tag = TAG, throwable = e)
            throw e
        }
    }
}
