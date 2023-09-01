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

import com.microsoft.identity.common.java.exception.ClientException;
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

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

public class UrlUtil {
    private static final String TAG = UrlUtil.class.getSimpleName();

    public static URL appendPathToURL(@NonNull final URL urlToAppend,
                                      @Nullable final String pathString) // TODO remove post test slice
            throws URISyntaxException, MalformedURLException {
        return appendPathToURL(urlToAppend, pathString, null);
    }

    /**
     * Append a given path string to a URL.
     *
     * @param urlToAppend URL to be appended to.
     * @param pathString  a string containing path segments to be appended to the URL.
     * @return appended URL
     */
    public static URL appendPathToURL(@NonNull final URL urlToAppend,
                                      @Nullable final String pathString,
                                      @Nullable final String queryParam) // TODO remove post test slice
            throws URISyntaxException, MalformedURLException {

        if (StringUtil.isNullOrEmpty(pathString)) {
            return urlToAppend;
        }

        final CommonURIBuilder pathBuilder = new CommonURIBuilder();
        pathBuilder.setPath(pathString);

        final List<String> pathSegmentsToAppend = pathBuilder.getPathSegments();

        final CommonURIBuilder builder = new CommonURIBuilder(urlToAppend.toString());
        final List<String> pathSegments = builder.getPathSegments();

        final List<String> combinedPathSegments = new ArrayList<>();

        // TODO check this with MSAL team
        // Add all non-empty path segments from the base URL
        for (final String path : pathSegments) {
            if (!StringUtil.isNullOrEmpty(path)) {
                combinedPathSegments.add(path);
            }
        }

        // Add all non-empty path segments from the path to append
        for (final String path : pathSegmentsToAppend) {
            if (!StringUtil.isNullOrEmpty(path)) {
                combinedPathSegments.add(path);
            }
        }

        builder.setPathSegments(combinedPathSegments);
        if (queryParam != null && !queryParam.isEmpty()) {
            builder.setQuery(queryParam);
        }
        return builder.build().toURL();
    }

    /**
     * Get URL parameters from a given {@link URI} object.
     *
     * @param urlString a uri string.
     * @return a map of url parameters.
     */
    @NonNull
    public static Map<String, String> getParameters(@Nullable final String urlString)
            throws ClientException {
        if (StringUtil.isNullOrEmpty(urlString)){
            Logger.warn(TAG, "url string is null.");
            return Collections.emptyMap();
        }

        try {
            return getParameters(new URI(urlString));
        } catch (final URISyntaxException e){
            throw new ClientException(ClientException.MALFORMED_URL,
                    "Cannot extract parameter from a malformed URL string.", e);
        }
    }
    /**
     * Get URL parameters from a given {@link URI} object.
     *
     * @param uri String
     * @return a map of url parameters.
     */
    @NonNull
    public static Map<String, String> getParameters(@Nullable final URI uri){
        final String methodName = ":getUrlParameters";

        if (uri == null){
            Logger.warn(TAG, "uri is null.");
            return Collections.emptyMap();
        }

        if (uri.isOpaque()){
            // Opaque URI *might* have query params, but Java's URI would just treat the whole URL as
            // [scheme:]scheme-specific-part[#fragment]
            //
            // Since we want to try extracting query params from it (and we don't care about other parts of the URI),
            // we're going to prepend a scheme so that Java's URI recognizes this as a hierarchical one.
            // [scheme:][//authority][path][?query][#fragment]
            //
            // See: https://docs.oracle.com/javase/8/docs/api/java/net/URI.html
            try {
                return getParameters(new URI("scheme://" + uri.toString()));
            } catch (final URISyntaxException e) {
                Logger.warn(TAG, "Cannot convert opaque URI.");
                return Collections.emptyMap();
            }
        }

        final String fragment = uri.getFragment();
        if (!StringUtil.isNullOrEmpty(fragment) &&
                !urlFormDecode(fragment).isEmpty()) {
            Logger.warn(TAG, "Received url contains unexpected fragment parameters.");
            Logger.warnPII(TAG, "Unexpected fragment: " + uri.getFragment());
        }

        if (StringUtil.isNullOrEmpty(uri.getQuery())) {
            Logger.info(
                    TAG + methodName,
                    "URL does not contain query parameter"
            );
            return Collections.emptyMap();
        }

        return urlFormDecode(uri.getRawQuery());
    }

    /**
     * Decode url string into a key value pairs with default query delimiter.
     *
     * @param urlParameter URL query parameter
     * @return key value pairs
     */
    @NonNull
    public static Map<String, String> urlFormDecode(@NonNull final String urlParameter) {
        return urlFormDecodeData(urlParameter, "&");
    }

    /**
     * Decode url string into a key value pairs with given query delimiter given
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

    /**
     * This creates a url from a String, rewriting any malformedUrlExceptions to runtime.
     * @param urlString the string to convert.
     * @return the corresponding {@link URL}.
     */
    public static URL makeUrlSilent(String urlString) {
        try {
            return new URL(urlString);
        } catch (final MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String removeTrailingSlash(@NonNull final String urlString) {
        return urlString.replaceFirst("/*$", "");
    }
}
