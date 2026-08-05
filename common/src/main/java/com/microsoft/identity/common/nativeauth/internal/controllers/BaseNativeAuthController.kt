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
package com.microsoft.identity.common.nativeauth.internal.controllers

import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.cache.ICacheRecord
import com.microsoft.identity.common.java.commands.parameters.CommandParameters
import com.microsoft.identity.common.java.commands.parameters.DeviceCodeFlowCommandParameters
import com.microsoft.identity.common.java.commands.parameters.GenerateShrCommandParameters
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters
import com.microsoft.identity.common.java.commands.parameters.RemoveAccountCommandParameters
import com.microsoft.identity.common.java.commands.parameters.RopcTokenCommandParameters
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters
import com.microsoft.identity.common.java.controllers.BaseController
import com.microsoft.identity.common.java.dto.AccountRecord
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResponse
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult
import com.microsoft.identity.common.java.providers.oauth2.IAuthorizationStrategy
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache
import com.microsoft.identity.common.java.providers.oauth2.TokenResult
import com.microsoft.identity.common.java.request.SdkType
import com.microsoft.identity.common.java.result.LocalAuthenticationResult
import com.microsoft.identity.common.java.result.AcquireTokenResult
import com.microsoft.identity.common.java.result.GenerateShrResult
import com.microsoft.identity.common.java.ui.PreferredAuthMethod
import com.microsoft.identity.common.java.nativeauth.commands.parameters.BaseNativeAuthCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.BaseSignInTokenCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import com.microsoft.identity.common.java.util.ported.PropertyBag
import com.microsoft.identity.common.java.util.StringUtil
import lombok.EqualsAndHashCode
import java.net.URL

