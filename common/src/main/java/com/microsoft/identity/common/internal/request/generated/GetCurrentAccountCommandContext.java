package com.microsoft.identity.common.internal.request.generated;

import com.google.auto.value.AutoValue;
import com.microsoft.identity.common.internal.request.SdkType;

import net.jcip.annotations.Immutable;

@Immutable
@AutoValue
@AutoValue.CopyAnnotations
public abstract class GetCurrentAccountCommandContext extends CommandContext {

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract GetCurrentAccountCommandContext.Builder setApplicationName(String value);
        public abstract GetCurrentAccountCommandContext.Builder setApplicationVersion(String value);
        public abstract GetCurrentAccountCommandContext.Builder setRequiredBrokerProtocolVersion(String value);
        public abstract GetCurrentAccountCommandContext.Builder setCorrelationId(String value);
        public abstract GetCurrentAccountCommandContext.Builder setSdkType(SdkType value);
        public abstract GetCurrentAccountCommandContext.Builder setSdkVersion(String value);
        public abstract GetCurrentAccountCommandContext build();

    }
}
