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
package com.microsoft.identity.common.java;

import java.nio.charset.Charset;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public class AuthenticationConstants {

    /**
     * The Constant UTF8.
     */
    public static final String ENCODING_UTF8_STRING = "UTF-8";

    /**
     * The Constant ENCODING_UTF8.
     */
    public static final Charset ENCODING_UTF8 = Charset.forName(ENCODING_UTF8_STRING);

    /**
     * The Constant ASCII.
     */
    public static final String ENCODING_ASCII_STRING = "ASCII";

    /**
     * The Constant CHARSET_ASCII.
     */
    public static final Charset CHARSET_ASCII = Charset.forName(ENCODING_ASCII_STRING);

    /**
     * Represents the constants value for Azure Active Directory.
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class AAD {
        /**
         * String of client request id.
         */
        public static final String CLIENT_REQUEST_ID = "client-request-id";

        /**
         * String of AAD version.
         */
        public static final String AAD_VERSION = "ver";

        /**
         * Constant for v1 endpoint
         */
        public static final String AAD_VERSION_V1 = "1.0";

        /**
         * Constant for v2 endpoint
         */
        public static final String AAD_VERSION_V2 = "2.0";
    }

    /**
     * Sdk platform and Sdk version fields.
     */
    public static final class SdkPlatformFields {
        /**
         * The String representing the sdk version.
         */
        public static final String VERSION = "x-client-Ver";
    }
}
