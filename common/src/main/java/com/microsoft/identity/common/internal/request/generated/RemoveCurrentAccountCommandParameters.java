package com.microsoft.identity.common.internal.request.generated;

import com.google.auto.value.AutoValue;
import com.microsoft.identity.common.internal.dto.AccountRecord;

@AutoValue
public abstract class RemoveCurrentAccountCommandParameters extends CommandParameters implements IAccountCommandParameters {

    public abstract String clientId();
    public abstract String redirectUri();
    public abstract AccountRecord accountRecord();

    public RemoveAccountCommandParameters toRemoveAccountCommandParameters(){
        RemoveAccountCommandParameters.Builder builder = RemoveAccountCommandParameters.builder();
        return builder.setAccountRecord(this.accountRecord())
                .setClientId(this.clientId())
                .setRedirectUri(this.redirectUri())
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setClientId(String value);
        public abstract Builder setRedirectUri(String value);
        public abstract Builder setAccountRecord(AccountRecord value);
        public abstract RemoveCurrentAccountCommandParameters build();
    }
}
