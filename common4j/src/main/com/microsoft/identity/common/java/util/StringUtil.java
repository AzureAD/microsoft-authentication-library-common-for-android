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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import lombok.NonNull;

import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8;
import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8_STRING;

public class StringUtil {
    private static String TAG = StringUtil.class.getSimpleName();

    /**
     * Checks if string is null or empty.
     *
     * @param message String to check for null or blank.
     * @return true, if the string is null or blank.
     */
    public static boolean isNullOrEmpty(String message) {
        return message == null || message.trim().length() == 0;
    }

    /**
     * Perform URL decode on the given source.
     *
     * @param source The String to decode for.
     * @return The decoded string.
     * @throws UnsupportedEncodingException If encoding is not supported.
     */
    public static String urlFormDecode(final String source) throws UnsupportedEncodingException {
        if (isNullOrEmpty(source)) {
            return "";
        }

        return URLDecoder.decode(source, ENCODING_UTF8_STRING);
    }

    /**
     * Get a string from the given exception.
     *
     * @param exception an exception object to extract a stack trace string from.
     * @return A stack trace string
     */
    public static String getStackTraceAsString(@NonNull final Exception exception) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return pw.toString();
    }

    public static String fromByteArray(@NonNull final byte[] bytes){
        return new String(bytes, ENCODING_UTF8);
    }

    public static byte[] toByteArray(@NonNull final String string){
        return string.getBytes(ENCODING_UTF8);
    }
}
