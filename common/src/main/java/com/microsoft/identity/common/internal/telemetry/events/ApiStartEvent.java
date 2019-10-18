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
package com.microsoft.identity.common.internal.telemetry.events;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.BrokerAcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.BrokerAcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.OperationParameters;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.Event;
import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.EventType;
import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.Key;
import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.Value;

public class ApiStartEvent extends BaseEvent {
    private static final String TAG = ApiStartEvent.class.getSimpleName();

    public ApiStartEvent() {
        super();
        names(Event.API_START_EVENT);
        types(EventType.API_EVENT);
    }

    @Override
    public ApiStartEvent put(@NonNull final String propertyName, final String propertyValue) {
        super.put(propertyName, propertyValue);
        return this;
    }

    public ApiStartEvent putProperties(@NonNull final OperationParameters parameters) {
        if (parameters == null) {
            return this;
        }

        final Authority authority = parameters.getAuthority();

        if (authority != null) {
            if (authority.getAuthorityURL() != null) {
                put(Key.AUTHORITY, authority.getAuthorityURL().getAuthority()); //Pii
            }
            put(Key.AUTHORITY_TYPE, authority.getAuthorityTypeString());
        }

        if (parameters.getSdkType() != null) {
            put(Key.SDK_NAME, parameters.getSdkType().name());
        }

        put(Key.SDK_VERSION, parameters.getSdkVersion());

        put(Key.CLAIM_REQUEST, StringUtil.isEmpty(
                parameters.getClaimsRequestJson()) ? Value.FALSE : Value.TRUE
        );

        put(Key.REDIRECT_URI, parameters.getRedirectUri()); //Pii
        put(Key.CLIENT_ID, parameters.getClientId()); //Pii

        if (parameters instanceof AcquireTokenOperationParameters) {
            final AcquireTokenOperationParameters atOperationParameters = (AcquireTokenOperationParameters) parameters;

            if (atOperationParameters.getAuthorizationAgent() != null) {
                put(
                        Key.USER_AGENT,
                        atOperationParameters.getAuthorizationAgent().name()
                );
            }

            put(Key.LOGIN_HINT, //Pii
                    atOperationParameters.getLoginHint()
            );

            if (atOperationParameters.getExtraQueryStringParameters() != null) {
                put(Key.REQUEST_QUERY_PARAMS, //Pii
                        String.valueOf(atOperationParameters.getExtraQueryStringParameters().size())
                );
            }

            if (atOperationParameters.getOpenIdConnectPromptParameter() != null) {
                put(Key.PROMPT_BEHAVIOR,
                        atOperationParameters.getOpenIdConnectPromptParameter().toString()
                );
            }
        }

        if (parameters instanceof AcquireTokenSilentOperationParameters) {
            if (parameters.getAccount() != null) {
                put(Key.USER_ID, parameters.getAccount().getHomeAccountId()); //Pii
            }
            put(
                    Key.IS_FORCE_REFRESH,
                    String.valueOf(parameters.getForceRefresh())
            );
            put(
                    Key.BROKER_PROTOCOL_VERSION,
                    String.valueOf(parameters.getRequiredBrokerProtocolVersion())
            );

            if (parameters.getScopes() != null) {
                put(Key.SCOPE_SIZE, String.valueOf(parameters.getScopes().size()));
                put(Key.SCOPE, parameters.getScopes().toString()); //Pii
            }
        }

        if (parameters instanceof BrokerAcquireTokenOperationParameters) {
            //TODO when integrate the telemetry with broker.
        }

        if (parameters instanceof BrokerAcquireTokenSilentOperationParameters) {
            //TODO when integrate the telemetry with broker.
        }

        return this;
    }

    public ApiStartEvent authority(@NonNull final String authority) {
        put(Key.AUTHORITY, sanitizeUrlForTelemetry(authority));
        return this;
    }

    public ApiStartEvent putAuthorityType(@NonNull final String authorityType) {
        put(Key.AUTHORITY_TYPE, authorityType);
        return this;
    }

    public ApiStartEvent putApiId(@NonNull final String apiId) {
        put(Key.API_ID, apiId);
        return this;
    }

    public ApiStartEvent putValidationStatus(@NonNull final String validationStatus) {
        put(Key.AUTHORITY_VALIDATION_STATUS, validationStatus);
        return this;
    }

    public ApiStartEvent putLoginHint(@NonNull final String loginHint) {
        try {
            put(Key.LOGIN_HINT, StringExtensions.createHash(loginHint));
        } catch (final NoSuchAlgorithmException | UnsupportedEncodingException exception) {
            Logger.warn(TAG, exception.getMessage());
        }

        return this;
    }

    public ApiStartEvent putExtendedExpiresOnSetting(@NonNull final String extendedExpiresOnSetting) {
        put(Key.EXTENDED_EXPIRES_ON_SETTING, extendedExpiresOnSetting);
        return this;
    }

    /**
     * Convenience method to sanitize the url for telemetry.
     *
     * @param url the {@link URL} to sanitize.
     * @return the sanitized URL.
     */
    private static String sanitizeUrlForTelemetry(@NonNull final String url) {
        URL urlToSanitize = null;
        try {
            urlToSanitize = new URL(url);
        } catch (MalformedURLException e1) {
            com.microsoft.identity.common.internal.logging.Logger.errorPII(
                    TAG,
                    "Url is invalid",
                    e1
            );
        }

        return urlToSanitize == null ? null : sanitizeUrlForTelemetry(urlToSanitize);
    }

    /**
     * Sanitizes {@link URL} of tenant identifiers. B2C authorities are treated as null.
     *
     * @param url the URL to sanitize.
     * @return the sanitized URL.
     */
    private static String sanitizeUrlForTelemetry(@NonNull final URL url) {
        if (url == null) {
            return null;
        }

        final String authority = url.getAuthority();
        final String[] splitArray = url.getPath().split("/");

        final StringBuilder logPath = new StringBuilder();
        logPath.append(url.getProtocol())
                .append("://")
                .append(authority)
                .append('/');

        // we do not want to send tenant information
        // index 0 is blank
        // index 1 is tenant
        for (int i = 2; i < splitArray.length; i++) {
            logPath.append(splitArray[i]);
            logPath.append('/');
        }

        return logPath.toString();
    }
}