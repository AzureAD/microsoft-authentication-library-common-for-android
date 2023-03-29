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

package com.microsoft.identity.common.internal.providers.oauth2.nativeauth

import com.microsoft.identity.common.java.BaseAccount
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme
import com.microsoft.identity.common.java.cache.ICacheRecord
import com.microsoft.identity.common.java.commands.parameters.RopcTokenCommandParameters
import com.microsoft.identity.common.java.dto.IAccountRecord
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.net.HttpResponse
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAccessToken
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsRefreshToken
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse
import com.microsoft.identity.common.java.providers.oauth2.AccessToken
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResultFactory
import com.microsoft.identity.common.java.providers.oauth2.IAuthorizationStrategy
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Configuration
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import com.microsoft.identity.common.java.providers.oauth2.RefreshToken
import com.microsoft.identity.common.java.providers.oauth2.TokenResult
import java.net.URI
import java.util.concurrent.Future

/**
 * The implementation of native authentication API oauth2 client.
 * This class is introduced to conform with the (generics) definition of OAuth2Strategy, while the
 * native authentication API doesn't conform with the OAuth2 pattern. This class is abstracts away
 * the unnecessary logic of OAuth2Strategy from [NativeAuthOAuth2Strategy].
 *
 * Many parts of this class are irrelevant for native authentication flows. Hence, they have been
 * marked as deprecated (and hidden), so they can't be referenced outside this class.
 *
 * TODO: update OAuth2Strategy and create a new layer in the hierarchy, which OAuth2Strategy
 * inherits from, and which BaseNativeAuthOAuth2Strategy is a sibling of.
 */
