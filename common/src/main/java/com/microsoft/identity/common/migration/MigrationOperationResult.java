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
package com.microsoft.identity.common.migration;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * The result of a cache reencryption operation.
 */
@Accessors(prefix = "m")
class MigrationOperationResult implements IMigrationOperationResult {

    /**
     * The total number of records we attempted to migrate.
     */
    @Getter
    @Setter
    private int mCountOfTotalRecords;

    /**
     * The count of records that could not be successfully migrated.
     */
    @Getter
    private int mCountOfFailedRecords;

    /**
     * Errors encountered during migration, a {@link Map} indicating unique errors + count of
     * occurrences.
     */
    @Getter
    private Map<String, Integer> mFailures = new HashMap<>();

    void addFailure(@NonNull final Exception exception) {
        final String exceptionKey = createExceptionKey(exception);
        final Integer currentCountOfException = mFailures.get(exceptionKey);

        if (null == currentCountOfException) {
            mFailures.put(exceptionKey, 1); // First time we've hit this error
        } else {
            mFailures.put(exceptionKey, currentCountOfException + 1); // increment
        }

        // Increment failure count...
        mCountOfFailedRecords++;
    }

    private static String createExceptionKey(@NonNull final Exception exception) {
        final String simpleName = exception.getClass().getSimpleName();
        final String msg = exception.getMessage();
        return simpleName + "::" + msg;
    }
}
