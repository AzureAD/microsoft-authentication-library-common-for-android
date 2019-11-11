package com.microsoft.identity.common.internal.request.generated;

import com.google.auto.value.AutoValue;
import com.microsoft.identity.common.internal.dto.IAccountRecord;

@AutoValue
public abstract class RemoveAccountCommandParameters extends CommandParameters {

    public abstract String clientId();
    public abstract String redirectUri();
    public abstract IAccountRecord accountRecord();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setClientId(String value);
        public abstract Builder setRedirectUri(String value);
        public abstract RemoveAccountCommandParameters build();
    }
}
