package com.microsoft.identity.common.internal.request.generated;

import android.content.Context;

import com.google.auto.value.AutoValue;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.request.SdkType;

import net.jcip.annotations.Immutable;

@Immutable
@AutoValue
@AutoValue.CopyAnnotations
public abstract class GetDeviceModeCommandContext extends CommandContext {

    //region Black Magic for keeping specific fields out of equals
    protected Context androidApplicationContext;

    public Context androidApplicationContext() {
        return androidApplicationContext;
    }


    public GetDeviceModeCommandContext.Builder toBuilder() {
        GetDeviceModeCommandContext.Builder builder = autoToBuilder();
        builder.androidContext = this.androidApplicationContext;
        return builder;
    }
    //endregion

    public static GetDeviceModeCommandContext.Builder builder() {
        return new AutoValue_GetDeviceModeCommandContext.Builder();
    }

    abstract GetDeviceModeCommandContext.Builder autoToBuilder();

    @AutoValue.Builder
    public abstract static class Builder {

        //region Black Magic for keeping specific fields out of equals
        private Context androidContext;

        public GetDeviceModeCommandContext.Builder setAndroidApplicationContext(Context context) {
            androidContext = context;
            return this;
        }

        public GetDeviceModeCommandContext build() {
            GetDeviceModeCommandContext x = autoBuild();
            x.androidApplicationContext = this.androidContext;
            return x;
        }

        abstract GetDeviceModeCommandContext autoBuild();
        //endregion

        public abstract Builder setApplicationName(String value);
        public abstract Builder setApplicationVersion(String value);
        public abstract Builder setRequiredBrokerProtocolVersion(String value);
        public abstract Builder setCorrelationId(String value);
        public abstract Builder setSdkType(SdkType value);
        public abstract Builder setSdkVersion(String value);

    }
}