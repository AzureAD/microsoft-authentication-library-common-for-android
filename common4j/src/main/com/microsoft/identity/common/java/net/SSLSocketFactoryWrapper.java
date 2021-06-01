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

import com.microsoft.identity.common.java.logging.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.SocketFactory;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import lombok.experimental.Accessors;

/**
 * This class is a SSLSocketFactory wrapper that explicitly enables TLSv1.2.
 * In Android, the default socket would return one that only supports up to TLSv1.1 if API<20
 * reference: https://developer.android.com/reference/javax/net/ssl/SSLSocket
 */
public class SSLSocketFactoryWrapper extends SSLSocketFactory {
    private static final String TAG = SSLSocketFactoryWrapper.class.getSimpleName();

    private static final SSLSocketFactoryWrapper sDefault = new SSLSocketFactoryWrapper((SSLSocketFactory) getDefault());

    // Gets TLS version of the latest-established socket connection. For testing only.
    // NOTE: This onMethod thing doesn't generate javadoc, but this method is only exposed for testing only.
    @Getter(value = AccessLevel.PACKAGE, onMethod_={@Synchronized})
    @Setter(value = AccessLevel.PACKAGE, onMethod_={@Synchronized})
    @Accessors(prefix = "s")
    static String sLastHandshakeTLSversion = "";

    private static final String[] SUPPORTED_SSL_PROTOCOLS = new String[]{"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"};

    private final SSLSocketFactory mBaseSocketFactory;

    private SSLSocketFactoryWrapper(SSLSocketFactory baseSocketFactory) {
        mBaseSocketFactory = baseSocketFactory;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return mBaseSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return mBaseSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return modifyEnabledSockets(mBaseSocketFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return modifyEnabledSockets(mBaseSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return modifyEnabledSockets(mBaseSocketFactory.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return modifyEnabledSockets(mBaseSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return modifyEnabledSockets(mBaseSocketFactory.createSocket(address, port, localAddress, localPort));
    }

    /**
     * Returns a Socket with enabled protocols that include TLSv1.2
     *
     * @param socket {@link Socket} Socket to be modified to enable TLSv1.2
     * @return Socket
     */
    private Socket modifyEnabledSockets(Socket socket) {
        if (socket instanceof SSLSocket) {
            final SSLSocket sslSocket = (SSLSocket) socket;
            sslSocket.setEnabledProtocols(SUPPORTED_SSL_PROTOCOLS);
            sslSocket.addHandshakeCompletedListener(new HandshakeCompletedListener() {
                @Override
                public void handshakeCompleted(final HandshakeCompletedEvent event) {
                    setLastHandshakeTLSversion(event.getSession().getProtocol());
                }
            });
        }
        return socket;
    }

    /**
     * Returns a {@link SSLSocketFactoryWrapper}.
     */
    @NonNull
    public static synchronized SSLSocketFactory getSocketFactory(@Nullable SSLContext context) {
        final String methodName = "getSocketFactory";
        Logger.verbose(TAG + methodName, "getting SSLSocketFactory.");

        if (context == null){
            return sDefault;
        }

        final SocketFactory factory = context.getSocketFactory();
        if (factory == null){
            Logger.warn(TAG + methodName, "Failed to construct a SSLSocketFactory from SSLContext, returns the default one.");
            return sDefault;
        }

        return new SSLSocketFactoryWrapper((SSLSocketFactory) context.getSocketFactory());
    }
}
