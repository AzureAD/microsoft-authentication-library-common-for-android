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
package com.microsoft.identity.common.internal.result;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.HashMapExtensions;
import com.microsoft.identity.common.exception.ArgumentException;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.exception.IntuneAppProtectionPolicyRequiredException;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.exception.UiRequiredException;
import com.microsoft.identity.common.exception.UserCancelException;
import com.microsoft.identity.common.internal.broker.BrokerResult;
import com.microsoft.identity.common.internal.cache.SchemaUtil;
import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAccount;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.util.HeaderSerializationUtil;

import org.json.JSONException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_ACCOUNTS;


public class MsalBrokerResultAdapter implements IBrokerResultAdapter {

    private static final String TAG = MsalBrokerResultAdapter.class.getName();

    @Override
    public Bundle bundleFromAuthenticationResult(@NonNull final ILocalAuthenticationResult authenticationResult) {
        Logger.verbose(TAG, "Constructing result bundle from ILocalAuthenticationResult");

        final IAccountRecord accountRecord = authenticationResult.getAccountRecord();

        final AccessTokenRecord accessTokenRecord = authenticationResult.getAccessTokenRecord();

        final BrokerResult brokerResult = new BrokerResult.Builder()
                .accessToken(authenticationResult.getAccessToken())
                .idToken(authenticationResult.getIdToken())
                .homeAccountId(accountRecord.getHomeAccountId())
                .localAccountId(accountRecord.getLocalAccountId())
                .userName(accountRecord.getUsername())
                .tokenType(accessTokenRecord.getAccessTokenType())
                .clientId(accessTokenRecord.getClientId())
                .scope(accessTokenRecord.getTarget())
                .clientInfo(accountRecord.getClientInfo())
                .authority(accessTokenRecord.getAuthority())
                .environment(accessTokenRecord.getEnvironment())
                .tenantId(authenticationResult.getTenantId())
                .expiresOn(Long.parseLong(accessTokenRecord.getExpiresOn()))
                .extendedExpiresOn(Long.parseLong(accessTokenRecord.getExtendedExpiresOn()))
                .cachedAt(Long.parseLong(accessTokenRecord.getCachedAt()))
                .speRing(authenticationResult.getSpeRing())
                .refreshTokenAge(authenticationResult.getRefreshTokenAge())
                .success(true)
                .build();

        final Bundle resultBundle = new Bundle();
        resultBundle.putSerializable(AuthenticationConstants.Broker.BROKER_RESULT_V2, brokerResult);
        resultBundle.putBoolean(AuthenticationConstants.Broker.BROKER_REQUEST_V2_SUCCESS, true);

        return resultBundle;
    }

    @Override
    public Bundle bundleFromBaseException(@NonNull final BaseException exception) {
        Logger.verbose(TAG, "Constructing result bundle from BaseException");

        final BrokerResult.Builder builder = new BrokerResult.Builder()
                .success(false)
                .errorCode(exception.getErrorCode())
                .errorMessage(exception.getMessage())
                .correlationId(exception.getCorrelationId())
                .cliTelemErrorCode(exception.getCliTelemErrorCode())
                .cliTelemSubErrorCode(exception.getCliTelemSubErrorCode())
                .speRing(exception.getSpeRing())
                .refreshTokenAge(exception.getRefreshTokenAge());

        if (exception instanceof ServiceException) {
            builder.oauthSubErrorCode(((ServiceException) exception).getOAuthSubErrorCode())
                    .httpStatusCode(((ServiceException) exception).getHttpStatusCode())
                    .httpResponseHeaders(
                            HeaderSerializationUtil.toJson((
                                    (ServiceException) exception).getHttpResponseHeaders()
                            ))
                    .httpResponseBody(new Gson().toJson(
                            ((ServiceException) exception).getHttpResponseBody()));
        }

        if (exception instanceof IntuneAppProtectionPolicyRequiredException) {
            builder.userName(((IntuneAppProtectionPolicyRequiredException) exception).getAccountUpn())
                    .localAccountId(((IntuneAppProtectionPolicyRequiredException) exception).getAccountUserId())
                    .authority(((IntuneAppProtectionPolicyRequiredException) exception).getAuthorityUrl())
                    .tenantId(((IntuneAppProtectionPolicyRequiredException) exception).getTenantId());
        }

        final Bundle resultBundle = new Bundle();
        resultBundle.putSerializable(AuthenticationConstants.Broker.BROKER_RESULT_V2, builder.build());
        resultBundle.putBoolean(AuthenticationConstants.Broker.BROKER_REQUEST_V2_SUCCESS, false);

        return resultBundle;
    }

