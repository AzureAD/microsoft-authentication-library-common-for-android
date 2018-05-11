// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.logging;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;

public class DiagnosticContext {

    private static final String THREAD_ID = "thread_id";

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

        requestContext.put(THREAD_ID, String.valueOf(Thread.currentThread().getId()));
        sREQUEST_CONTEXT_THREAD_LOCAL.set(requestContext);
    }

    public static IRequestContext getRequestContext() {
        if (!hasThreadId()) {
            setThreadId();
        }

        return sREQUEST_CONTEXT_THREAD_LOCAL.get();
    }

    private static void setThreadId() {
        sREQUEST_CONTEXT_THREAD_LOCAL.get().put(
                THREAD_ID,
                String.valueOf(Thread.currentThread().getId())
        );
    }

    private static boolean hasThreadId() {
        return sREQUEST_CONTEXT_THREAD_LOCAL.get().containsKey(THREAD_ID);
    }

    public static void clear() {
        sREQUEST_CONTEXT_THREAD_LOCAL.remove();
    }
}
