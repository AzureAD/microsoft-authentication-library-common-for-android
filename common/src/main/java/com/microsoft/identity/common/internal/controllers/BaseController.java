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
package com.microsoft.identity.common.internal.controllers;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.SchemaUtil;
import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.IResult;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.BrokerAcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.OperationParameters;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.request.generated.CommandParameters;
import com.microsoft.identity.common.internal.request.generated.GetCurrentAccountCommandContext;
import com.microsoft.identity.common.internal.request.generated.GetCurrentAccountCommandParameters;
import com.microsoft.identity.common.internal.request.generated.GetDeviceModeCommandContext;
import com.microsoft.identity.common.internal.request.generated.GetDeviceModeCommandParameters;
import com.microsoft.identity.common.internal.request.generated.IContext;
import com.microsoft.identity.common.internal.request.generated.IScopesAddable;
import com.microsoft.identity.common.internal.request.generated.ITokenRequestParameters;
import com.microsoft.identity.common.internal.request.generated.InteractiveTokenCommandContext;
import com.microsoft.identity.common.internal.request.generated.LoadAccountCommandContext;
import com.microsoft.identity.common.internal.request.generated.LoadAccountCommandParameters;
import com.microsoft.identity.common.internal.request.generated.RemoveAccountCommandContext;
import com.microsoft.identity.common.internal.request.generated.RemoveAccountCommandParameters;
import com.microsoft.identity.common.internal.request.generated.RemoveCurrentAccountCommandContext;
import com.microsoft.identity.common.internal.request.generated.RemoveCurrentAccountCommandParameters;
import com.microsoft.identity.common.internal.request.generated.SilentTokenCommandContext;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * BaseController is overridden in MSAL and in the Broker.  Controllers are based on the MVC pattern and are responsible
 * for orchestrating the business logic associated with a command.
 *
 * @param <GenericInteractiveTokenCommandParameters>
 * @param <GenericSilentTokenCommandParameters>
 */