    @Override
    public ILocalAuthenticationResult authenticationResultFromBundle(@NonNull final Bundle resultBundle) {

        final BrokerResult brokerResult = (BrokerResult) resultBundle.getSerializable(
                AuthenticationConstants.Broker.BROKER_RESULT_V2
        );

        if (brokerResult == null) {
            Logger.error(TAG, "Broker Result not returned from Broker, ", null);
            return null;
        }

        try {
            Logger.verbose(TAG, "Broker Result returned from Bundle, constructing authentication result");

            final AccessTokenRecord accessTokenRecord = getAccessTokenRecord(brokerResult);
            final IAccountRecord accountRecord = getAccountRecord(brokerResult);
            final LocalAuthenticationResult authenticationResult = new LocalAuthenticationResult(
                    accessTokenRecord,
                    null,
                    brokerResult.getIdToken(),
                    accountRecord
            );
            return authenticationResult;
        } catch (final ServiceException e) {
            Logger.error(TAG, "Failed to parse Client Info ", e);
            return null;
        }

    }

    @Override
    public BaseException baseExceptionFromBundle(@NonNull final Bundle resultBundle) {
        Logger.verbose(TAG, "Constructing exception from result bundle");

        final BrokerResult brokerResult = (BrokerResult) resultBundle.getSerializable(
                AuthenticationConstants.Broker.BROKER_RESULT_V2
        );

        if (brokerResult == null) {
            Logger.error(TAG, "Broker Result not returned from Broker, ", null);
            return new BaseException(ErrorStrings.UNKNOWN_ERROR, "Broker Result not returned from Broker");

        }

        BaseException baseException;

        final String errorCode = brokerResult.getErrorCode();

        if (AuthenticationConstants.OAuth2ErrorCode.INTERACTION_REQUIRED.equalsIgnoreCase(errorCode) ||
                AuthenticationConstants.OAuth2ErrorCode.INVALID_GRANT.equalsIgnoreCase(errorCode)) {

            Logger.warn(TAG, "Received a UIRequired exception from Broker : " + errorCode);
            baseException = new UiRequiredException(
                    errorCode,
                    brokerResult.getErrorMessage()
            );

        } else if (AuthenticationConstants.OAuth2ErrorCode.UNAUTHORIZED_CLIENT.equalsIgnoreCase(errorCode) ||
                AuthenticationConstants.OAuth2SubErrorCode.PROTECTION_POLICY_REQUIRED.
                        equalsIgnoreCase(brokerResult.getSubErrorCode())) {

            Logger.warn(
                    TAG,
                    "Received a IntuneAppProtectionPolicyRequiredException exception from Broker : "
                            + errorCode);
            baseException = getIntuneProtectionRequiredException(brokerResult);

        } else if (ErrorStrings.USER_CANCELLED.equalsIgnoreCase(errorCode)) {

            Logger.warn(TAG, "Received a User cancelled exception from Broker : " + errorCode);
            baseException = new UserCancelException();

        } else if(ArgumentException.ILLEGAL_ARGUMENT_ERROR_CODE.equalsIgnoreCase(errorCode)) {

            Logger.warn(TAG, "Received a Argument exception from Broker : " + errorCode);
            baseException = new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME,
                    errorCode,
                    brokerResult.getErrorMessage()
            );

        } else if (!TextUtils.isEmpty(brokerResult.getHttpResponseHeaders()) ||
                !TextUtils.isEmpty(brokerResult.getHttpResponseBody())) {

            Logger.warn(TAG, "Received a Service exception from Broker : " + errorCode);
            baseException = getServiceException(brokerResult);

        } else {

            Logger.warn(TAG, "Received a Client exception from Broker : " + errorCode);
            baseException = new ClientException(
                    brokerResult.getErrorCode(),
                    brokerResult.getErrorMessage()
            );
        }

        baseException.setCliTelemErrorCode(brokerResult.getCliTelemErrorCode());
        baseException.setCliTelemSubErrorCode(brokerResult.getCliTelemSubErrorCode());
        baseException.setCorrelationId(brokerResult.getCorrelationId());
        baseException.setSpeRing(brokerResult.getSpeRing());
        baseException.setRefreshTokenAge(brokerResult.getRefreshTokenAge());