/**
 * The implementation of the basis for MSAL native authentication.
 * This class is introduced to conform with the method definition of BaseController, while none
 * of these methods are relevant for native authentication flows. Hence, calling this logic in
 * NativeAuthController will throw an exception.
 *
 * TODO: update BaseController and create a new layer in the hierarchy that contains non-native-auth
 * logic. Doing this will make [BaseNativeAuthCommand] obsolete as well.
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
abstract class BaseNativeAuthController : BaseController() {

    @Throws(ClientException::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "acquireToken() not supported in NativeAuthController"
    )
    override fun acquireToken(request: InteractiveTokenCommandParameters?): AcquireTokenResult {
        throw ClientException("acquireToken() not supported in NativeAuthController")
    }

    @Throws(ClientException::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "onFinishAuthorizationSession() not supported in NativeAuthController"
    )
    override fun onFinishAuthorizationSession(
        requestCode: Int,
        resultCode: Int,
        data: PropertyBag
    ) {
        throw ClientException("onFinishAuthorizationSession() not supported in NativeAuthController")
    }

    @Throws(ClientException::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "getAccounts() not supported in NativeAuthController"
    )
    override fun getAccounts(parameters: CommandParameters?): MutableList<ICacheRecord> {
        throw ClientException("getAccounts() not supported in NativeAuthController")
    }

    @Throws(ClientException::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "removeAccount() not supported in NativeAuthController"
    )
    override fun removeAccount(parameters: RemoveAccountCommandParameters?): Boolean {
        throw ClientException("removeAccount() not supported in NativeAuthController")
    }

    @Throws(ClientException::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "getDeviceMode() not supported in NativeAuthController"
    )
    override fun getDeviceMode(parameters: CommandParameters?): Boolean {
        throw ClientException("getDeviceMode() not supported in NativeAuthController")
    }

    @Throws(ClientException::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "getCurrentAccount() not supported in NativeAuthController"
    )
    override fun getCurrentAccount(parameters: CommandParameters?): MutableList<ICacheRecord> {
        throw ClientException("getCurrentAccount() not supported in NativeAuthController")
    }

    @Throws(ClientException::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "removeCurrentAccount() not supported in NativeAuthController"
    )
    override fun removeCurrentAccount(parameters: RemoveAccountCommandParameters?): Boolean {
        throw ClientException("removeCurrentAccount() not supported in NativeAuthController")
    }

    @Throws(ClientException::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "deviceCodeFlowAuthRequest() not supported in NativeAuthController"
    )
    override fun deviceCodeFlowAuthRequest(parameters: DeviceCodeFlowCommandParameters?): AuthorizationResult<*, *> {
        throw ClientException("deviceCodeFlowAuthRequest() not supported in NativeAuthController")
    }

    @Throws(ClientException::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "acquireDeviceCodeFlowToken() not supported in NativeAuthController"
    )
    override fun acquireDeviceCodeFlowToken(
        authorizationResult: AuthorizationResult<*, *>?,
        parameters: DeviceCodeFlowCommandParameters?
    ): AcquireTokenResult {
        throw ClientException("acquireDeviceCodeFlowToken() not supported in NativeAuthController")
    }

    @Throws(ClientException::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "generateSignedHttpRequest() not supported in NativeAuthController"
    )
    override fun generateSignedHttpRequest(parameters: GenerateShrCommandParameters?): GenerateShrResult {
        throw ClientException("generateSignedHttpRequest() not supported in NativeAuthController")
    }

    @Throws(ClientException::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "acquireTokenWithPassword() not supported in NativeAuthController"
    )
    override fun acquireTokenWithPassword(parameters: RopcTokenCommandParameters): AcquireTokenResult {
        throw ClientException("acquireTokenWithPassword() not supported in NativeAuthController")
    }

    @Throws(ClientException::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "renewAccessToken() not supported in NativeAuthController"
    )
    override fun renewAccessToken(parameters: SilentTokenCommandParameters): TokenResult {
        throw ClientException("renewAccessToken() not supported in NativeAuthController")
    }

    @Throws(ClientException::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "getStrategy() not supported in NativeAuthController"
    )
    override fun getStrategy(parameters: SilentTokenCommandParameters): OAuth2Strategy<
        *, *, out AuthorizationRequest<*>, out AuthorizationRequest.Builder<*>,
        out IAuthorizationStrategy<*, *>, *, *, *, *, *, *, *, out AuthorizationResult<*, *>> {
        throw ClientException("getStrategy() not supported in NativeAuthController")
    }

    @Throws(ClientException::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "getCacheRecord() not supported in NativeAuthController"
    )
    override fun getCacheRecord(parameters: SilentTokenCommandParameters): ICacheRecord {
        throw ClientException("getCacheRecord() not supported in NativeAuthController")
    }

    @Throws(ClientException::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "getTokenCache() not supported in NativeAuthController"
    )
    override fun getTokenCache(parameters: SilentTokenCommandParameters): OAuth2TokenCache<
        out OAuth2Strategy<*, *, *, *, *, *, *, *, *, *, *, *, *>,
        out AuthorizationRequest<*>,
        *> {
        throw ClientException("getTokenCache() not supported in NativeAuthController")
    }

    @Throws(ClientException::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "performTokenRequest() not supported in NativeAuthController"
    )
    override fun performTokenRequest(
        strategy: OAuth2Strategy<*, *, out AuthorizationRequest<*>, out AuthorizationRequest.Builder<*>, out IAuthorizationStrategy<*, *>, *, *, *, *, *, *, *, out AuthorizationResult<*, *>>,
        request: AuthorizationRequest<*>,
        response: AuthorizationResponse,
        parameters: InteractiveTokenCommandParameters
    ): TokenResult {
        throw ClientException("performTokenRequest() not supported in NativeAuthController")
    }

    @Throws(ClientException::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "idTokenIsNull() not supported in NativeAuthController"
    )
    override fun idTokenIsNull(cacheRecord: ICacheRecord, sdkType: SdkType): Boolean {
        throw ClientException("idTokenIsNull() not supported in NativeAuthController")
    }

    @Throws(ClientException::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "getCachedAccountRecordFromAllCaches() not supported in NativeAuthController"
    )
    override fun getCachedAccountRecordFromAllCaches(parameters: SilentTokenCommandParameters): AccountRecord? {
        throw ClientException("getCachedAccountRecordFromAllCaches() not supported in NativeAuthController")
    }

    @Throws(ClientException::class)
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "getPreferredAuthMethod() not supported in NativeAuthController"
    )
    override fun getPreferredAuthMethod(): PreferredAuthMethod {
        throw ClientException("getPreferredAuthMethod() not supported in NativeAuthController")
    }

    protected fun saveAndReturnTokens(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parametersWithScopes: BaseSignInTokenCommandParameters,
        tokenApiResult: SignInTokenApiResult.Success
    ): SignInCommandResult.Complete {
        LogSession.logMethodCall(
            tag = javaClass.simpleName,
            correlationId = parametersWithScopes.getCorrelationId(),
            methodName = "${javaClass.simpleName}.saveAndReturnTokens"
        )
        val records: List<ICacheRecord> = saveTokens(
            oAuth2Strategy as MicrosoftStsOAuth2Strategy,
            createAuthorizationRequest(
                strategy = oAuth2Strategy,
                scopes = parametersWithScopes.scopes ?: emptyList(),
                clientId = parametersWithScopes.clientId,
                applicationIdentifier = parametersWithScopes.applicationIdentifier
            ),
            tokenApiResult.tokenResponse,
            parametersWithScopes.oAuth2TokenCache
        )

        // The first element in the returned list is the item we *just* saved, the rest of
        // the elements are necessary to construct the full IAccount + TenantProfile
        val newestRecord = records[0]

        return SignInCommandResult.Complete(
            authenticationResult = LocalAuthenticationResult(
                finalizeCacheRecordForResult(
                    newestRecord,
                    parametersWithScopes.authenticationScheme
                ),
                records,
                SdkType.MSAL,
                false
            ),
            correlationId = tokenApiResult.correlationId
        )
    }

    protected fun createAuthorizationRequest(
        strategy: NativeAuthOAuth2Strategy,
        scopes: List<String>,
        clientId: String,
        applicationIdentifier: String
    ): MicrosoftStsAuthorizationRequest {
        LogSession.logMethodCall(
            tag = javaClass.simpleName,
            correlationId = null,
            methodName = "${javaClass.simpleName}.createAuthorizationRequest"
        )

        val builder = MicrosoftStsAuthorizationRequest.Builder()
        builder.setAuthority(URL(strategy.getAuthority()))
        builder.setClientId(clientId)
        builder.setScope(StringUtil.join(" ", scopes))
        builder.setApplicationIdentifier(applicationIdentifier)
        return builder.build()
    }

    protected fun addDefaultScopes(scopes: List<String>?): List<String> {
        LogSession.logMethodCall(
            tag = javaClass.simpleName,
            correlationId = null,
            methodName = "${javaClass.simpleName}.createAuthorizationRequest"
        )
        val requestScopes = scopes?.toMutableList() ?: mutableListOf()
        requestScopes.addAll(AuthenticationConstants.DEFAULT_SCOPES)
        // sanitize empty and null scopes
        requestScopes.removeAll(listOf("", null))
        return requestScopes.toList()
    }

    /**
     * Builds a [NativeAuthOAuth2Strategy] from [parameters]. Shared by V1 and V2 controllers so
     * neither needs to duplicate the strategy-parameter construction logic.
     */
    protected fun createNativeAuthStrategy(parameters: BaseNativeAuthCommandParameters): NativeAuthOAuth2Strategy {
        LogSession.logMethodCall(
            tag = javaClass.simpleName,
            correlationId = null,
            methodName = "${javaClass.simpleName}.createNativeAuthStrategy"
        )
        val strategyParameters = OAuth2StrategyParameters.builder()
            .platformComponents(parameters.platformComponents)
            .challengeTypes(parameters.challengeType)
            .capabilities(parameters.capabilities)
            .requestInterceptor(parameters.requestInterceptor)
            .build()
        return parameters.authority.createOAuth2Strategy(strategyParameters)
    }
}
