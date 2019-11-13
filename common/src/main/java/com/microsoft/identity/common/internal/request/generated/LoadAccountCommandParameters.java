package com.microsoft.identity.common.internal.request.generated;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class LoadAccountCommandParameters extends CommandParameters implements IAccountCommandParameters {

    public abstract String clientId();
    public abstract String redirectUri();

    public static Builder builder() {
        return new AutoValue_LoadAccountCommandParameters.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setClientId(String value);
        public abstract Builder setRedirectUri(String value);
        public abstract LoadAccountCommandParameters build();
    }
}