        return baseException;

    }


    /**
     * Helper to get AccessTokenRecord from BrokerResult
     */
    private AccessTokenRecord getAccessTokenRecord(@NonNull final BrokerResult brokerResult) throws ServiceException {

        final AccessTokenRecord accessTokenRecord = new AccessTokenRecord();

        try {
            final ClientInfo clientInfo = new ClientInfo(brokerResult.getClientInfo());
            accessTokenRecord.setHomeAccountId(SchemaUtil.getHomeAccountId(clientInfo));
            accessTokenRecord.setRealm(clientInfo.getUtid());

            final URL authorityUrl = new URL(brokerResult.getAuthority());
            final AzureActiveDirectoryCloud cloudEnv = AzureActiveDirectory.
                    getAzureActiveDirectoryCloud(authorityUrl);
            if (cloudEnv != null) {
                Logger.info(TAG, "Using preferred cache host name...");
                accessTokenRecord.setEnvironment(cloudEnv.getPreferredCacheHostName());
            } else {
                accessTokenRecord.setEnvironment(
                        authorityUrl.getHost()
                );
            }
        } catch (MalformedURLException e) {
            Logger.error(TAG, "Malformed authority url ", e);
        }
        accessTokenRecord.setClientId(brokerResult.getClientId());
        accessTokenRecord.setSecret(brokerResult.getAccessToken());
        accessTokenRecord.setAccessTokenType(brokerResult.getTokenType());
        accessTokenRecord.setAuthority(brokerResult.getAuthority());
        accessTokenRecord.setTarget(brokerResult.getScope());
        accessTokenRecord.setCredentialType(CredentialType.AccessToken.name());

        accessTokenRecord.setExpiresOn(
                String.valueOf(brokerResult.getExpiresOn())
        );

        accessTokenRecord.setExtendedExpiresOn(
                String.valueOf(brokerResult.getExtendedExpiresOn())
        );

        accessTokenRecord.setCachedAt(
                String.valueOf(brokerResult.getCachedAt())
        );
        return accessTokenRecord;
    }

    /**
     * Helper method to retrieve IAccountRecord from BrokerResult
     */
    private IAccountRecord getAccountRecord(@NonNull final BrokerResult brokerResult) throws ServiceException {
        final ClientInfo clientInfo = new ClientInfo(brokerResult.getClientInfo());
        final MicrosoftStsAccount microsoftStsAccount = new MicrosoftStsAccount(
                new IDToken(brokerResult.getIdToken()),
                clientInfo
        );
        microsoftStsAccount.setEnvironment(brokerResult.getEnvironment());
        return new AccountRecord(microsoftStsAccount);
    }

    /**
     * Helper method to retrieve IntuneAppProtectionPolicyRequiredException from BrokerResult
     */
    private IntuneAppProtectionPolicyRequiredException getIntuneProtectionRequiredException(
            @NonNull final BrokerResult brokerResult) {

        final IntuneAppProtectionPolicyRequiredException exception =
                new IntuneAppProtectionPolicyRequiredException(
                        brokerResult.getErrorCode(),
                        brokerResult.getErrorMessage()
                );
        exception.setTenantId(brokerResult.getTenantId());
        exception.setAuthorityUrl(brokerResult.getAuthority());
        exception.setAccountUserId(brokerResult.getLocalAccountId());
        exception.setAccountUpn(brokerResult.getUserName());
        exception.setOauthSubErrorCode(brokerResult.getSubErrorCode());
        try {
            exception.setHttpResponseBody(HashMapExtensions.jsonStringAsMap(
                    brokerResult.getHttpResponseBody())
            );
            if (brokerResult.getHttpResponseHeaders() != null) {
                exception.setHttpResponseHeaders(
                        HeaderSerializationUtil.fromJson(
                                brokerResult.getHttpResponseHeaders())
                );
            }
        } catch (JSONException e) {
            Logger.warn(TAG, "Unable to parse json");
        }
        return exception;
    }

    /**
     * Helper method to retrieve ServiceException from BrokerResult
     */
    private ServiceException getServiceException(@NonNull final BrokerResult brokerResult) {

        final ServiceException serviceException = new ServiceException(
                brokerResult.getErrorCode(),
                brokerResult.getErrorMessage(),
                null
        );
        serviceException.setOauthSubErrorCode(brokerResult.getSubErrorCode());
        try {
            serviceException.setHttpResponseBody(
                    brokerResult.getHttpResponseBody() != null ?
                            HashMapExtensions.jsonStringAsMap(
                                    brokerResult.getHttpResponseBody()) :
                            null
            );
            serviceException.setHttpResponseHeaders(
                    brokerResult.getHttpResponseHeaders() != null ?
                            HeaderSerializationUtil.fromJson(
                                    brokerResult.getHttpResponseHeaders()) :
                            null
            );

        } catch (JSONException e) {
            Logger.warn(TAG, "Unable to parse json");
        }
        return serviceException;

    }

    /**
     * Get the bundle from the AccountRecord list.
     * @param records List of AccountRecord
     * @return Bundle
     */
    public Bundle bundleFromAccountRecordList(@NonNull final List<AccountRecord> records) {
        final Bundle resultBundle = new Bundle();
        ArrayList<String> accountRecordString = new ArrayList<>();
        for (AccountRecord record : records) {
            final String recordInGson = new Gson().toJson(record, AccountRecord.class);
            accountRecordString.add(recordInGson);
        }

        resultBundle.putStringArrayList(BROKER_ACCOUNTS, accountRecordString);
        return resultBundle;
    }

    /**
     * Get the AccountRecord list from bundle.
     * @param bundle Bundle
     * @return List of AccountRecord
     */
    public static List<AccountRecord> getAccountRecordListFromBundle(@NonNull final Bundle bundle) {
        final ArrayList<String> accountsList = bundle.getStringArrayList(BROKER_ACCOUNTS);
        final List<AccountRecord> result = new ArrayList<>();
        if (accountsList == null) {
            //The bundle does not contain the BROKER_RESULT_ACCOUNTS value.
            return null;
        }

        for (final String accountJson : accountsList) {
            final AccountRecord accountRecord = new Gson().fromJson(accountJson, AccountRecord.class);
            result.add(accountRecord);
        }

        return result;
    }
}