// TODO StrategyParameters are not needed? Check with MSAL
abstract class BaseNativeAuthOAuth2Strategy<GenericAccessToken : AccessToken,
    GenericAccount : BaseAccount,
    GenericOAuth2Configuration : OAuth2Configuration,
    GenericRefreshToken : RefreshToken,
    GenericTokenResult : TokenResult>(
    config: GenericOAuth2Configuration,
    strategyParameters: OAuth2StrategyParameters
) :
    MicrosoftStsOAuth2Strategy(config as MicrosoftStsOAuth2Configuration, strategyParameters) {

    @Throws(Exception::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "getAuthorizationResultFactory() not supported in NativeAuthOAuth2Strategy"
    )
    override fun getAuthorizationResultFactory(): AuthorizationResultFactory<
        out AuthorizationResult<*, *>,
        out AuthorizationRequest<*>> {
        throw ClientException(
            "getAuthorizationResultFactory() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Throws(Exception::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "getIssuerCacheIdentifier() not supported in NativeAuthOAuth2Strategy"
    )
    override fun getIssuerCacheIdentifier(request: MicrosoftStsAuthorizationRequest): String {
        throw ClientException(
            "getIssuerCacheIdentifier() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Throws(Exception::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "getAccessTokenFromResponse() not supported in NativeAuthOAuth2Strategy"
    )
    override fun getAccessTokenFromResponse(response: MicrosoftStsTokenResponse): MicrosoftStsAccessToken {
        throw ClientException(
            "getAccessTokenFromResponse() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Throws(Exception::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "getRefreshTokenFromResponse() not supported in NativeAuthOAuth2Strategy"
    )
    override fun getRefreshTokenFromResponse(response: MicrosoftStsTokenResponse): MicrosoftStsRefreshToken {
        throw ClientException(
            "getRefreshTokenFromResponse() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Throws(Exception::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "createAuthorizationRequestBuilder() not supported in NativeAuthOAuth2Strategy"
    )
    override fun createAuthorizationRequestBuilder(): MicrosoftStsAuthorizationRequest.Builder {
        throw ClientException(
            "createAuthorizationRequestBuilder() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Throws(Exception::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "createAuthorizationRequestBuilder() not supported in NativeAuthOAuth2Strategy"
    )
    override fun createAuthorizationRequestBuilder(account: IAccountRecord?):
        MicrosoftStsAuthorizationRequest.Builder {
        throw ClientException(
            "createAuthorizationRequestBuilder() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Throws(Exception::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "createRopcTokenRequest() not supported in NativeAuthOAuth2Strategy"
    )
    override fun createTokenRequest(
        request: MicrosoftStsAuthorizationRequest,
        response: MicrosoftStsAuthorizationResponse,
        authScheme: AbstractAuthenticationScheme
    ): MicrosoftStsTokenRequest {
        throw ClientException(
            "createTokenRequest() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Throws(Exception::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "createRopcTokenRequest() not supported in NativeAuthOAuth2Strategy"
    )
    override fun createRefreshTokenRequest(authScheme: AbstractAuthenticationScheme):
        MicrosoftStsTokenRequest {
        throw ClientException(
            "createTokenRequest() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Throws(Exception::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "createRopcTokenRequest() not supported in NativeAuthOAuth2Strategy"
    )
    override fun createRopcTokenRequest(tokenCommandParameters: RopcTokenCommandParameters):
        MicrosoftStsTokenRequest {
        throw ClientException(
            "createRopcTokenRequest() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Throws(Exception::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "validateAuthorizationRequest() not supported in NativeAuthOAuth2Strategy"
    )
    override fun validateAuthorizationRequest(request: MicrosoftStsAuthorizationRequest?) {
        throw ClientException(
            "validateAuthorizationRequest() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Throws(Exception::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "validateTokenRequest() not supported in NativeAuthOAuth2Strategy"
    )
    override fun validateTokenRequest(request: MicrosoftStsTokenRequest?) {
        throw ClientException(
            "validateTokenRequest() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Throws(Exception::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "getTokenResultFromHttpResponse() not supported in NativeAuthOAuth2Strategy"
    )
    override fun getTokenResultFromHttpResponse(response: HttpResponse): TokenResult {
        throw ClientException(
            "getTokenResultFromHttpResponse() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Throws(Exception::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "validateTokenResponse() not supported in NativeAuthOAuth2Strategy"
    )
    override fun validateTokenResponse(
        request: MicrosoftStsTokenRequest,
        response: MicrosoftStsTokenResponse
    ) {
        throw ClientException(
            "validateTokenResponse() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "requestAuthorization() not supported in NativeAuthOAuth2Strategy"
    )
    override fun requestAuthorization(
        request: MicrosoftStsAuthorizationRequest,
        authorizationStrategy: IAuthorizationStrategy<*, *>
    ): Future<AuthorizationResult<*, *>> {
        throw ClientException(
            "requestAuthorization() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "requestToken() not supported in NativeAuthOAuth2Strategy"
    )
    override fun requestToken(request: MicrosoftStsTokenRequest?): GenericTokenResult {
        throw ClientException(
            "requestToken() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "performTokenRequest() not supported in NativeAuthOAuth2Strategy"
    )
    override fun performTokenRequest(request: MicrosoftStsTokenRequest?): HttpResponse {
        throw ClientException(
            "performTokenRequest() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "getTokenEndpoint() not supported in NativeAuthOAuth2Strategy"
    )
    override fun getTokenEndpoint(): String {
        throw ClientException(
            "getTokenEndpoint() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "getRequestBody() not supported in NativeAuthOAuth2Strategy"
    )
    override fun getRequestBody(request: MicrosoftStsTokenRequest?): String {
        throw ClientException(
            "getRequestBody() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "getAuthorityFromTokenEndpoint() not supported in NativeAuthOAuth2Strategy"
    )
    override fun getAuthorityFromTokenEndpoint(): String {
        throw ClientException(
            "getAuthorityFromTokenEndpoint() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "getDeviceCode() not supported in NativeAuthOAuth2Strategy"
    )
    override fun getDeviceCode(
        request: MicrosoftStsAuthorizationRequest
    ): AuthorizationResult<*, *> {
        throw ClientException(
            "getDeviceCode() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "getIssuer() not supported in NativeAuthOAuth2Strategy"
    )
    override fun getIssuer(): URI {
        throw ClientException(
            "getIssuer() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }

    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "validateCachedResult() not supported in NativeAuthOAuth2Strategy"
    )
    override fun validateCachedResult(
        authScheme: AbstractAuthenticationScheme,
        cacheRecord: ICacheRecord
    ): Boolean {
        throw ClientException(
            "validateCachedResult() not supported in " +
                "NativeAuthOAuth2Strategy"
        )
    }
}
