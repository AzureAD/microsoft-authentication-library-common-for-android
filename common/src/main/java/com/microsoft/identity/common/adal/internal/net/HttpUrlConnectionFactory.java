package com.microsoft.identity.common.adal.internal.net;

import android.support.annotation.VisibleForTesting;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Internal class for creating {@link HttpURLConnection}.
 * For testability, test case could set mocked {@link HttpURLConnection}
 * to inject dependency.
 */
public final class HttpUrlConnectionFactory {

    private static HttpURLConnection sMockedConnection = null;

    private static URL sMockedConnectionOpenUrl = null;

    /**
     * Private constructor to prevent the class from being initiated.
     */
    private HttpUrlConnectionFactory() { }

    /**
     * Set the mocked {@link HttpURLConnection}.
     * @param mockedHttpUrlConnection The mocked {@link HttpURLConnection} to set.
     */
    public static void setMockedHttpUrlConnection(final HttpURLConnection mockedHttpUrlConnection) {
        sMockedConnection = mockedHttpUrlConnection;
        if (mockedHttpUrlConnection == null) {
            sMockedConnectionOpenUrl = null;
        }
    }

    public static HttpURLConnection createHttpUrlConnection(final URL url) throws IOException {
        if (sMockedConnection != null) {
            sMockedConnectionOpenUrl = url;
            return sMockedConnection;
        }

        return (HttpURLConnection) url.openConnection();
    }

    @VisibleForTesting
    public static URL getMockedConnectionOpenUrl() {
        return sMockedConnectionOpenUrl;
    }
}
