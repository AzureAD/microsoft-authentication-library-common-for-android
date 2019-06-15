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
package com.microsoft.identity.common.internal.telemetry;

import android.support.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.logging.Logger;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.*;

public class ApiEvent extends BaseEvent {
    private static final String TAG = ApiEvent.class.getSimpleName();

    public ApiEvent() {
        super();
        putEventName(TELEMETRY_EVENT_API_EVENT);
        put(TELEMETRY_KEY_API_EVENT_COUNT, "0");
    }

    public ApiEvent put(final ApiEvent newApiEvent) {
        super.put(newApiEvent);
        //increate the event count
        put(TELEMETRY_KEY_API_EVENT_COUNT, String.valueOf(Integer.parseInt(this.getProperties().get(TELEMETRY_KEY_API_EVENT_COUNT)) + 1));
        return this;
    }

    public ApiEvent putAuthority(@NonNull final String authority) {
        super.put(TELEMETRY_KEY_AUTHORITY, sanitizeUrlForTelemetry(authority));
        return this;
    }

    public ApiEvent putAuthorityType(@NonNull final String authorityType) {
        put(TELEMETRY_KEY_AUTHORITY_TYPE, authorityType);
        return this;
    }

    public ApiEvent putUiBehavior(@NonNull final String uiBehavior) {
        super.put(TELEMETRY_KEY_UI_BEHAVIOR, uiBehavior);
        return this;
    }

    public ApiEvent putApiId(@NonNull final String apiId) {
        super.put(TELEMETRY_KEY_API_ID, apiId);
        return this;
    }

    public ApiEvent putValidationStatus(@NonNull final String validationStatus) {
        super.put(TELEMETRY_KEY_AUTHORITY_VALIDATION_STATUS, validationStatus);
        return this;
    }

    public ApiEvent putLoginHint(@NonNull final String loginHint) {
        try {
            super.put(TELEMETRY_KEY_LOGIN_HINT, StringExtensions.createHash(loginHint));
        } catch (final NoSuchAlgorithmException | UnsupportedEncodingException exception) {
            Logger.warn(TAG, exception.getMessage());
        }

        return this;
    }

    public ApiEvent putExtendedExpiresOnSetting(@NonNull final  String extendedExpiresOnSetting) {
        put(TELEMETRY_KEY_EXTENDED_EXPIRES_ON_SETTING, extendedExpiresOnSetting);
        return this;
    }

    public ApiEvent isApiCallSuccessful(final Boolean isSuccessful) {
        super.put(TELEMETRY_KEY_IS_SUCCESSFUL, isSuccessful.toString());
        return this;
    }

    public ApiEvent putCorrelationId(@NonNull final String correlationId) {
        super.put(TELEMETRY_KEY_CORRELATION_ID, correlationId);
        return this;
    }

    public ApiEvent putRequestId(@NonNull final String requestId) {
        super.put(TELEMETRY_KEY_REQUEST_ID, requestId);
        return this;
    }

    public ApiEvent putApiErrorCode(@NonNull final String errorCode) {
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