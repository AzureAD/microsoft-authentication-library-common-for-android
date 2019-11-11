package com.microsoft.identity.common.internal.request.generated;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.ui.browser.BrowserDescriptor;

import net.jcip.annotations.Immutable;

import java.util.List;

@Immutable
@AutoValue
@AutoValue.CopyAnnotations
public abstract class InteractiveTokenCommandContext extends CommandContext {

    //region Black Magic for keeping specific fields out of equals
    protected Context androidApplicationContext;
    protected OAuth2TokenCache tokenCache;
    protected Activity androidActivity;

    public abstract List<BrowserDescriptor> browserSafeList();

    public Context androidApplicationContext(){
        return androidApplicationContext;
    }

    public OAuth2TokenCache tokenCache() {
        return tokenCache;
    }

    public Builder toBuilder(){
        Builder builder = autoToBuilder();
        builder.androidContext = this.androidApplicationContext;
        builder.tokenCache = this.tokenCache;
        builder.androidActivity = this.androidActivity;
        return builder;
    }
    //endregion

    public static InteractiveTokenCommandContext.Builder builder() {
        return new AutoValue_InteractiveTokenCommandContext.Builder();
    }

    abstract Builder autoToBuilder();

    @AutoValue.Builder
    public abstract static class Builder {

        //region Black Magic for keeping specific fields out of equals
        private Context androidContext;
        private OAuth2TokenCache tokenCache;
        private Activity androidActivity;

        public Builder setAndroidApplicationContext(Context context){
            androidContext = context;
            return this;
        }

        public Builder setOAuth2TokenCache(OAuth2TokenCache cache){
            tokenCache = cache;
            return this;
        }

        public Builder setActivity(Activity activity){
            androidActivity = activity;
            return this;
        }

        public InteractiveTokenCommandContext build(){
            InteractiveTokenCommandContext x = autoBuild();
            x.androidApplicationContext = this.androidContext;
            x.tokenCache = this.tokenCache;
            x.androidActivity = this.androidActivity;
            return x;
        }

        abstract InteractiveTokenCommandContext autoBuild();
        //endregion

        public abstract Builder setApplicationName(String value);
        public abstract Builder setApplicationVersion(String value);
        public abstract Builder setRequiredBrokerProtocolVersion(String value);
        public abstract Builder setCorrelationId(String value);
        public abstract Builder setSdkType(SdkType value);
        public abstract Builder setSdkVersion(String value);
        public abstract Builder setBrowserSafeList(List<BrowserDescriptor> value);


    }

}
