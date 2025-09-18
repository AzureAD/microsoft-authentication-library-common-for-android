package com.microsoft.identity.common.java.commands;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * A DTO for the results of a batch AcquirePrtSsoToken request.
 */
@Builder
@Getter
@Accessors(prefix = "m")
public class AcquirePrtSsoTokenBatchResult {

    /**
     * Map of successful per-account results keyed by home account id.
     */
    @SerializedName("results")
    private final @NonNull List<AcquirePrtSsoTokenResult> mResults;

    /**
     * Map of failed accounts keyed by home account id with error message/code.
     * (Only include accounts that did not produce a full result object.)
     */
    @SerializedName("failedAccounts")
    private final @Nullable Map<String, String> mFailedAccounts;

    /**
     * Optional top-level error (e.g., service unavailable before processing any accounts).
     */
    @SerializedName("error")
    private final @Nullable String mError;

    /**
     * Correlation / request id for diagnostics.
     */
    @SerializedName("correlationId")
    private final @Nullable String mCorrelationId;

    /**
     * Authority used for the request.
     */
    @SerializedName("authority")
    private final @Nullable String mAuthority;
}