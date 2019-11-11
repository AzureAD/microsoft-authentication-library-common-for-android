package com.microsoft.identity.common.internal.request.generated;

import com.google.auto.value.AutoValue;
import com.microsoft.identity.common.internal.request.SdkType;

import net.jcip.annotations.Immutable;

@Immutable
@AutoValue
@AutoValue.CopyAnnotations
public abstract class GetDeviceModeCommandContext extends CommandContext {

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract GetDeviceModeCommandContext.Builder setApplicationName(String value);
        public abstract GetDeviceModeCommandContext.Builder setApplicationVersion(String value);
        public abstract GetDeviceModeCommandContext.Builder setRequiredBrokerProtocolVersion(String value);
        public abstract GetDeviceModeCommandContext.Builder setCorrelationId(String value);
        public abstract GetDeviceModeCommandContext.Builder setSdkType(SdkType value);
        public abstract GetDeviceModeCommandContext.Builder setSdkVersion(String value);
        public abstract GetDeviceModeCommandContext build();

    }
}