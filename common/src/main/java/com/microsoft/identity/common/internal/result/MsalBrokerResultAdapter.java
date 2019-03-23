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

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ServiceException;
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

import java.net.MalformedURLException;
import java.net.URL;


// TODO : To be implemented.
public class MsalBrokerResultAdapter implements IBrokerResultAdapter {

    private static final String TAG = MsalBrokerResultAdapter.class.getName();

    @Override
    public Bundle bundleFromAuthenticationResult(ILocalAuthenticationResult authenticationResult) {
        return null;
    }

    @Override
    public Bundle bundleFromBaseException(BaseException exception) {
        return null;
    }

    @Override
    public ILocalAuthenticationResult authenticationResultFromBundle(@NonNull final Bundle resultBundle) {
        final BrokerResult brokerResult = (BrokerResult) resultBundle.getSerializable(
                AuthenticationConstants.Broker.BROKER_RESULT_V2
        );
        if(brokerResult == null) {
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
           Logger.error(TAG, "Failed to parse Client Info " , e);
           return null;
        }



    }

    @Override
    public BaseException baseExceptionFromBundle(Bundle resultBundle) {
        return null;
    }

    /**
     * Helper to get AccessTokenRecord from BrokerResult
     * @param brokerResult
     * @return
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
            Logger.error(TAG , "Malformed authority url ", e);
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

    private IAccountRecord getAccountRecord(@NonNull final BrokerResult brokerResult) throws ServiceException {
        final ClientInfo clientInfo = new ClientInfo(brokerResult.getClientInfo());
        final MicrosoftStsAccount microsoftStsAccount = new MicrosoftStsAccount(
                new IDToken(brokerResult.getIdToken()),
                clientInfo
        );
        microsoftStsAccount.setEnvironment(brokerResult.getEnvironment());
        return new AccountRecord(microsoftStsAccount);
    }

}
