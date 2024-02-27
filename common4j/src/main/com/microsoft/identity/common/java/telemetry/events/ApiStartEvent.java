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
package com.microsoft.identity.common.java.telemetry.events;

import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Event;
import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.EventType;
import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Key;
import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Value;

import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.commands.parameters.BrokerInteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.BrokerSilentTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.TokenCommandParameters;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.StringUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Nullable;

import lombok.NonNull;

@Deprecated
public class ApiStartEvent extends BaseEvent {
    private static final String TAG = ApiStartEvent.class.getSimpleName();

    public ApiStartEvent() {
        super();
        names(Event.API_START_EVENT);
        types(EventType.API_EVENT);
    }

    public ApiStartEvent(@NonNull final String apiId) {
        super();
        names(Event.API_START_EVENT);
        types(EventType.API_EVENT);
        putApiId(apiId);
    }

    @Override
    public ApiStartEvent put(@NonNull final String propertyName, final String propertyValue) {
        super.put(propertyName, propertyValue);
        return this;
    }

    public ApiStartEvent putBrokerVersion(final String brokerVersion) {
        put(Key.BROKER_VERSION, brokerVersion);
        return this;
    }

    public ApiStartEvent putProperties(
            @Nullable final CommandParameters parameters) {
        if (parameters == null) {
            return this;
        }

        if (parameters.getSdkType() != null) {
            put(Key.SDK_NAME, parameters.getSdkType().name());
        }

        put(Key.SDK_VERSION, parameters.getSdkVersion());

        put(Key.REDIRECT_URI, parameters.getRedirectUri()); //Pii
        put(Key.CLIENT_ID, parameters.getClientId()); //Pii

        put(
                Key.BROKER_PROTOCOL_VERSION,
                String.valueOf(parameters.getRequiredBrokerProtocolVersion())
        );

        if (parameters instanceof TokenCommandParameters) {

            final TokenCommandParameters tokenCommandParameters = (TokenCommandParameters) parameters;

            final Authority authority = tokenCommandParameters.getAuthority();

            if (authority != null) {
                if (authority.getAuthorityURL() != null) {
                    put(Key.AUTHORITY, authority.getAuthorityURL().getAuthority()); //Pii
                }
                put(Key.AUTHORITY_TYPE, authority.getAuthorityTypeString());
            }

            put(Key.CLAIM_REQUEST, StringUtil.isNullOrEmpty(
                    tokenCommandParameters.getClaimsRequestJson()) ? Value.FALSE : Value.TRUE
            );

            if (tokenCommandParameters.getScopes() != null) {
                put(Key.SCOPE_SIZE, String.valueOf(tokenCommandParameters.getScopes().size()));
                put(Key.SCOPE, tokenCommandParameters.getScopes().toString()); //Pii
            }

            final AbstractAuthenticationScheme authScheme = tokenCommandParameters.getAuthenticationScheme();

            if (null != authScheme) {
                put(Key.AUTHENTICATION_SCHEME, authScheme.getName());
            }

        }

        if (parameters instanceof InteractiveTokenCommandParameters) {
            final InteractiveTokenCommandParameters atOperationParameters = (InteractiveTokenCommandParameters) parameters;

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

            if (atOperationParameters.getPrompt() != null) {
                put(Key.PROMPT_BEHAVIOR,
                        atOperationParameters.getPrompt().toString()
                );
            }
        }

        if (parameters instanceof SilentTokenCommandParameters) {

            final SilentTokenCommandParameters silentParameters = (SilentTokenCommandParameters) parameters;

            if (silentParameters.getAccount() != null) {
                put(Key.USER_ID, silentParameters.getAccount().getHomeAccountId()); //Pii
            }
            put(
                    Key.IS_FORCE_REFRESH,
                    String.valueOf(silentParameters.isForceRefresh())
            );
        }

        if (parameters instanceof BrokerInteractiveTokenCommandParameters) {
            final BrokerInteractiveTokenCommandParameters interactiveParameters = (BrokerInteractiveTokenCommandParameters) parameters;

            put(Key.BROKER_PROTOCOL_VERSION, interactiveParameters.getNegotiatedBrokerProtocolVersion());

            put(Key.CALLER_APP_VERSION, interactiveParameters.getCallerAppVersion()); // oii
            put(Key.CALLER_APP_PACKAGE_NAME, interactiveParameters.getCallerPackageName()); // oii
            put(Key.CALLER_APP_UUID, String.valueOf(interactiveParameters.getCallerUid())); // oii
        }

        if (parameters instanceof BrokerSilentTokenCommandParameters) {
            final BrokerSilentTokenCommandParameters silentParameters = (BrokerSilentTokenCommandParameters) parameters;

            put(Key.BROKER_PROTOCOL_VERSION, silentParameters.getNegotiatedBrokerProtocolVersion());

            put(Key.CALLER_APP_VERSION, silentParameters.getCallerAppVersion()); // oii
            put(Key.CALLER_APP_PACKAGE_NAME, silentParameters.getCallerPackageName()); // oii
            put(Key.CALLER_APP_UUID, String.valueOf(silentParameters.getCallerUid())); // oii
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

    public ApiStartEvent putWorkPlaceJoined(final boolean isWorkPlaceJoined) {
        put(Key.IS_WPJ_JOINED, String.valueOf(isWorkPlaceJoined));
        return this;
    }

    public ApiStartEvent putLoginHint(@NonNull final String loginHint) {
        final String methodTag = TAG + ":putLoginHint";
        try {
            put(Key.LOGIN_HINT, StringUtil.createHash(loginHint));
        } catch (final NoSuchAlgorithmException exception) {
            Logger.warn(methodTag, exception.getMessage());
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
            Logger.errorPII(
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
