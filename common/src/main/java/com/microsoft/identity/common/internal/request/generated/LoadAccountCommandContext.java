package com.microsoft.identity.common.internal.request.generated;

import android.content.Context;

import com.google.auto.value.AutoValue;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.request.SdkType;

import net.jcip.annotations.Immutable;

@Immutable
@AutoValue
@AutoValue.CopyAnnotations
public abstract class LoadAccountCommandContext extends CommandContext {

    //region Black Magic for keeping specific fields out of equals
    protected Context androidApplicationContext;
    protected OAuth2TokenCache tokenCache;

    public Context androidApplicationContext() {
        return androidApplicationContext;
    }

    public OAuth2TokenCache tokenCache() {
        return tokenCache;
    }

    public Builder toBuilder() {
        Builder builder = autoToBuilder();
        builder.androidContext = this.androidApplicationContext;
        builder.tokenCache = this.tokenCache;
        return builder;
    }
    //endregion

    public static LoadAccountCommandContext.Builder builder() {
        return new AutoValue_LoadAccountCommandContext.Builder();
    }

    abstract Builder autoToBuilder();

    @AutoValue.Builder
    public abstract static class Builder {

        //region Black Magic for keeping specific fields out of equals
        private Context androidContext;
        private OAuth2TokenCache tokenCache;

        public Builder setAndroidApplicationContext(Context context) {
            androidContext = context;
            return this;
        }

        public Builder setOAuth2TokenCache(OAuth2TokenCache cache) {
            tokenCache = cache;
            return this;
        }

        public LoadAccountCommandContext build() {
            LoadAccountCommandContext x = autoBuild();
            x.androidApplicationContext = this.androidContext;
            x.tokenCache = this.tokenCache;
            return x;
        }

        abstract LoadAccountCommandContext autoBuild();
        //endregion

        public abstract Builder setApplicationName(String value);

        public abstract Builder setApplicationVersion(String value);

        public abstract Builder setRequiredBrokerProtocolVersion(String value);

        public abstract Builder setCorrelationId(String value);

        public abstract Builder setSdkType(SdkType value);

        public abstract Builder setSdkVersion(String value);

    }
}
