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
package com.microsoft.identity.common.exception;

import com.microsoft.identity.common.adal.internal.net.HttpWebResponse;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.adal.internal.util.HashMapExtensions;

import org.json.JSONException;

import java.util.HashMap;
import java.util.List;

public class CommonCoreBaseException extends Exception {
    private String mErrorCode;

    /**
     * Default constructor.
     */
    CommonCoreBaseException() {
    }

    /**
     * Initiates the detailed error code.
     *
     * @param errorCode The error code contained in the exception.
     */
    public CommonCoreBaseException(final String errorCode) {
        mErrorCode = errorCode;
    }

    /**
     * Initiates the {@link CommonCoreBaseException} with error code and error message.
     *
     * @param errorCode    The error code contained in the exception.
     * @param errorMessage The error message contained in the exception.
     */
    public CommonCoreBaseException(final String errorCode, final String errorMessage) {
        super(errorMessage);
        mErrorCode = errorCode;
    }

    /**
     * Initiates the {@link CommonCoreBaseException} with error code, error message and throwable.
     *
     * @param errorCode    The error code contained in the exception.
     * @param errorMessage The error message contained in the exception.
     * @param throwable    The {@link Throwable} contains the cause for the exception.
     */
    public CommonCoreBaseException(final String errorCode, final String errorMessage,
                  final Throwable throwable) {
        super(errorMessage, throwable);
        mErrorCode = errorCode;
    }

    /**
     * @return The error code for the exception, could be null. {@link CommonCoreBaseException} is the top level base exception, for the
     * constants value of all the error code.
     */
    public String getErrorCode() {
        return mErrorCode;
    }

    /**
     * {@inheritDoc}
     * Return the detailed description explaining why the exception is returned back.
     */
    @Override
    public String getMessage() {
        if (!StringExtensions.isNullOrBlank(super.getMessage())) {
            return super.getMessage();
        }

        return "";
    }

    protected int mHttpStatusCode;

    protected HashMap<String, String> mHttpResponseBody = null;

    protected HashMap<String, List<String>> mHttpResponseHeaders = null;

    /**
     * When {@link java.net.SocketTimeoutException} is thrown, no status code will be caught. Will use 0 instead.
     */
    static final int DEFAULT_STATUS_CODE = 0;
    /**
     * @return The http status code for the request sent to the service.
     */
    public int getHttpStatusCode() {
        return mHttpStatusCode;
    }

    /**
     * Gets the response body that may be returned by the service.
     *
     * @return response body map, null if not initialized.
     */
    public HashMap<String, String> getHttpResponseBody() {
        return mHttpResponseBody;
    }

    /**
     * Get the response headers that indicated an error.
     *
     * @return The response headers for the network call, null if not initialized.
     */
    public HashMap<String, List<String>> getHttpResponseHeaders() {
        return mHttpResponseHeaders;
    }

    public void setHttpResponse(HttpWebResponse response) {
        if (null != response) {
            mHttpStatusCode = response.getStatusCode();

            if (null != response.getResponseHeaders()) {
                mHttpResponseHeaders = new HashMap<>(response.getResponseHeaders());
            }

            if (null != response.getBody()) {
                try {
                    mHttpResponseBody = new HashMap<>(HashMapExtensions.getJsonResponse(response));
                } catch (final JSONException exception) {
                    //Log.e(CommonCoreBaseException.class.getSimpleName(), ADALError.SERVER_INVALID_JSON_RESPONSE.toString(), exception);
                }
            }
        }
    }
}
