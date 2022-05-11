package com.microsoft.identity.common.java.logging;

import com.microsoft.identity.common.java.util.StringUtil;

public class RequestContextGuard implements AutoCloseable {

    private final IRequestContext requestContext;
    public RequestContextGuard(IRequestContext requestContext) {
        this.requestContext = requestContext;
    }

    public RequestContextGuard(String correlationId) {
        this.requestContext = new RequestContext();
        if (!StringUtil.isNullOrEmpty(correlationId)) {
            this.requestContext.put(DiagnosticContext.CORRELATION_ID, correlationId);
        }
    }

    public void initialize() {
        DiagnosticContext.INSTANCE.setRequestContext(this.requestContext);
    }

    @Override
    public void close()  {
        DiagnosticContext.INSTANCE.clear();
    }
}
