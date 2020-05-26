package com.microsoft.identity.common.internal.net;

import com.microsoft.identity.common.internal.authscheme.IPoPAuthenticationSchemeParams;

import java.io.IOException;
import java.util.concurrent.Callable;

class NoRetryPolicy implements RetryPolicy<HttpResponse> {
    @Override
    public HttpResponse attempt(Callable<HttpResponse> response) throws IOException {
        try {
            return response.call();
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new RuntimeException(e);
        }
    }
}
