package com.microsoft.identity.common.java.telemetry.relay;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data()
@Accessors(prefix = "m")
public class TelemetryRelayClientException extends Exception {
    private static final long serialVersionUID = -1543623857511895210L;

    public static final String INITIALIZATION_FAILED = "initialization_failed";

    private String mErrorCode;

    public TelemetryRelayClientException(final @Nullable String message, final @Nonnull Throwable cause, final @Nonnull String errorCode) {
        super(message, cause);
        this.mErrorCode = errorCode;
    }

    public TelemetryRelayClientException(final @Nonnull Throwable cause, final @Nonnull String errorCode) {
        this(null, cause, errorCode);
    }
}
