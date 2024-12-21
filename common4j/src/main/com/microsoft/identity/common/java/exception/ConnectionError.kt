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
package com.microsoft.identity.common.java.exception

import java.io.EOFException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

enum class ConnectionError(val value: String) {
    NO_NETWORK("ce_no_network"),
    NETWORK_TEMPORARILY_UNAVAILABLE("ce_network_temporarily_unavailable"),
    UNEXPECTED_EXCEPTION("ce_unexpected_exception"),
    CONNECTION_TIMEOUT("ce_connection_timeout");

    /**
     * Returns true if the given [Throwable] is a connection error.
     **/
    fun compare(throwable: Throwable): Boolean {
        if (throwable !is ClientException){
            return false
        }

        return this.value == throwable.subErrorCode
    }

    companion object {
        /**
         * Converts this [ConnectionError] into a [ClientException]
         **/
        @JvmStatic
        fun getClientException(cause: Throwable): ClientException {
            val e = ClientException(
                ClientException.IO_ERROR,
                "An IO error occurred in the network layer: " + cause.message,
                cause
            )

            e.subErrorCode = getConnectionError(cause).value
            return e
        }

        /**
         * Converts a [Throwable] into a suberrorCode
         **/
        private fun getConnectionError(cause: Throwable): ConnectionError {
            if (cause is SocketTimeoutException) {
                // Slow or unreliable network
                return CONNECTION_TIMEOUT
            }

            if (cause is EOFException // Unexpected disconnect
                || cause is SSLException // SSL Handshake failed
                || cause is ConnectException // Remote socket connection failed
            ) {
                return NETWORK_TEMPORARILY_UNAVAILABLE
            }

            if (cause is UnknownHostException // Unable to query DNS for hostname
                || cause is SocketException // No conn to remote socket, or Airplane mode, or no internet permission will hit this
            ) {
                return NO_NETWORK
            }

            return UNEXPECTED_EXCEPTION
        }
    }
}
