package com.microsoft.identity.common.java.opentelemetry.perf;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

public class OperationPerfTotal {
    @SerializedName("operation_name")
    private final String operationName;

    @SerializedName("num_times_executed")
    private final long numTimesExecuted;

    @SerializedName("total_elapsed_time")
    private final long totalElapsedTime;

    public OperationPerfTotal(String operationName, long numTimesExecuted, long totalElapsedTime) {
        this.operationName = operationName;
        this.numTimesExecuted = numTimesExecuted;
        this.totalElapsedTime = totalElapsedTime;
    }

    public String getOperationName() {
        return operationName;
    }

    public long getNumTimesExecuted() {
        return numTimesExecuted;
    }

    public long getTotalElapsedTime() {
        return totalElapsedTime;
    }
}
