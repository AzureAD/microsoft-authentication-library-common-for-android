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
package com.microsoft.identity.common.java.logging;

import com.microsoft.identity.common.java.util.StringUtil;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * This class provide way to run piece of code with a request context as resource.
 * Caller may use it in try block. Request Context is automatically unset when try
 * block finishes. To actually set the request context, caller must call initialize()
 * method after creating object of this class.
 */
public class RequestContextGuard implements AutoCloseable {

    private final IRequestContext requestContext;

    /**
     * Sets the current request context.
     * @param requestContext given request context
     */
    public RequestContextGuard(@Nullable final IRequestContext requestContext) {
        this.requestContext = requestContext;
    }

    /**
     * creates request context and adds provided correlationId to it.
     * @param correlationId correlationId
     */
    public RequestContextGuard(@Nullable final String correlationId) {
        this.requestContext = new RequestContext();
        if (!StringUtil.isNullOrEmpty(correlationId)) {
            this.requestContext.put(DiagnosticContext.CORRELATION_ID, correlationId);
        }
    }

    /**
     * Updates the Diagnostic Context with current request context.
     */
    public void initialize() {
        DiagnosticContext.INSTANCE.setRequestContext(this.requestContext);
    }

    /**
     * Clears the Diagnostic context.
     */
    @Override
    public void close()  {
        DiagnosticContext.INSTANCE.clear();
    }
}