public abstract class BaseController<
        GenericInteractiveTokenCommandParameters extends CommandParameters,
        GenericSilentTokenCommandParameters extends CommandParameters> {

    private static final String TAG = BaseController.class.getSimpleName();

    public abstract AcquireTokenResult acquireToken(final InteractiveTokenCommandContext context,
                                                    final GenericInteractiveTokenCommandParameters request)
            throws Exception;

    public abstract void completeAcquireToken(
            final int requestCode,
            final int resultCode,
            final Intent data
    );

    public abstract AcquireTokenResult acquireTokenSilent(final SilentTokenCommandContext context,
                                                          final GenericSilentTokenCommandParameters request)
            throws Exception;

    public abstract List<ICacheRecord> getAccounts(final LoadAccountCommandContext context,
                                                   final LoadAccountCommandParameters parameters)
            throws Exception;

    public abstract boolean removeAccount(final RemoveAccountCommandContext context,
                                          final RemoveAccountCommandParameters parameters)
            throws Exception;

    public abstract boolean getDeviceMode(final GetDeviceModeCommandContext context,
                                          final GetDeviceModeCommandParameters parameters)
            throws Exception;

    public abstract List<ICacheRecord> getCurrentAccount(final GetCurrentAccountCommandContext context,
                                                         final GetCurrentAccountCommandParameters parameters)
            throws Exception;

    public abstract boolean removeCurrentAccount(final RemoveCurrentAccountCommandContext context,
                                                 final RemoveCurrentAccountCommandParameters parameters)
            throws Exception;

    /**
     * Pre-filled ALL the fields in AuthorizationRequest.Builder
     */
    protected abstract AuthorizationRequest.Builder initializeAuthorizationRequestBuilder(@NonNull final AuthorizationRequest.Builder builder,
                                                                                          @NonNull final GenericInteractiveTokenCommandParameters parameters);

    protected abstract AuthorizationRequest getAuthorizationRequest(@NonNull final OAuth2Strategy strategy,
                                                                    @NonNull final GenericInteractiveTokenCommandParameters parameters);

    protected abstract TokenResult performTokenRequest(@NonNull final OAuth2Strategy strategy,
                                                       @NonNull final AuthorizationRequest request,
                                                       @NonNull final AuthorizationResponse response,
                                                       @NonNull final InteractiveTokenCommandContext context)
            throws IOException, ClientException;

    protected abstract void renewAccessToken(@NonNull final SilentTokenCommandContext context,
                                             @NonNull final GenericSilentTokenCommandParameters parameters,
                                             @NonNull final AcquireTokenResult acquireTokenSilentResult,
                                             @NonNull final OAuth2TokenCache tokenCache,
                                             @NonNull final OAuth2Strategy strategy,
                                             @NonNull final ICacheRecord cacheRecord)
            throws IOException, ClientException;

    /**
     * Log IResult objects.  IResult objects are returned from Authorization and Token Requests
     *
     * @param tag    The log tag to use.
     * @param result The result object to log.
     */
    public static void logResult(@NonNull final String tag,
                                 @NonNull final IResult result) {
        final String TAG = tag + ":" + result.getClass().getSimpleName();

        if (result.getSuccess()) {
            Logger.info(
                    TAG,
                    "Success Result"
            );

            logExposedFieldsOfObject(TAG, result.getSuccessResponse());
        } else {
            Logger.warn(
                    TAG,
                    "Failure Result"
            );

            if (result.getErrorResponse() != null) {
                if (result.getErrorResponse().getError() != null) {
                    Logger.warn(
                            TAG,
                            "Error: " + result.getErrorResponse().getError()
                    );
                }

                if (result.getErrorResponse().getErrorDescription() != null) {
                    Logger.warnPII(
                            TAG,
                            "Description: " + result.getErrorResponse().getErrorDescription()
                    );
                }

                logExposedFieldsOfObject(TAG, result.getErrorResponse());
            }
        }

        if (result instanceof AuthorizationResult) {
            AuthorizationResult authResult = (AuthorizationResult) result;

            if (authResult.getAuthorizationStatus() != null) {
                Logger.info(
                        TAG,
                        "Authorization Status: " + authResult.getAuthorizationStatus().toString()
                );
            }
        }
    }

    /**
     * Log parameters objects passed to controllers
     *
     * @param tag        The log tag to use.
     * @param parameters The request parameters to log.
     */
    protected void logParameters(String tag, Object parameters) {
        final String TAG = tag + ":" + parameters.getClass().getSimpleName();

        if (Logger.getAllowPii()) {
            Logger.infoPII(TAG, ObjectMapper.serializeObjectToJsonString(parameters));
        } else {
            Logger.info(TAG, ObjectMapper.serializeExposedFieldsOfObjectToJsonString(parameters));
        }
    }

    protected static void logExposedFieldsOfObject(@NonNull final String tag,
                                                   @NonNull final Object object) {
        final String TAG = tag + ":" + object.getClass().getSimpleName();
        Logger.info(TAG, ObjectMapper.serializeExposedFieldsOfObjectToJsonString(object));
    }

    protected abstract TokenResult performSilentTokenRequest(
            @NonNull final OAuth2Strategy strategy,
            @NonNull final RefreshTokenRecord refreshTokenRecord,
            @NonNull final IContext context,
            @NonNull final ITokenRequestParameters parameters)
            throws ClientException, IOException;


    protected List<ICacheRecord> saveTokens(@NonNull final OAuth2Strategy strategy,
                                            @NonNull final AuthorizationRequest request,
                                            @NonNull final TokenResponse tokenResponse,
                                            @NonNull final OAuth2TokenCache tokenCache) throws ClientException {
        final String methodName = ":saveTokens";

        Logger.info(
                TAG + methodName,
                "Saving tokens..."
        );

        return tokenCache.saveAndLoadAggregatedAccountData(
                strategy,
                request,
                tokenResponse
        );
    }

    protected boolean refreshTokenIsNull(@NonNull final ICacheRecord cacheRecord) {
        return null == cacheRecord.getRefreshToken();
    }

    protected boolean accessTokenIsNull(@NonNull final ICacheRecord cacheRecord) {
        return null == cacheRecord.getAccessToken();
    }

    protected boolean idTokenIsNull(@NonNull final ICacheRecord cacheRecord,
                                    @NonNull final SdkType sdkType) {
        final IdTokenRecord idTokenRecord = (sdkType == SdkType.ADAL) ?
                cacheRecord.getV1IdToken() : cacheRecord.getIdToken();

        return null == idTokenRecord;
    }

    protected CommandParameters addDefaultScopes(List<String> scopes, IScopesAddable parameters) {
        return parameters.addDefaultScopes(scopes);
    }

    protected abstract AccountRecord getCachedAccountRecord(
            SilentTokenCommandContext context,
            GenericSilentTokenCommandParameters parameters) throws ClientException;

    /**
     * Helper method which returns false if the tenant id of the authority
     * doesn't match with the tenant of the Access token for AADAuthority.
     *
     * Returns true otherwise.
     */
    protected boolean isRequestAuthorityRealmSameAsATRealm(@NonNull final Authority requestAuthority,
                                                           @NonNull final AccessTokenRecord accessTokenRecord)
            throws ServiceException, ClientException {
        if(requestAuthority instanceof AzureActiveDirectoryAuthority){

            String tenantId = ((AzureActiveDirectoryAuthority) requestAuthority).getAudience().getTenantId();

            if(AzureActiveDirectoryAudience.isHomeTenantAlias(tenantId)) {
                // if realm on AT and home account's tenant id do not match, we have a token for guest and
                // requested authority here is for home, so return false we need to refresh the token
                final String utidFromHomeAccountId = accessTokenRecord
                        .getHomeAccountId()
                        .split(Pattern.quote("."))[1];

                return utidFromHomeAccountId.equalsIgnoreCase(accessTokenRecord.getRealm());

            }else {
                tenantId = ((AzureActiveDirectoryAuthority) requestAuthority)
                        .getAudience()
                        .getTenantUuidForAlias(requestAuthority.getAuthorityURL().toString());
                return tenantId.equalsIgnoreCase(accessTokenRecord.getRealm());
            }
        }
        return true;
    }

    protected boolean isMsaAccount(final MicrosoftTokenResponse microsoftTokenResponse) {
        final String tenantId = SchemaUtil.getTenantId(
                microsoftTokenResponse.getClientInfo(),
                microsoftTokenResponse.getIdToken()
        );
        return AzureActiveDirectoryAudience.MSA_MEGA_TENANT_ID.equalsIgnoreCase(tenantId);
    }

}
