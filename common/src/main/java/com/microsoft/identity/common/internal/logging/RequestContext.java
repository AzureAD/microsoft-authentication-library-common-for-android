package com.microsoft.identity.common.internal.logging;

// TODO I'm not wedded to this name, but the concept may work for tracking correlationIds
public class RequestContext {

    private String mCorrelationId;

    public void setCorrelationId(final String correlationId) {
        mCorrelationId = correlationId;
    }

    public String getCorrelationId() {
        return mCorrelationId;
    }

}
