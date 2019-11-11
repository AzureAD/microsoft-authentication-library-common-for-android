package com.microsoft.identity.common.internal.request.generated;

import android.accounts.Account;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.dto.IAccountRecord;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * BrokerSilentTokenCommandParameters
 *
 * NOTE: Need to check to see which of these fields is nullable.  We have multiple versions of ADAL in the wild
 * which do not necessarily provide values for all fields.  We may want to consider different parameter objects for these. For
 * example 1 for ADAL and 1 for MSAL
 */
@AutoValue
@AutoValue.CopyAnnotations
public abstract class BrokerSilentTokenCommandParameters
        extends CommandParameters
        implements IScopesAddable<BrokerSilentTokenCommandParameters>{

    abstract Set<String> scopes();
    abstract IAccountRecord account();
    abstract String clientId();
    abstract String redirectUri();
    abstract Authority authority();
    @Nullable
    abstract String claimsRequestJson();
    @Nullable
    abstract Boolean forceRefresh();
    @Nullable
    abstract Account accountManagerAccount();
    @Nullable
    abstract String callerPackageName();
    abstract int callerUid();
    @Nullable
    abstract String callerAppVersion();
    @Nullable
    abstract String homeAccountId(); // Home account id be null if the request if from Adal
    abstract String localAccountId();
    @Nullable
    abstract String loginHint();
    abstract List<Pair<String, String>> extraQueryStringParameters();
    // Device state might not be propagated to MSODS yet, so we might want to wait before re-acquiring PRT.
    // TODO: Move this to context (may be nice to indicate in the name if this is seconds or miliseconds)
    abstract int sleepTimeBeforePrtAcquisition();

    public static Builder builder() {
        return new AutoValue_BrokerSilentTokenCommandParameters.Builder();
    }

    public abstract Builder toBuilder();

    public BrokerSilentTokenCommandParameters addDefaultScopes(List<String> defaultScopes){
        BrokerSilentTokenCommandParameters.Builder builder = this.toBuilder();
        Set<String> requestedScopes = this.scopes();
        requestedScopes.addAll(defaultScopes);
        // sanitize empty and null scopes
        requestedScopes.removeAll(Arrays.asList("", null));
        builder.setScopes(requestedScopes);
        return builder.build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setScopes(Set<String> value);
        public abstract Builder setAccount(IAccountRecord value);
        public abstract Builder setClientId(String value);
        public abstract Builder setRedirectUri(String value);
        public abstract Builder setAuthority(Authority value);
        public abstract Builder setClaimsRequestJson(String value);
        public abstract Builder setForceRefresh(Boolean value);
        public abstract Builder setAccountManagerAccount(Account value);
        public abstract Builder setCallerPackageName(String value);
        public abstract Builder setCallerUid(int value);
        public abstract Builder setCallerAppVersion(String value);
        public abstract Builder setHomeAccountId(String value);
        public abstract Builder setLocalAccountId(String value);
        public abstract Builder setLoginHint(String value);
        public abstract Builder setExtraQueryStringParameters(List<Pair<String, String>> value);
        public abstract Builder setSleepTimeBeforePrtAcquisition(int value);
        public abstract BrokerSilentTokenCommandParameters build();

    }

}
