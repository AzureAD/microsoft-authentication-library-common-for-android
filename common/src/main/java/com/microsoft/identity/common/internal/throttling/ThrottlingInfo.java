package com.microsoft.identity.common.internal.throttling;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Getter
public class ThrottlingInfo {

    private class SerializedNames {
        public static final String HTTP_STATUS_CODE = "http_status_code";
        public static final String RETRY_AFTER = "retry_after";
    }

    @SerializedName(SerializedNames.HTTP_STATUS_CODE)
    private int httpStatusCode;

    @SerializedName(SerializedNames.RETRY_AFTER)
    private long retryAfter;

    public ThrottlingInfo(int httpStatusCode, long retryAfter) {
        this.httpStatusCode = httpStatusCode;
        this.retryAfter = retryAfter;
    }
}
