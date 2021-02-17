package com.microsoft.identity.common.internal.throttling;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.providers.oauth2.TokenErrorResponse;

import lombok.Getter;

@Getter
public class ThrottlingInfo {

    private class SerializedNames {
        public static final String HTTP_STATUS_CODE = "http_status_code";
        public static final String RETRY_AFTER = "retry_after";
        public static final String TOKEN_ERROR_RESPONSE = "token_error_response";
    }

    @SerializedName(SerializedNames.HTTP_STATUS_CODE)
    private final int httpStatusCode;

    @SerializedName(SerializedNames.RETRY_AFTER)
    private final long retryAfter;

    @SerializedName(SerializedNames.TOKEN_ERROR_RESPONSE)
    private final TokenErrorResponse tokenErrorResponse;

    public ThrottlingInfo(final int httpStatusCode,
                          final long retryAfter,
                          final TokenErrorResponse tokenErrorResponse) {
        this.httpStatusCode = httpStatusCode;
        this.retryAfter = retryAfter;
        this.tokenErrorResponse = tokenErrorResponse;
    }
}
