package com.microsoft.identity.common.internal.eststelemetry;

public interface IRequestTelemetry {
    String getHeaderStringForFields();

    String getCompleteHeaderString();

    RequestTelemetry derive(RequestTelemetry requestTelemetry);
}
