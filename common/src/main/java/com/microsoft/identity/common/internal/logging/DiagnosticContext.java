package com.microsoft.identity.common.internal.logging;

// TODO Not crazy about this name either
public class DiagnosticContext {

    private static final ThreadLocal<RequestContext> sREQUEST_CONTEXT_THREAD_LOCAL =
            new ThreadLocal<RequestContext>() {
                @Override // This is the default value for the RequestContext if it's unset
                protected RequestContext initialValue() {
                    final RequestContext defaultRequestContext = new RequestContext();
                    defaultRequestContext.setCorrelationId("UNSET");
                    return defaultRequestContext;
                }
            };

    public static void setRequestContext(final RequestContext requestContext) {
        sREQUEST_CONTEXT_THREAD_LOCAL.set(requestContext);
    }

    public static RequestContext getRequestContext() {
        return sREQUEST_CONTEXT_THREAD_LOCAL.get();
    }

    public static void clear() {
        sREQUEST_CONTEXT_THREAD_LOCAL.remove();
    }
}
