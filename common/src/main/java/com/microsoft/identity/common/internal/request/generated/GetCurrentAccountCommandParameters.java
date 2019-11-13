package com.microsoft.identity.common.internal.request.generated;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class GetCurrentAccountCommandParameters extends CommandParameters implements IAccountCommandParameters {

    public abstract String clientId();
    public abstract String redirectUri();

    public static Builder builder() {
        return new AutoValue_GetCurrentAccountCommandParameters.Builder();
    }

    public LoadAccountCommandParameters toLoadAccountAccountCommandParameters(){
        LoadAccountCommandParameters.Builder builder = LoadAccountCommandParameters.builder();
        builder.setClientId(this.clientId());
        builder.setRedirectUri(this.redirectUri());
        return builder.build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setClientId(String value);
        public abstract Builder setRedirectUri(String value);
        public abstract GetCurrentAccountCommandParameters build();
    }
}
