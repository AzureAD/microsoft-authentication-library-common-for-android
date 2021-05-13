package com.microsoft.identity.common.java.eststelemetry;

import com.microsoft.identity.common.java.commands.ICommandResult;

import lombok.Builder;

@Builder
public class MockCommandResult<T> implements ICommandResult {

    private String correlationId;
    private ResultStatus resultStatus;

    @Builder.Default
    private T result = null;

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public ResultStatus getStatus() {
        return resultStatus;
    }

    @Override
    public Object getResult() {
        return result;
    }
}
