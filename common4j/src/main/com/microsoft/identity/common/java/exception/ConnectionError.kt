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

enum class ConnectionError(val value: String) {
    FAILED_TO_OPEN_CONNECTION("ce_failed_to_open_connection"),
    FAILED_TO_SET_REQUEST_METHOD("ce_failed_to_set_request_method"),
    FAILED_TO_WRITE_TO_OUTPUT_STREAM("ce_failed_to_write_to_output_stream"),
    FAILED_TO_READ_FROM_INPUT_STREAM("ce_failed_to_read_from_input_stream"),
    FAILED_TO_GET_RESPONSE_CODE("ce_failed_to_get_response_code"),
    CONNECTION_TIMEOUT("ce_connection_timeout");

    /**
     * Converts this [ConnectionError] into a [ClientException]
     **/
    fun getClientException(cause: Throwable): ClientException {
        val e = ClientException(
            ClientException.IO_ERROR,
            "An IO error occurred in the network layer: " + cause.message,
            cause
        )
        e.subErrorCode = value
        return e
    }

    /**
     * Returns true if the given [Throwable] is a connection error.
     **/
    fun compare(throwable: Throwable): Boolean {
        if (throwable !is ClientException){
            return false
        }

        return this.value == throwable.subErrorCode
    }
}
