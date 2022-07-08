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
package com.microsoft.identity.common.java.commands.parameters;

import com.microsoft.identity.common.java.broker.IBrokerAccount;
import com.microsoft.identity.common.java.cache.BrokerOAuth2TokenCache;
import com.microsoft.identity.common.java.exception.ArgumentException;
import com.microsoft.identity.common.java.request.BrokerRequestType;
import com.microsoft.identity.common.java.util.StringUtil;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class BrokerRopcTokenCommandParameters extends RopcTokenCommandParameters implements IBrokerTokenCommandParameters {
    private final String callerPackageName;
    private final int callerUid;
    private final String callerAppVersion;
    private final String brokerVersion;

    private final IBrokerAccount brokerAccount;
    private final String homeAccountId;
    private final String localAccountId;
    private final int sleepTimeBeforePrtAcquisition;

    private final String negotiatedBrokerProtocolVersion;
    private final BrokerRequestType requestType;
    private final String tenantIdRequestingBrt;

    public static BrokerRopcTokenCommandParameters from(@NonNull final TokenCommandParameters tokenCommandParameters) {
        if (!(tokenCommandParameters instanceof IBrokerTokenCommandParameters)) {
            throw new IllegalArgumentException("parameters is not of type BrokerRopcTokenCommandParameters");
        }

        final IBrokerTokenCommandParameters brokerTokenCommandParameters = (IBrokerTokenCommandParameters) tokenCommandParameters;

        return BrokerRopcTokenCommandParameters.builder()
                .account(tokenCommandParameters.getAccount())
                .scopes(tokenCommandParameters.getScopes())
                .authority(tokenCommandParameters.getAuthority())
                .claimsRequestJson(tokenCommandParameters.getClaimsRequestJson())
                .authenticationScheme(tokenCommandParameters.getAuthenticationScheme())
                .forceRefresh(tokenCommandParameters.isForceRefresh())
                .loginHint(tokenCommandParameters.getLoginHint())
                .extraOptions(tokenCommandParameters.getExtraOptions())
                .platformComponents(tokenCommandParameters.getPlatformComponents())
                .oAuth2TokenCache(tokenCommandParameters.getOAuth2TokenCache())
                .isSharedDevice(tokenCommandParameters.isSharedDevice())
                .applicationName(tokenCommandParameters.getApplicationName())
                .applicationVersion(tokenCommandParameters.getApplicationVersion())
                .requiredBrokerProtocolVersion(tokenCommandParameters.getRequiredBrokerProtocolVersion())
                .sdkType(tokenCommandParameters.getSdkType())
                .sdkVersion(tokenCommandParameters.getSdkVersion())
                .clientId(tokenCommandParameters.getClientId())
                .redirectUri(tokenCommandParameters.getRedirectUri())
                .powerOptCheckEnabled(tokenCommandParameters.isPowerOptCheckEnabled())
                .flightInformation(tokenCommandParameters.getFlightInformation())
                .correlationId(tokenCommandParameters.getCorrelationId())
                .callerUid(brokerTokenCommandParameters.getCallerUid())
                .callerPackageName(brokerTokenCommandParameters.getCallerPackageName())
                .callerAppVersion(brokerTokenCommandParameters.getCallerAppVersion())
                .brokerVersion(brokerTokenCommandParameters.getBrokerVersion())
                .brokerAccount(brokerTokenCommandParameters.getBrokerAccount())
                .homeAccountId(brokerTokenCommandParameters.getHomeAccountId())
                .localAccountId(brokerTokenCommandParameters.getLocalAccountId())
                .requestType(brokerTokenCommandParameters.getRequestType())
                .tenantIdRequestingBrt(brokerTokenCommandParameters.getTenantIdRequestingBrt())
                .build();
    }

    @Override
    public void validate() throws ArgumentException {
        if (getAuthority() == null) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_WITH_PASSWORD_OPERATION_NAME,
                    "mAuthority", "Authority Url is not set"
            );
        }
        if (getScopes() == null || getScopes().isEmpty()) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_WITH_PASSWORD_OPERATION_NAME,
                    "mScopes", "Scope or resource is not set"
            );
        }
        if (StringUtil.isNullOrEmpty(getClientId())) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_WITH_PASSWORD_OPERATION_NAME,
                    "mClientId", "Client Id is not set"
            );
        }

        // If the request type is BROKER_RT_REQUEST, it means the caller here would be broker itself so
        // calling package name and calling uid will be null, otherwise we need to validate that these are
        // not null for successfully storing tokens in cache.
        if (!isRequestFromBroker()) {
            if (callerUid == 0) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME,
                        "mCallerUId", "Caller Uid is not set"
                );
            }
            if (StringUtil.isNullOrEmpty(callerPackageName)) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME,
                        "mCallerPackageName", "Caller package name is not set"
                );
            }
            if (!(getOAuth2TokenCache() instanceof BrokerOAuth2TokenCache)) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME,
                        "AcquireTokenSilentOperationParameters",
                        "OAuth2Cache not an instance of BrokerOAuth2TokenCache"
                );
            }
            if (getSdkType().isCapableOfMSA() &&
                    !getPlatformComponents().getPlatformUtil().isValidCallingApp(getRedirectUri(), getCallerPackageName())) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME,
                        "mRedirectUri", "The redirect URI doesn't match the uri" +
                        " generated with caller package name and signature"
                );
            }
        }
    }
}
