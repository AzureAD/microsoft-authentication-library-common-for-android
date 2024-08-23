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
package com.microsoft.identity.common.java.net;

import com.microsoft.identity.common.java.util.StringUtil;

import net.jcip.annotations.Immutable;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import lombok.NonNull;

/**
 * Internal class to wrap the raw server response, headers and status code.
 */
@Immutable
public class HttpResponse {

    private final int mStatusCode;
    private final String mResponseBody;
    private final Map<String, List<String>> mResponseHeaders;
    private final Date mDate;

    /**
     * Constructor for {@link HttpResponse}.
     *
     * @param statusCode      The status code from the server response.
     * @param responseBody    Raw response body.
     * @param responseHeaders Response headers from the connection sent to the server.
     */
    public HttpResponse(final int statusCode, final String responseBody,
                        final Map<String, List<String>> responseHeaders) {
        this(new Date(), statusCode, responseBody, responseHeaders);
    }

    public HttpResponse(@NonNull final Date date,
                        final int statusCode,
                        final String responseBody,
                        final Map<String, List<String>> headerFields) {
        mDate = new Date(date.getTime());
        mStatusCode = statusCode;
        mResponseBody = responseBody;
        mResponseHeaders = headerFields;
    }

    public Date getDate() {
        return new Date(mDate.getTime());
    }

    /**
     * @return The status code.
     */
    public int getStatusCode() {
        return mStatusCode;
    }

    /**
     * @return The raw server response.
     */
    public String getBody() {
        return mResponseBody;
    }

    /**
     * @return The unmodified Map of response headers.
     * Response headers is set by {@link java.net.HttpURLConnection#getHeaderFields()} which is an unmodified Map.
     */
    public Map<String, List<String>> getHeaders() {
        return mResponseHeaders;
    }

    /**
     * Gets the value from the response header list associated to the provided key at a given index.
     * @param key   header key for look up.
     * @param index index of the element to get
     * @return      element at given index of the header list associated to the provided key.
     *              if element is not found returns null.
     */
    @Nullable
    public String getHeaderValue(@NonNull final String key, final int index){
        if (mResponseHeaders == null || index < 0 || StringUtil.isNullOrEmpty(key)){
            return null;
        }

        final List<String> list = mResponseHeaders.get(key);
        if (list == null || list.size() <= index){
            return null;
        }

        return list.get(index);
    }

    /**
     * Checks if media type in response content type is same as expected media type.
     */
    public boolean isContentTypeMediaType(@NonNull final String expectedResponseType) {
        final String responseContentType = this.getHeaderValue(HttpConstants.HeaderField.CONTENT_TYPE, 0);
        if (StringUtil.isNullOrEmpty(responseContentType)) {
            return false;
        }
        // Split the content type by semicolon to extract media type
        // e.g. JWE response content type is expected to be application/jose and there can be extra parameter e.g
        // eSTS is sending "application/jose; charset=utf-8". We are parsing the media type part and validating
        // it.
        final String mediaType = responseContentType.split(";")[0].trim();
        return expectedResponseType.equalsIgnoreCase(mediaType);
    }

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @Override
    public String toString() {
        return "HttpResponse{" +
                "mStatusCode=" + mStatusCode +
                ", mResponseBody='" + mResponseBody + '\'' +
                ", mResponseHeaders=" + mResponseHeaders +
                '}';
    }
    //CHECKSTYLE:ON
}
