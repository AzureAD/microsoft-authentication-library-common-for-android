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
package com.microsoft.identity.common.java.util;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * Our URIBuilder.
 * We want to make sure we never send duplicated parameters to the server.
 * This is done by
 * 1. disabling {@link org.apache.hc.core5.net.URIBuilder#addParameter(String, String)} and
 * {@link org.apache.hc.core5.net.URIBuilder#addParameters(List)}
 * 2. adding {@link CommonURIBuilder#addParametersIfAbsent}
 */
public class CommonURIBuilder extends URIBuilder {

    public CommonURIBuilder() {
        super();
    }

    public CommonURIBuilder(final String string) throws URISyntaxException {
        super(string);
    }

    public CommonURIBuilder(final URI uri) {
        super(uri);
    }

    @Override
    public CommonURIBuilder addParameters(@NonNull final List<NameValuePair> nvps) {
        throw new UnsupportedOperationException("This should never be used. Either use setParameter or addParametersIfAbsent");
    }

    @Override
    public CommonURIBuilder addParameter(@NonNull final String param, @NonNull final String value) {
        throw new UnsupportedOperationException("This should never be used. Either use setParameter or addParametersIfAbsent");
    }

    @Override
    public CommonURIBuilder setParameters(List<NameValuePair> nvps) {
        super.setParameters(nvps);
        return this;
    }

    @Override
    public CommonURIBuilder setParameters(NameValuePair... nvps) {
        super.setParameters(nvps);
        return this;
    }

    @Override
    public CommonURIBuilder setParameter(String param, String value) {
        super.setParameter(param, value);
        return this;
    }

    /**
     * Adds parameters to URI query if it does not already exist.
     * The parameter name and value are expected to be unescaped and may contain non ASCII characters.
     * <p>
     * Please note query parameters and custom query component are mutually exclusive. This method
     * will remove custom query if present.
     * </p>
     *
     * @param params list of parameters.
     * @return {@link CommonURIBuilder}
     */
    public CommonURIBuilder addParametersIfAbsent(@Nullable final Map<String, ?> params) {
        if (params == null) {
            return this;
        }

        for (final Map.Entry<String, ?> entry : params.entrySet()) {
            addParameterIfAbsent(entry.getKey(), String.valueOf(entry.getValue()));
        }

        return this;
    }

    /**
     * Adds parameters to URI query if it does not already exist.
     * The parameter name and value are expected to be unescaped and may contain non ASCII characters.
     * <p>
     * Please note query parameters and custom query component are mutually exclusive. This method
     * will remove custom query if present.
     * </p>
     *
     * @param params list of parameters.
     * @return {@link CommonURIBuilder}
     */
    public CommonURIBuilder addParametersIfAbsent(@Nullable final List<Map.Entry<String, String>> params) {
        if (params == null) {
            return this;
        }

        for (final Map.Entry<String, String> entry : params) {
            addParameterIfAbsent(entry.getKey(), entry.getValue());
        }

        return this;
    }

    /**
     * Adds parameter to URI query if it does not already exist.
     * The parameter name and value are expected to be unescaped and may contain non ASCII characters.
     * <p>
     * Please note query parameters and custom query component are mutually exclusive. This method
     * will remove custom query if present.
     * </p>
     *
     * @param param parameter name.
     * @param value parameter value.
     * @return {@link CommonURIBuilder}
     */
    public CommonURIBuilder addParameterIfAbsent(@NonNull final String param, @NonNull final String value) {
        if (containsParam(param)) {
            return this;
        }

        super.addParameter(param, value);
        return this;
    }

    private boolean containsParam(@NonNull final String param) {
        for (final NameValuePair pair : getQueryParams()) {
            if (pair.getName().equalsIgnoreCase(param)) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @return the last segment in the URI. Returns an empty string if there are no path segments.
     */
    public String getLastPathSegment() {
        final List<String> pathSegments = getPathSegments();
        if (pathSegments.isEmpty()) {
            return "";
        }

        return pathSegments.get(pathSegments.size() - 1);
    }
}
