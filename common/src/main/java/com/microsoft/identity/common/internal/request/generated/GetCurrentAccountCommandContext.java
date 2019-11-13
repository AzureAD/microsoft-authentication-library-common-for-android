package com.microsoft.identity.common.internal.request.generated;

import android.content.Context;

import com.google.auto.value.AutoValue;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.request.SdkType;

import net.jcip.annotations.Immutable;

@Immutable
@AutoValue
@AutoValue.CopyAnnotations
public abstract class GetCurrentAccountCommandContext extends CommandContext {

    public abstract Boolean isSharedDevice();

    //region Black Magic for keeping specific fields out of equals
    protected Context androidApplicationContext;
    protected OAuth2TokenCache tokenCache;

    public Context androidApplicationContext() {
        return androidApplicationContext;
    }

    public OAuth2TokenCache tokenCache() {
        return tokenCache;
    }

    public GetCurrentAccountCommandContext.Builder toBuilder() {
        GetCurrentAccountCommandContext.Builder builder = autoToBuilder();
        builder.androidContext = this.androidApplicationContext;
        builder.tokenCache = this.tokenCache;
        return builder;
    }
    //endregion

    public static GetCurrentAccountCommandContext.Builder builder() {
        return new AutoValue_GetCurrentAccountCommandContext.Builder();
    }

    abstract GetCurrentAccountCommandContext.Builder autoToBuilder();

    public LoadAccountCommandContext toLoadAccountCommandContext() {
        LoadAccountCommandContext.Builder builder = LoadAccountCommandContext.builder();
        return builder.setAndroidApplicationContext(this.androidApplicationContext)
                .setApplicationName(this.applicationName())
                .setApplicationVersion(this.applicationVersion())
                .setCorrelationId(this.correlationId())
                .setOAuth2TokenCache(this.tokenCache())
                .setRequiredBrokerProtocolVersion(this.requiredBrokerProtocolVersion())
                .setSdkType(this.sdkType())
                .setSdkVersion(this.sdkVersion())
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        //region Black Magic for keeping specific fields out of equals
        private Context androidContext;
        private OAuth2TokenCache tokenCache;

        public GetCurrentAccountCommandContext.Builder setAndroidApplicationContext(Context context) {
            androidContext = context;
            return this;
        }

        public GetCurrentAccountCommandContext.Builder setTokenCache(OAuth2TokenCache cache) {
            tokenCache = cache;
            return this;
        }


        public GetCurrentAccountCommandContext build() {
            GetCurrentAccountCommandContext x = autoBuild();
            x.androidApplicationContext = this.androidContext;
            x.tokenCache = this.tokenCache;
            return x;
        }

        abstract GetCurrentAccountCommandContext autoBuild();
        //endregion

        public abstract Builder setApplicationName(String value);

        public abstract Builder setApplicationVersion(String value);

        public abstract Builder setRequiredBrokerProtocolVersion(String value);

        public abstract Builder setCorrelationId(String value);

        public abstract Builder setSdkType(SdkType value);

        public abstract Builder setSdkVersion(String value);

        public abstract Builder setIsSharedDevice(Boolean value);

    }
}
