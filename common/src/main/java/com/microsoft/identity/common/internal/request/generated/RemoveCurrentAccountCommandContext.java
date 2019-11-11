package com.microsoft.identity.common.internal.request.generated;

import com.google.auto.value.AutoValue;
import com.microsoft.identity.common.internal.request.SdkType;

import net.jcip.annotations.Immutable;

@Immutable
@AutoValue
@AutoValue.CopyAnnotations
public abstract class RemoveCurrentAccountCommandContext extends CommandContext {

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder setApplicationName(String value);
        public abstract Builder setApplicationVersion(String value);
        public abstract Builder setRequiredBrokerProtocolVersion(String value);
        public abstract Builder setCorrelationId(String value);
        public abstract Builder setSdkType(SdkType value);
        public abstract Builder setSdkVersion(String value);
        public abstract RemoveCurrentAccountCommandContext build();

    }
}
