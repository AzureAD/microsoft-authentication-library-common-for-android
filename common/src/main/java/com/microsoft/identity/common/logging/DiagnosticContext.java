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
package com.microsoft.identity.common.logging;

import com.microsoft.identity.common.java.logging.IRequestContext;

/**
 * Class is deprecated.
 *
 * @see com.microsoft.identity.common.java.logging.DiagnosticContext
 */
// TODO @Deprecate
public class DiagnosticContext{

    /**
     * Set the request context.
     *
     * @param requestContext IRequestContext
     */
    public static void setRequestContext(final IRequestContext requestContext) {
        com.microsoft.identity.common.java.logging.DiagnosticContext.INSTANCE.setRequestContext(requestContext);
    }

    /**
     * Get the request context.
     *
     * @return IRequestContext
     */
    public static IRequestContext getRequestContext() {
        return com.microsoft.identity.common.java.logging.DiagnosticContext.INSTANCE.getRequestContext();
    }

    /**
     * Clear the local request context thread.
     */
    public static void clear() {
        com.microsoft.identity.common.java.logging.DiagnosticContext.INSTANCE.clear();
    }
}
