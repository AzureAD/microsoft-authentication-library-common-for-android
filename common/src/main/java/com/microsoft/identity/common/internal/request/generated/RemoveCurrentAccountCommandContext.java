package com.microsoft.identity.common.internal.request.generated;

import android.content.Context;

import com.google.auto.value.AutoValue;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.ui.browser.BrowserDescriptor;

import net.jcip.annotations.Immutable;

import java.util.List;

@Immutable
@AutoValue
@AutoValue.CopyAnnotations
public abstract class RemoveCurrentAccountCommandContext extends CommandContext {

    public abstract Boolean isSharedDevice();

    public abstract List<BrowserDescriptor> browserSafeList();

    //region Black Magic for keeping specific fields out of equals
    protected Context androidApplicationContext;
    protected OAuth2TokenCache tokenCache;

    public Context androidApplicationContext() {
        return androidApplicationContext;
    }

    public RemoveCurrentAccountCommandContext.Builder toBuilder() {
        RemoveCurrentAccountCommandContext.Builder builder = autoToBuilder();
        builder.androidContext = this.androidApplicationContext;
        builder.tokenCache = this.tokenCache;
        return builder;
    }
    //endregion

    public static RemoveCurrentAccountCommandContext.Builder builder() {
        return new AutoValue_RemoveCurrentAccountCommandContext.Builder();
    }

    abstract RemoveCurrentAccountCommandContext.Builder autoToBuilder();

    public RemoveAccountCommandContext toRemoveAccountCommandContext() {
        RemoveAccountCommandContext.Builder builder = RemoveAccountCommandContext.builder();

        return builder.setAndroidApplicationContext((this.androidApplicationContext))
                .setApplicationName(this.applicationName())
                .setApplicationVersion(this.applicationVersion())
                .setOAuth2TokenCache(this.tokenCache)
                .setCorrelationId(this.correlationId())
                .setSdkType(this.sdkType())
                .setSdkVersion((this.sdkVersion()))
                .setRequiredBrokerProtocolVersion(this.requiredBrokerProtocolVersion())
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        //region Black Magic for keeping specific fields out of equals
        private Context androidContext;
        private OAuth2TokenCache tokenCache;

        public RemoveCurrentAccountCommandContext.Builder setAndroidApplicationContext(Context context) {
            androidContext = context;
            return this;
        }

        public RemoveCurrentAccountCommandContext.Builder setTokenCache(OAuth2TokenCache cache) {
            tokenCache = cache;
            return this;
        }

        public RemoveCurrentAccountCommandContext build() {
            RemoveCurrentAccountCommandContext x = autoBuild();
            x.androidApplicationContext = this.androidContext;
            x.tokenCache = this.tokenCache;
            return x;
        }

        abstract RemoveCurrentAccountCommandContext autoBuild();
        //endregion


        public abstract Builder setApplicationName(String value);

        public abstract Builder setApplicationVersion(String value);

        public abstract Builder setRequiredBrokerProtocolVersion(String value);

        public abstract Builder setCorrelationId(String value);

        public abstract Builder setSdkType(SdkType value);

        public abstract Builder setSdkVersion(String value);

        public abstract Builder setBrowserSafeList(List<BrowserDescriptor> value);

        public abstract Builder setIsSharedDevice(Boolean value);

    }
}
