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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.logging.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Class is deprecated.
 *
 * @see com.microsoft.identity.common.java.logging.DiagnosticContext
 */
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
@Deprecated
public final class DiagnosticContext extends com.microsoft.identity.common.logging.DiagnosticContext {

    private static final String TAG = DiagnosticContext.class.getSimpleName();

    private static boolean sLogDeprecationWarning = true;

    public static void setRequestContext(final IRequestContext requestContext) {
        com.microsoft.identity.common.logging.DiagnosticContext.setRequestContext(requestContext);
        logDeprecationWarning();
    }

    private static void logDeprecationWarning() {
        final String methodTag = TAG + ":logDeprecationWarning";
        if (sLogDeprecationWarning) {
            sLogDeprecationWarning = false;
            Logger.warn(methodTag, "This class is deprecated. "
                    + "Migrate usage to: com.microsoft.identity.common.logging.DiagnosticContext");
        }
    }

    public static IRequestContext getRequestContext() {
        logDeprecationWarning();

        // To maintain true backcompat, we'll new up an instance of the old interface which
        // will delegate to the object returned by the super class.
        final com.microsoft.identity.common.java.logging.IRequestContext origRc =
                com.microsoft.identity.common.java.logging.DiagnosticContext.INSTANCE.getRequestContext();
        return new IRequestContext() {
            @Override
            public String toJsonString() {
                return origRc.toJsonString();
            }

            @Override
            public int size() {
                return origRc.size();
            }

            @Override
            public boolean isEmpty() {
                return origRc.isEmpty();
            }

            @Override
            public boolean containsKey(@Nullable final Object key) {
                return origRc.containsKey(key);
            }

            @Override
            public boolean containsValue(@Nullable final Object value) {
                return origRc.containsValue(value);
            }

            @Nullable
            @Override
            public String get(@Nullable final Object key) {
                return origRc.get(key);
            }

            @Nullable
            @Override
            public String put(final String key, final String value) {
                return origRc.put(key, value);
            }

            @Nullable
            @Override
            public String remove(@Nullable final Object key) {
                return origRc.remove(key);
            }

            @Override
            public void putAll(@NonNull final Map<? extends String, ? extends String> m) {
                origRc.putAll(m);
            }

            @Override
            public void clear() {
                origRc.clear();
            }

            @NonNull
            @Override
            public Set<String> keySet() {
                return origRc.keySet();
            }

            @NonNull
            @Override
            public Collection<String> values() {
                return origRc.values();
            }

            @NonNull
            @Override
            public Set<Entry<String, String>> entrySet() {
                return origRc.entrySet();
            }
        };
    }

    public static void clear() {
        logDeprecationWarning();
        com.microsoft.identity.common.logging.DiagnosticContext.clear();
    }
}
