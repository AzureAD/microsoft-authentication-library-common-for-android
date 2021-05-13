package com.microsoft.identity.common.java.eststelemetry;

import com.microsoft.identity.common.java.commands.ICommand;

import lombok.Builder;

@Builder
public class MockCommand implements ICommand<Boolean> {

    private String correlationId;

    @Builder.Default
    private Boolean result = true;

    @Builder.Default
    private Boolean isEligibleForEstsTelemetry = true;

    @Builder.Default
    private Boolean isEligibleForCaching = true;

    @Builder.Default
    private Boolean willReachTokenEndpoint = true;

    @Override
    public Boolean execute() throws Exception {
        return false;
    }

    @Override
    public boolean isEligibleForEstsTelemetry() {
        return result;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public boolean isEligibleForCaching() {
        return isEligibleForCaching;
    }

    @Override
    public boolean willReachTokenEndpoint() {
        return willReachTokenEndpoint;
    }
}
