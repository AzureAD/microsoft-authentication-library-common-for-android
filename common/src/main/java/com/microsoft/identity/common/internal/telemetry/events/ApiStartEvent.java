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

import android.support.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
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

import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.*;

public class ApiStartEvent extends BaseEvent {
    private static final String TAG = ApiStartEvent.class.getSimpleName();

    public ApiStartEvent() {
        super();
        names(TELEMETRY_EVENT_API_EVENT_START);
    }

    public ApiStartEvent putProperties(OperationParameters parameters) {
        /*
        private Set<String> mScopes;
    protected IAccountRecord mAccount;
    @Expose()
    private String clientId;
    @Expose()
    private String redirectUri;
    @Expose()
    private Authority mAuthority;
    @Expose()
    private String mClaimsRequestJson;
    @Expose()
    private SdkType mSdkType = SdkType.MSAL; // default value where we get a v2 id token;
    @Expose()
    private String mSdkVersion;
    @Expose()
    private String mApplicationName;
    @Expose()
    private String mApplicationVersion;
         */
        this.put(TELEMETRY_KEY_AUTHORITY, parameters.getAuthority().toString());
        this.put(TELEMETRY_KEY_AUTHORITY_TYPE, parameters.getAuthority().getAuthorityTypeString());
        this.put(TELEMETRY_KEY_APPLICATION_NAME, parameters.getApplicationName());
        this.put(TELEMETRY_KEY_APPLICATION_VERSION, parameters.getApplicationVersion());
        this.put(TELEMETRY_KEY_SDK_NAME, parameters.getSdkType().name());
        this.put(TELEMETRY_KEY_SDK_VERSION, parameters.getSdkVersion());
        this.put(TELEMETRY_KEY_CLAIM_REQUEST, StringUtil.isEmpty(
                parameters.getClaimsRequestJson())? TELEMETRY_VALUE_NO : TELEMETRY_VALUE_YES
        );
        this.put(TELEMETRY_KEY_REDIRECT_URI, parameters.getRedirectUri());
        this.put(TELEMETRY_KEY_CLIENT_ID, parameters.getClientId());
        this.put(
                TELEMETRY_KEY_LOGIN_HINT,
                parameters.getAccount() == null ? TELEMETRY_VALUE_NOT_FOUND : parameters.getAccount().getHomeAccountId()
        );
        this.put(TELEMETRY_KEY_SCOPE_SIZE, String.valueOf(parameters.getScopes().size()));
        this.put(TELEMETRY_KEY_SCOPE, parameters.getScopes().toString());

        if (parameters instanceof AcquireTokenOperationParameters) {
            //TODO
        }

        if (parameters instanceof AcquireTokenSilentOperationParameters) {
            //TODO
        }

        if (parameters instanceof BrokerAcquireTokenOperationParameters) {
            //TODO
        }

        if (parameters instanceof BrokerAcquireTokenSilentOperationParameters) {
            //TODO
        }

        return this;
    }

    public ApiStartEvent authority(@NonNull final String authority) {
        super.put(TELEMETRY_KEY_AUTHORITY, sanitizeUrlForTelemetry(authority));
        return this;
    }

    public ApiStartEvent putAuthorityType(@NonNull final String authorityType) {
        put(TELEMETRY_KEY_AUTHORITY_TYPE, authorityType);
        return this;
    }

    public ApiStartEvent putUiBehavior(@NonNull final String uiBehavior) {
        super.put(TELEMETRY_KEY_UI_BEHAVIOR, uiBehavior);
        return this;
    }

    public ApiStartEvent putApiId(@NonNull final String apiId) {
        super.put(TELEMETRY_KEY_API_ID, apiId);
        return this;
    }

    public ApiStartEvent putValidationStatus(@NonNull final String validationStatus) {
        super.put(TELEMETRY_KEY_AUTHORITY_VALIDATION_STATUS, validationStatus);
        return this;
    }

    public ApiStartEvent putLoginHint(@NonNull final String loginHint) {
        try {
            super.put(TELEMETRY_KEY_LOGIN_HINT, StringExtensions.createHash(loginHint));
        } catch (final NoSuchAlgorithmException | UnsupportedEncodingException exception) {
            Logger.warn(TAG, exception.getMessage());
        }

        return this;
    }

    public ApiStartEvent putExtendedExpiresOnSetting(@NonNull final  String extendedExpiresOnSetting) {
        put(TELEMETRY_KEY_EXTENDED_EXPIRES_ON_SETTING, extendedExpiresOnSetting);
        return this;
    }

    public ApiStartEvent isApiCallSuccessful(final Boolean isSuccessful) {
        super.put(TELEMETRY_KEY_IS_SUCCESSFUL, isSuccessful.toString());
        return this;
    }

    public ApiStartEvent putRequestId(@NonNull final String requestId) {
        super.put(TELEMETRY_KEY_REQUEST_ID, requestId);
        return this;
    }

    public ApiStartEvent putApiErrorCode(@NonNull final String errorCode) {
        super.put(TELEMETRY_KEY_API_ERROR_CODE, errorCode);
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