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

import android.os.Build;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.platform.Device;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class DiagnosticContext {

    public static final String CORRELATION_ID = "correlation_id";
    private static final String THREAD_ID = "thread_id";

    private DiagnosticContext() {
    }

    private static final ThreadLocal<IRequestContext> REQUEST_CONTEXT_THREAD_LOCAL =
            new ThreadLocal<IRequestContext>() {
                @Override // This is the default value for the RequestContext if it's unset
                protected RequestContext initialValue() {
                    final RequestContext defaultRequestContext = new RequestContext();
                    defaultRequestContext.put(CORRELATION_ID, "UNSET");
                    return defaultRequestContext;
                }
            };

    /**
     * Set the request context.
     *
     * @param requestContext IRequestContext
     */
    public static void setRequestContext(final IRequestContext requestContext) {
        if (null == requestContext) {
            clear();
            return;
        }

        requestContext.put(THREAD_ID, String.valueOf(Thread.currentThread().getId()));
        REQUEST_CONTEXT_THREAD_LOCAL.set(requestContext);
    }

    /**
     * Get the request context.
     *
     * @return IRequestContext
     */
    public static IRequestContext getRequestContext() {
        if (!hasThreadId()) {
            setThreadId();
        }

        return REQUEST_CONTEXT_THREAD_LOCAL.get();
    }

    private static void setThreadId() {
        REQUEST_CONTEXT_THREAD_LOCAL.get().put(
                THREAD_ID,
                String.valueOf(Thread.currentThread().getId())
        );
    }

    private static boolean hasThreadId() {
        return REQUEST_CONTEXT_THREAD_LOCAL.get().containsKey(THREAD_ID);
    }

    /**
     * Clear rhe local request context thread.
     */
    public static void clear() {
        REQUEST_CONTEXT_THREAD_LOCAL.remove();
    }

    /**
     * Function previously resided in Device.java, contains Constants
     * Attaching the constants to Diagnostic Context, for the purpose of having them in one place.
     */
    @SuppressWarnings("deprecation")
    public static Map<String, String> getPlatformIdParameters() {
        final Map<String, String> platformParameters = new HashMap<>();


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //CPU_ABI has been deprecated
            platformParameters.put(AuthenticationConstants.PlatformIdParameters.CPU_PLATFORM, Build.CPU_ABI);
        } else {
            final String[] supportedABIs = Build.SUPPORTED_ABIS;

            if (supportedABIs != null && supportedABIs.length > 0) {
                platformParameters.put(AuthenticationConstants.PlatformIdParameters.CPU_PLATFORM, supportedABIs[0]);
            }
        }

        platformParameters.put(AuthenticationConstants.PlatformIdParameters.OS, String.valueOf(Build.VERSION.SDK_INT));
        platformParameters.put(AuthenticationConstants.PlatformIdParameters.DEVICE_MODEL, Build.MODEL);

        return Collections.unmodifiableMap(platformParameters);
    }
}
