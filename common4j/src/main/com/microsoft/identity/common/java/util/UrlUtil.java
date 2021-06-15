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

import cz.msebera.android.httpclient.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

public class UrlUtil {

    /**
     * Append a given path string to a URL.
     *
     * @param urlToAppend URL to be appended to.
     * @param pathString  a string containing path segments to be appended to the URL.
     * @return appended URL
     */
    public static String appendPathToURL(@NonNull final URL urlToAppend,
                                         @Nullable final String pathString) throws URISyntaxException {

        if (StringUtil.isNullOrEmpty(pathString)){
            return urlToAppend.toString();
        }

        final URIBuilder pathBuilder = new URIBuilder();
        pathBuilder.setPath(pathString);

        final List<String> pathSegmentsToAppend = pathBuilder.getPathSegments();

        final URIBuilder builder = new URIBuilder(urlToAppend.toString());
        final List<String> pathSegments = builder.getPathSegments();

        final List<String> combinedPathSegments = new ArrayList<>();
        combinedPathSegments.addAll(pathSegments);

        for(final String path : pathSegmentsToAppend){
            if (!StringUtil.isNullOrEmpty(path)){
                combinedPathSegments.add(path);
            }
        }

        builder.setPathSegments(combinedPathSegments);
        return builder.build().toString();
    }
}
