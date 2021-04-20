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
package com.microsoft.identity.common.internal.net;

import com.microsoft.identity.common.java.internal.net.HttpResponse;
import com.microsoft.identity.common.java.internal.net.RetryPolicy;

/**
 * A client object for handling HTTP requests and responses.  This class accepts a RetryPolicy that
 * is applied to the responses.  By default, the policy is a null policy not retrying anything.  The
 * default settings for read timeout and connection timeout are 30s, and the default setting for the
 * size of the buffer for reading objects from the http stream is 1024 bytes.
 *
 * There are two ways to supply timeout values to this class, one method takes suppliers, the other
 * one integers.  If you use both of these, the suppliers method will take precendence over the method
 * using integers.
 */
public class UrlConnectionHttpClient extends com.microsoft.identity.common.java.internal.net.UrlConnectionHttpClient {

    // Added for backcompat. Eventually, this constructor and this whole class will not be needed.
    private UrlConnectionHttpClient(RetryPolicy<HttpResponse> retryPolicy, int connectTimeoutMs, int readTimeoutMs, com.microsoft.identity.common.java.internal.net.UrlConnectionHttpClient.Supplier<Integer> connectTimeoutMsSupplier, com.microsoft.identity.common.java.internal.net.UrlConnectionHttpClient.Supplier<Integer> readTimeoutMsSupplier, int streamBufferSize) {
        super(retryPolicy, connectTimeoutMs, readTimeoutMs, connectTimeoutMsSupplier, readTimeoutMsSupplier, streamBufferSize);
    }

    /**
     * Obtain a static default instance of the HTTP Client class.
     * @return a default-configured HttpClient.
     */
    public static UrlConnectionHttpClient getDefaultInstance() {
        return (UrlConnectionHttpClient) com.microsoft.identity.common.java.internal.net.UrlConnectionHttpClient.getDefaultInstance();
    }
}
