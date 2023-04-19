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

package com.microsoft.identity.common.internal.cache

import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.commands.parameters.TokenCommandParameters
import com.microsoft.identity.common.java.dto.AccessTokenRecord
import com.microsoft.identity.common.java.dto.AccountRecord
import com.microsoft.identity.common.java.dto.CredentialType
import com.microsoft.identity.common.java.dto.IdTokenRecord
import com.microsoft.identity.common.java.dto.RefreshTokenRecord
import com.microsoft.identity.common.java.exception.ServiceException
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAccount
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftRefreshToken
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.ClientInfo
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest
import com.microsoft.identity.common.java.request.SdkType
import com.microsoft.identity.common.java.util.SchemaUtil
import com.microsoft.identity.common.java.util.StringUtil
import java.util.Arrays
import java.util.concurrent.TimeUnit

// TODO check if these are the credential record types we need
class NativeAuthAccountCredentialAdapter : BaseNativeAuthCredentialAdapter<
    MicrosoftAccount,
    MicrosoftRefreshToken>() {

    private val TAG = NativeAuthAccountCredentialAdapter::class.java.simpleName

    override fun createAccount(
        strategy: NativeAuthOAuth2Strategy,
        request: MicrosoftStsAuthorizationRequest,
        response: MicrosoftStsTokenResponse
    ): AccountRecord {
        Logger.verbose(TAG, "Creating Account")
        return AccountRecord(strategy.createAccount(response))
    }

    override fun createAccessToken(
        strategy: NativeAuthOAuth2Strategy,
        request: MicrosoftStsAuthorizationRequest,
        response: MicrosoftStsTokenResponse
    ): AccessTokenRecord {
        Logger.verbose(TAG, "Creating Access Token")
        return try {
            val cachedAt: Long = getCachedAt()
            val expiresOn: Long = getExpiresOn(response)
            val refreshOn: Long = getRefreshOn(response)
            val clientInfo = ClientInfo(
                response.clientInfo
            )
            val accessToken = AccessTokenRecord()
            // Required fields
            accessToken.credentialType = getCredentialType(
                StringUtil.sanitizeNull(
                    response.tokenType
                )
            )
            accessToken.homeAccountId = SchemaUtil.getHomeAccountId(clientInfo)
            accessToken.realm = getRealm(strategy, response)
            // TODO fix when using a real AAD environment. Cache identifier lookup will fail with
            // mock APIs
            accessToken.environment = strategy.getIssuerCacheIdentifierFromAuthority()
            accessToken.clientId = request.clientId
            accessToken.target = getTarget(
                request.scope,
                response.scope
            )
            accessToken.cachedAt = cachedAt.toString() // generated @ client side
            accessToken.expiresOn = expiresOn.toString()
            accessToken.refreshOn = refreshOn.toString()
            accessToken.secret = response.accessToken

            // Optional fields
            accessToken.extendedExpiresOn = getExtendedExpiresOn(response)
            accessToken.authority = strategy.getAuthority()
            accessToken.accessTokenType = response.tokenType

            accessToken
        } catch (e: ServiceException) {
            // TODO handle this properly
            throw RuntimeException(e)
        }
    }

    override fun createRefreshToken(
        strategy: NativeAuthOAuth2Strategy,
        request: MicrosoftStsAuthorizationRequest,
        response: MicrosoftStsTokenResponse
    ): RefreshTokenRecord {
        Logger.verbose(TAG, "Creating Refresh Token")
        return try {
            val cachedAt = getCachedAt()
            val clientInfo = ClientInfo(
                response.clientInfo
            )
            val refreshToken = RefreshTokenRecord()
            // Required
            refreshToken.credentialType = CredentialType.RefreshToken.name
            refreshToken.environment = strategy.getIssuerCacheIdentifierFromAuthority()
            refreshToken.homeAccountId = SchemaUtil.getHomeAccountId(clientInfo)
            refreshToken.clientId = request.clientId
            refreshToken.secret = response.refreshToken

            // Optional
            refreshToken.familyId = response.familyId
            refreshToken.target = getTarget(
                request.scope,
                response.scope
            )

            // TODO are these needed? Expected?
            refreshToken.cachedAt = cachedAt.toString() // generated @ client side
            refreshToken
        } catch (e: ServiceException) {
            // TODO handle this properly
            throw java.lang.RuntimeException(e)
        }
    }

    override fun createIdToken(
        strategy: NativeAuthOAuth2Strategy,
        request: MicrosoftStsAuthorizationRequest,
        response: MicrosoftStsTokenResponse
    ): IdTokenRecord {
        Logger.verbose(TAG, "Creating ID Token")
        return try {
            val clientInfo = ClientInfo(response.clientInfo)
            val idToken = IdTokenRecord()
            // Required fields
            idToken.homeAccountId = SchemaUtil.getHomeAccountId(clientInfo)
            idToken.environment = strategy.getIssuerCacheIdentifierFromAuthority()
            idToken.realm = getRealm(strategy, response)
            idToken.credentialType = SchemaUtil.getCredentialTypeFromVersion(
                response.idToken
            )
            idToken.clientId = request.clientId
            idToken.secret = response.idToken
            idToken.authority = strategy.getAuthority()
            idToken
        } catch (e: ServiceException) {
            // TODO handle this properly
            throw java.lang.RuntimeException(e)
        }
    }

    override fun asRefreshToken(refreshToken: MicrosoftRefreshToken?): RefreshTokenRecord {
        TODO("Not yet implemented")
    }

    override fun asAccount(account: MicrosoftAccount?): AccountRecord {
        TODO("Not yet implemented")
    }

    override fun asIdToken(
        account: MicrosoftAccount?,
        refreshToken: MicrosoftRefreshToken?
    ): IdTokenRecord {
        TODO("Not yet implemented")
    }

    // TODO make original functions in MicrosoftStsAccountCredentialAdapter accessible and reusable
    private fun getRealm(
        strategy: NativeAuthOAuth2Strategy,
        tokenResponse: MicrosoftStsTokenResponse
    ): String? {
        val msAccount = strategy.createAccount(tokenResponse)
        return msAccount.realm
    }

    /**
     * Returns the correct target based on whether the default scopes were returned or not
     *
     * @param requestScope The request scope to parse.
     * @param responseScope The response scope to parse.
     * @return The target containing default scopes.
     */
    private fun getTarget(
        requestScope: String,
        responseScope: String
    ): String? {
        return if (StringUtil.isNullOrEmpty(responseScope)) {
            val scopesToCache = StringBuilder()
            // The response scopes were empty -- per https://tools.ietf.org/html/rfc6749#section-3.3
            // we are going to fall back to a the request scopes minus any default scopes....
            val requestScopes = requestScope.split("\\s+".toRegex()).toTypedArray()
            val requestScopeSet: MutableSet<String> = HashSet(Arrays.asList(*requestScopes))
            requestScopeSet.removeAll(AuthenticationConstants.DEFAULT_SCOPES)
            for (scope in requestScopeSet) {
                scopesToCache.append(scope).append(' ')
            }
            scopesToCache.toString().trim { it <= ' ' }
        } else {
            responseScope
        }
    }

    private fun getExtendedExpiresOn(response: MicrosoftStsTokenResponse): String {
        val currentTimeMillis = System.currentTimeMillis()
        val currentTimeSecs = TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis)
        val extExpiresIn = response.extExpiresIn ?: 0
        return (currentTimeSecs + extExpiresIn).toString()
    }

    private fun getCredentialType(tokenType: String): String {
        // Assume default behavior; that token is of 'Bearer' auth scheme.
        val type = CredentialType.AccessToken.name
        return if (TokenRequest.TokenType.POP.equals(tokenType, ignoreCase = true)) {
            CredentialType.AccessToken_With_AuthScheme.name
        } else {
            type
        }
    }

    private fun getCachedAt(): Long {
        val currentTimeMillis = System.currentTimeMillis()
        return TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis)
    }

    private fun getExpiresOn(msTokenResponse: MicrosoftStsTokenResponse): Long {
        val currentTimeMillis = System.currentTimeMillis()
        val currentTimeSecs = TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis)
        val expiresIn = msTokenResponse.expiresIn
        return currentTimeSecs + expiresIn
    }

    private fun getRefreshOn(msTokenResponse: MicrosoftStsTokenResponse): Long {
        val currentTimeMillis = System.currentTimeMillis()
        val currentTimeSecs = TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis)
        val refreshIn =
            if (msTokenResponse.refreshIn == null) msTokenResponse.expiresIn else msTokenResponse.refreshIn
        return currentTimeSecs + refreshIn
    }

    override fun createAccountRecord(
        parameters: TokenCommandParameters?,
        sdkType: SdkType?,
        response: MicrosoftStsTokenResponse?
    ): AccountRecord {
        TODO("not implemented")
    }

    override fun createAccessTokenRecord(
        parameters: TokenCommandParameters?,
        accountRecord: AccountRecord?,
        response: MicrosoftStsTokenResponse?
    ): AccessTokenRecord {
        TODO("not implemented")
    }

    override fun createRefreshTokenRecord(
        parameters: TokenCommandParameters?,
        accountRecord: AccountRecord?,
        response: MicrosoftStsTokenResponse?
    ): RefreshTokenRecord {
        TODO("not implemented")
    }

    override fun createIdTokenRecord(
        parameters: TokenCommandParameters?,
        accountRecord: AccountRecord?,
        response: MicrosoftStsTokenResponse?
    ): IdTokenRecord {
        TODO("not implemented")
    }
}
