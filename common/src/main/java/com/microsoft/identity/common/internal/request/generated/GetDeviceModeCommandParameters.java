package com.microsoft.identity.common.internal.request.generated;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class GetDeviceModeCommandParameters extends CommandParameters {

    abstract String clientId();

    abstract String redirectUri();

    public static GetDeviceModeCommandParameters.Builder builder() {
        return new AutoValue_GetDeviceModeCommandParameters.Builder();
    }

    public abstract GetDeviceModeCommandParameters.Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setClientId(String value);

        public abstract Builder setRedirectUri(String value);

        public abstract GetDeviceModeCommandParameters build();
    }

}

