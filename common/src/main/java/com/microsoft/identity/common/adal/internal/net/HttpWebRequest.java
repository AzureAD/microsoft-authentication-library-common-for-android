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

import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;

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
        connection.setRequestProperty("Connection", "close");


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
     * Check if the network is available. If the network is unavailable, {@link ClientException}
     * will throw with error code {@link ErrorStrings#NO_NETWORK_CONNECTION_POWER_OPTIMIZATION}
     * when connection is not available to refresh token because power optimization is enabled, or
     * throw with error code {@link ErrorStrings#DEVICE_NETWORK_NOT_AVAILABLE} otherwise.
     *
     * @param context Context
     * @throws ClientException throw network exception
     */
    public static void throwIfNetworkNotAvailable(final Context context) throws ClientException {
        final DefaultConnectionService connectionService = new DefaultConnectionService(context);
        if (!connectionService.isConnectionAvailable()) {
            if (connectionService.isNetworkDisabledFromOptimizations()) {
                final ClientException dozeModeException = new ClientException(
                        ErrorStrings.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION,
                        "Connection is not available to refresh token because power optimization is "
                                + "enabled. And the device is in doze mode or the app is standby");
                throw dozeModeException;
            } else {
                final ClientException generalNetworkException = new ClientException(
                        ErrorStrings.DEVICE_NETWORK_NOT_AVAILABLE,
                        "Connection is not available to refresh token");
                throw generalNetworkException;
            }
        }
    }

    /**
     * Convert stream into the string.
     *
     * @param inputStream {@link InputStream} to be converted to be a string.
     * @return The converted string
     * @throws IOException Thrown when failing to access inputStream stream.
     */
    private static String convertStreamToString(InputStream inputStream) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));
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
