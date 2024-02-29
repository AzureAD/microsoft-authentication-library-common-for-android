//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.net;

public final class HttpConstants {

    /**
     * HTTP header fields.
     */
    public static final class HeaderField {

        /**
         * @see <a href="https://tools.ietf.org/html/rfc1945#appendix-D.2.1">RFC-1945</a>
         */
        public static final String ACCEPT = "Accept";

        /**
         * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">RFC-2616</a>
         */
        public static final String CONTENT_TYPE = "Content-Type";

        /**
         * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">RFC-2616</a>
         */
        public static final String CONTENT_LENGTH = "Content-Length";

        /**
         * Header used to track SPE Ring for telemetry.
         */
        public static final String X_MS_CLITELEM = "x-ms-clitelem";

        /**
         * Header to track if Cached Credential Service (CCS) was used
         */
        public static final String XMS_CCS_REQUEST_ID = "xms-ccs-requestid";

        /**
         * Header to track if Cached Credential Service (CCS) request sequence
         */
        public static final String XMS_CCS_REQUEST_SEQUENCE = "x-ms-srs";
    }

    /**
     * Identifiers for file formats and format contents.
     */
    public static final class MediaType {

        /**
         * @see <a href="https://tools.ietf.org/html/rfc7159">RFC-7159</a>
         */
        public static final String APPLICATION_JSON = "application/json";
    }
}
