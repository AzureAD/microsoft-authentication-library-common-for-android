package com.microsoft.identity.common.internal.commands;

import android.accounts.Account;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * A DTO for the results from an AcquirePrtSsoToken request.
 */
@Builder
@Getter
@Accessors(prefix = "m")
public class AcquirePrtSsoTokenResult {

    /**
     * Object holding auth error information
     *
     */
    @SerializedName("error")
    private final @Nullable String mError;
    /**
     * Account name.
     *
     */
    @SerializedName("account")
    private final @Nullable String mAccountName;
    /**
     * Account authority.
     */
    private final @Nullable String mAccountAuthority;
    /**
     * SSO cookie name
     *
     */
    @SerializedName("cookieName")
    private final @Nullable String mCookieName;
    /**
     * SSO cookie content
     *
     */
    @SerializedName("ssoToken")
    private final @Nullable String mCookieContent;
    /**
     * telemetry data
     *
     */
    @SerializedName("telemetry")
    private final @NonNull Map<String, Object> mTelemetry;

}
