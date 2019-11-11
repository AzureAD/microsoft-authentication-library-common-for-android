package com.microsoft.identity.common.internal.request.generated;

import com.google.auto.value.AutoValue;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.request.SdkType;

import net.jcip.annotations.Immutable;

@Immutable
@AutoValue
@AutoValue.CopyAnnotations
public abstract class LoadAccountCommandContext extends CommandContext {

    protected OAuth2TokenCache tokenCache;

    public OAuth2TokenCache tokenCache() {
        return tokenCache;
    }

    public Builder toBuilder(){
        Builder builder = autoToBuilder();
        builder.tokenCache = this.tokenCache;
        return builder;
    }
    //endregion

    abstract Builder autoToBuilder();

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder setApplicationName(String value);
        public abstract Builder setApplicationVersion(String value);
        public abstract Builder setRequiredBrokerProtocolVersion(String value);
        public abstract Builder setCorrelationId(String value);
        public abstract Builder setSdkType(SdkType value);
        public abstract Builder setSdkVersion(String value);

        private OAuth2TokenCache tokenCache;

        public Builder setOAuth2TokenCache(OAuth2TokenCache cache){
            tokenCache = cache;
            return this;
        }

        public LoadAccountCommandContext build(){
            LoadAccountCommandContext x = autoBuild();
            x.tokenCache = this.tokenCache;
            return x;
        }

        abstract LoadAccountCommandContext autoBuild();

    }
}
