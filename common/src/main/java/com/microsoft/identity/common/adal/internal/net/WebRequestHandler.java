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
package com.microsoft.identity.common.adal.internal.net;

import android.os.Build;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.HeaderField;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.MediaType;

/**
 * It uses one time async task. WebRequest are wrapped here to prevent multiple
 * reuses for same tasks. Each request returns a handler for cancel action. Call
 * this from UI thread to correctly create async task and execute.
 */
public class WebRequestHandler implements IWebRequestHandler {

    /**
     * Header for accept.
     */
    public static final String HEADER_ACCEPT = HeaderField.ACCEPT;

    /**
     * Header for json type.
     */
    public static final String HEADER_ACCEPT_JSON = MediaType.APPLICATION_JSON;

    private UUID mRequestCorrelationId = null;

    private String mCurrentClientVersion = "";

    @Override
    public HttpWebResponse sendGet(URL url, Map<String, String> headers) throws IOException {
        final HttpWebRequest request = new HttpWebRequest(url, HttpWebRequest.REQUEST_METHOD_GET, updateHeaders(headers));
        return request.send();
    }

    @Override
    public HttpWebResponse sendPost(URL url, Map<String, String> headers, byte[] content,
                                    String contentType) throws IOException {
        final HttpWebRequest request = new HttpWebRequest(
                url,
                HttpWebRequest.REQUEST_METHOD_POST,
                updateHeaders(headers),
                content,
                contentType);
        return request.send();
    }

    private Map<String, String> updateHeaders(final Map<String, String> headers) {

        if (mRequestCorrelationId != null) {
            headers.put(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, mRequestCorrelationId.toString());
        }

        headers.put(AuthenticationConstants.AAD.ADAL_ID_PLATFORM, "Android");
        // TODO don't make this dependency circular
        headers.put(AuthenticationConstants.AAD.ADAL_ID_VERSION, mCurrentClientVersion);
        headers.put(AuthenticationConstants.AAD.ADAL_ID_OS_VER, "" + Build.VERSION.SDK_INT);
        headers.put(AuthenticationConstants.AAD.ADAL_ID_DM, android.os.Build.MODEL);

        return headers;
    }

    /**
     * Sets correlationId.
     *
     * @param requestCorrelationId {@link UUID}
     */
    public void setRequestCorrelationId(final UUID requestCorrelationId) {
        mRequestCorrelationId = requestCorrelationId;
    }

    @Override
    public void setClientVersion(final String clientVersion) {
        if (!StringExtensions.isNullOrBlank(clientVersion)) {
            mCurrentClientVersion = clientVersion;
        }
    }


}
