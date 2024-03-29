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

import android.content.Context;
import android.os.Debug;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Webrequest are called in background thread from API level. HttpWebRequest
 * does not create another thread.
 */
public class HttpWebRequest {
    static final String REQUEST_METHOD_POST = "POST";
    static final String REQUEST_METHOD_GET = "GET";

    private static final int DEBUG_SIMULATE_DELAY = 0;
    private static final int CONNECT_TIME_OUT = AuthenticationSettings.INSTANCE.getConnectTimeOut();
    private static final int READ_TIME_OUT = AuthenticationSettings.INSTANCE.getReadTimeOut();
    private final String mRequestMethod;
    private final URL mUrl;
    private final byte[] mRequestContent;
    private final String mRequestContentType;
    private final Map<String, String> mRequestHeaders;

    /**
     * Constructor of HttpWebRequest.
     *
     * @param requestURL    URL
     * @param requestMethod String
     * @param headers       Map<String, String>
     */
    public HttpWebRequest(URL requestURL, String requestMethod, Map<String, String> headers) {
        this(requestURL, requestMethod, headers, null, null);
    }

    /**
     * Constructor of HttpWebRequest.
     *
     * @param requestURL         URL
     * @param requestMethod      String
     * @param headers            Map<String, String>
     * @param requestContent     byte[]
     * @param requestContentType String
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public HttpWebRequest(
            URL requestURL,
            String requestMethod,
            Map<String, String> headers,
            byte[] requestContent,
            String requestContentType) {
        mUrl = requestURL;
        mRequestMethod = requestMethod;
        mRequestHeaders = new HashMap<>();
        if (mUrl != null) {
            mRequestHeaders.put("Host", mUrl.getAuthority());
        }
        mRequestHeaders.putAll(headers);
        mRequestContent = requestContent;
        mRequestContentType = requestContentType;
    }

    /**
     * setupConnection before sending the request.
     */
    private HttpURLConnection setupConnection() throws IOException {
        if (mUrl == null) {
            throw new IllegalArgumentException("requestURL");
        }
        if (!mUrl.getProtocol().equalsIgnoreCase("http")
                && !mUrl.getProtocol().equalsIgnoreCase("https")) {
            throw new IllegalArgumentException("requestURL");
        }
        HttpURLConnection.setFollowRedirects(true);
        final HttpURLConnection connection = HttpUrlConnectionFactory.createHttpUrlConnection(mUrl);
        connection.setConnectTimeout(CONNECT_TIME_OUT);

        // Apply the request headers
        final Set<Map.Entry<String, String>> headerEntries = mRequestHeaders.entrySet();
        for (final Map.Entry<String, String> entry : headerEntries) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        connection.setReadTimeout(READ_TIME_OUT);
        connection.setInstanceFollowRedirects(true);
        connection.setUseCaches(false);
        connection.setRequestMethod(mRequestMethod);
        connection.setDoInput(true); // it will at least read status
        // code. Default is true.
        setRequestBody(connection, mRequestContent, mRequestContentType);

        return connection;
    }

    /**
     * Send the request.
     *
     * @return HttpWebResponse
     * @throws IOException throws if the input stream is null.
     */
    public HttpWebResponse send() throws IOException {
        final HttpURLConnection connection = setupConnection();
        final HttpWebResponse response;
        InputStream responseStream = null;
        try {
            try {
                responseStream = connection.getInputStream();
            } catch (IOException ex) {
                // If it does not get the error stream, it will return
                // exception in the httpresponse
                responseStream = connection.getErrorStream();
                if (responseStream == null) {
                    throw ex;
                }
            }
            // GET request should read status after getInputStream to make
            // this work for different SDKs
            final int statusCode = connection.getResponseCode();
            final String responseBody = convertStreamToString(responseStream);

            // It will only run in debugger and set from outside for testing
            if (Debug.isDebuggerConnected() && DEBUG_SIMULATE_DELAY > 0) {
                // sleep background thread in debugging mode
                try {
                    Thread.sleep(DEBUG_SIMULATE_DELAY);
                } catch (InterruptedException e) {
                    // Do nothing.
                }
            }

            response = new HttpWebResponse(statusCode, responseBody, connection.getHeaderFields());
        } finally {
            safeCloseStream(responseStream);
            // We are not disconnecting from network to allow connection to be returned into the
            // connection pool. If we call disconnect due to buggy implementation we are not reusing
            // connections.
            //if (connection != null) {
            //	connection.disconnect();
            //}
        }

        return response;
    }

    /**
     * Convert stream into the string.
     *
     * @param inputStream {@link InputStream} to be converted to be a string.
     * @return The converted string
     * @throws IOException Thrown when failing to access inputStream stream.
     */
    private static String convertStreamToString(final InputStream inputStream) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream, AuthenticationConstants.CHARSET_UTF8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }

            return sb.toString();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private static void setRequestBody(HttpURLConnection connection, byte[] contentRequest, String requestContentType) throws IOException {
        if (null != contentRequest) {
            connection.setDoOutput(true);

            if (null != requestContentType && !requestContentType.isEmpty()) {
                connection.setRequestProperty("Content-Type", requestContentType);
            }

            connection.setRequestProperty("Content-Length",
                    Integer.toString(contentRequest.length));
            connection.setFixedLengthStreamingMode(contentRequest.length);

            OutputStream out = null;
            try {
                out = connection.getOutputStream();
                out.write(contentRequest);
            } finally {
                safeCloseStream(out);
            }
        }
    }

    /**
     * Close the stream safely.
     *
     * @param stream stream to be closed
     */
    private static void safeCloseStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                // swallow error in this case
            }
        }
    }
}
