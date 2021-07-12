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
package com.microsoft.identity.common.adal.internal.tokensharing;

import com.microsoft.identity.common.java.cache.ICacheRecord;

public interface ITokenShareResultInternal {

    /**
     * Export formats for RTs consumed by TSL.
     */
    class TokenShareExportFormatInternal {

        private TokenShareExportFormatInternal() {
            // Container for constants. Don't instantiate.
        }

        /**
         * Used for ORG_ID accounts. Legacy format used by ADAL.
         */
        public static final String SSO_STATE_SERIALIZER_BLOB = "SSO_STATE_SERIALIZER_BLOB";

        /**
         * Raw RT String. Used by MSA format.
         */
        public static final String RAW = "RAW";
    }

    /**
     * Returns the underlying cache records used to create this result.
     *
     * @return The ICacheRecord.
     */
    ICacheRecord getCacheRecord();

    /**
     * Enum capturing the format of the payload returned by {@link #getRefreshToken()}.
     *
     * @return The export format.
     */
    String getFormat();

    /**
     * Gets the refresh token string, in the format returned by {@link #getFormat()}.
     *
     * @return The formatted refresh token value.
     */
    String getRefreshToken();
}
