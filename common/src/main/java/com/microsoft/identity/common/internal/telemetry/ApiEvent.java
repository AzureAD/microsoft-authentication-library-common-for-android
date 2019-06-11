package com.microsoft.identity.common.internal.telemetry;

import android.support.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.logging.Logger;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

public class ApiEvent extends BaseEvent {
    private static final String TAG = ApiEvent.class.getSimpleName();
    public static final String API_ID = "api_id";
    public static final String CORRELATION_ID = "correlation_id";
    public static final String REQUEST_ID = "request_id";
    public static final String AUTHORITY_NAME = "authority";
    public static final String AUTHORITY_TYPE = "authority_type";
    public static final String AUTHORITY_VALIDATION = "authority_validation_status";
    public static final String UI_BEHAVIOR = "ui_behavior";
    public static final String WAS_SUCCESSFUL = "is_successful";
    public static final String TENANT_ID = "tenant_id";
    public static final String LOGIN_HINT = "login_hint";
    public static final String USER_ID = "user_id";
    public static final String API_ERROR_CODE = "api_error_code";

    public ApiEvent putAuthority(@NonNull final String authority) {
        super.put(AUTHORITY_NAME, sanitizeUrlForTelemetry(authority));
        return this;
    }

    /**
     * Convenience method to sanitize the url for telemetry.
     *
     * @param url the {@link URL} to sanitize.
     * @return the sanitized URL.
     */
    public static String sanitizeUrlForTelemetry(@NonNull final String url) {
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
    public static String sanitizeUrlForTelemetry(@NonNull final URL url) {
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

    public ApiEvent putUiBehavior(@NonNull final String uiBehavior) {
        super.put(UI_BEHAVIOR, uiBehavior);
        return this;
    }

    public ApiEvent putApiId(@NonNull final String apiId) {
        super.put(API_ID, apiId);
        return this;
    }

    public ApiEvent putValidationStatus(@NonNull final String validationStatus) {
        super.put(AUTHORITY_VALIDATION, validationStatus);
        return this;
    }

    public ApiEvent putLoginHint(@NonNull final String loginHint) {
        try {
            super.put(LOGIN_HINT, StringExtensions.createHash(loginHint));
        } catch (final NoSuchAlgorithmException | UnsupportedEncodingException exception) {
            Logger.warn(TAG, exception.getMessage());
        }

        return this;
    }

    public ApiEvent isApiCallSuccessful(final Boolean isSuccessful) {
        super.put(WAS_SUCCESSFUL, isSuccessful.toString());
        return this;
    }

    public ApiEvent putCorrelationId(@NonNull final String correlationId) {
        super.put(CORRELATION_ID, correlationId);
        return this;
    }

    public ApiEvent putRequestId(@NonNull final String requestId) {
        super.put(REQUEST_ID, requestId);
        return this;
    }

    public ApiEvent putApiErrorCode(@NonNull final String errorCode) {
        super.put(API_ERROR_CODE, errorCode);
        return this;
    }
}