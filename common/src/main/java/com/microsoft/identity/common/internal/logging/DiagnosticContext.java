package com.microsoft.identity.common.internal.logging;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;

public class DiagnosticContext {

    private static final ThreadLocal<IRequestContext> sREQUEST_CONTEXT_THREAD_LOCAL =
            new ThreadLocal<IRequestContext>() {
                @Override // This is the default value for the RequestContext if it's unset
                protected RequestContext initialValue() {
                    final RequestContext defaultRequestContext = new RequestContext();
                    defaultRequestContext.put(AuthenticationConstants.AAD.CORRELATION_ID, "UNSET");
                    return defaultRequestContext;
                }
            };

    public static void setRequestContext(final IRequestContext requestContext) {
        if (null == requestContext) {
            clear();
            return;
        }

        sREQUEST_CONTEXT_THREAD_LOCAL.set(requestContext);
    }

    public static IRequestContext getRequestContext() {
        return sREQUEST_CONTEXT_THREAD_LOCAL.get();
    }

    public static void clear() {
        sREQUEST_CONTEXT_THREAD_LOCAL.remove();
    }
}
