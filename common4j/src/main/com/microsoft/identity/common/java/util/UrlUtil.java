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

import com.microsoft.identity.common.java.logging.Logger;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import cz.msebera.android.httpclient.client.utils.URIBuilder;
import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

public class UrlUtil {
    private static final String TAG = UrlUtil.class.getSimpleName();

    /**
     * Append a given path string to a URL.
     *
     * @param urlToAppend URL to be appended to.
     * @param pathString  a string containing path segments to be appended to the URL.
     * @return appended URL
     */
    public static URL appendPathToURL(@NonNull final URL urlToAppend,
                                      @Nullable final String pathString)
            throws URISyntaxException, MalformedURLException {

        if (StringUtil.isNullOrEmpty(pathString)) {
            return urlToAppend;
        }

        final URIBuilder pathBuilder = new URIBuilder();
        pathBuilder.setPath(pathString);

        final List<String> pathSegmentsToAppend = pathBuilder.getPathSegments();

        final URIBuilder builder = new URIBuilder(urlToAppend.toString());
        final List<String> pathSegments = builder.getPathSegments();

        final List<String> combinedPathSegments = new ArrayList<>(pathSegments);

        for (final String path : pathSegmentsToAppend) {
            if (!StringUtil.isNullOrEmpty(path)) {
                combinedPathSegments.add(path);
            }
        }

        builder.setPathSegments(combinedPathSegments);
        return builder.build().toURL();
    }


    /**
     * create url from given endpoint. return null if format is not right.
     *
     * @param endpoint url as a string
     * @return URL object for this string
     */
    public static URL getUrl(@NonNull final String endpoint) {
        final String methodName = ":getUrl";
        try {
            return new URL(endpoint);
        } catch (final MalformedURLException e) {
            Logger.errorPII(TAG + methodName, "URL is malformed", e);
            return null;
        }
    }

    /**
     * Get URL parameters from final url.
     *
     * @param finalUrl String
     * @return a map of url parameters.
     */
    @NonNull
    public static Map<String, String> getUrlParameters(@Nullable final String finalUrl) {
        if (StringUtil.isNullOrEmpty(finalUrl)){
            return Collections.emptyMap();
        }

        final String methodName = ":getUrlParameters";
        try {
            final URI response = new URIBuilder(finalUrl).build();
            final String fragment = response.getFragment();

            if (!StringUtil.isNullOrEmpty(fragment) &&
                    !urlFormDecode(fragment).isEmpty()) {
                Logger.warn(TAG, "Received url contains unexpected fragment parameters.");
                Logger.warnPII(TAG, "Unexpected fragment: " + response.getFragment());
            }

            if (StringUtil.isNullOrEmpty(response.getQuery())){
                Logger.info(
                        TAG + methodName,
                        "URL does not contain query parameter"
                );
                return Collections.emptyMap();
            }

            return urlFormDecode(response.getQuery());
        } catch (final URISyntaxException e) {
            Logger.warn(TAG + methodName, "Failed to extract URL parameters");
            Logger.errorPII(
                    TAG + methodName,
                    "Url is invalid",
                    e
            );
            return Collections.emptyMap();
        }
    }

    /**
     * decode url string into a key value pairs with default query delimiter.
     *
     * @param urlParameter URL query parameter
     * @return key value pairs
     */
    @NonNull
    public static Map<String, String> urlFormDecode(@NonNull final String urlParameter) {
        return urlFormDecodeData(urlParameter, "&");
    }

    /**
     * decode url string into a key value pairs with given query delimiter given
     * string as a=1&b=2 will return key value of [[a,1],[b,2]].
     *
     * @param urlParameter URL parameter to be decoded
     * @param delimiter    query delimiter
     * @return Map key value pairs
     */
    @NonNull
    static Map<String, String> urlFormDecodeData(@NonNull final String urlParameter,
                                                 @NonNull final String delimiter) {
        final String methodName = ":urlFormDecodeData";
        final Map<String, String> result = new HashMap<>();

        if (!StringUtil.isNullOrEmpty(urlParameter)) {
            StringTokenizer parameterTokenizer = new StringTokenizer(urlParameter, delimiter);

            while (parameterTokenizer.hasMoreTokens()) {
                String pair = parameterTokenizer.nextToken();
                String[] elements = pair.split("=", 2);
                String value = null;
                String key = null;

                if (elements.length == 2) {
                    try {
                        key = StringUtil.urlFormDecode(elements[0].trim());
                        value = StringUtil.urlFormDecode(elements[1].trim());
                    } catch (UnsupportedEncodingException e) {
                        Logger.errorPII(
                                TAG + methodName,
                                "Encoding format is not supported",
                                e
                        );
                        continue;
                    }
                } else if (elements.length == 1) {
                    try {
                        key = StringUtil.urlFormDecode(elements[0].trim());
                        value = "";
                    } catch (UnsupportedEncodingException e) {
                        Logger.errorPII(
                                TAG + methodName,
                                "Encoding format is not supported",
                                e
                        );
                        continue;
                    }
                }

                if (!StringUtil.isNullOrEmpty(key)) {
                    result.put(key, value);
                }
            }
        }

        return result;
    }


}
