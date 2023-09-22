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
package com.microsoft.identity.common.java.opentelemetry;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;

public interface ITelemetryHelper {
    /**
     * Indicates on the Span that the operation was successful and then ends current Span.
     */
    //Suppressing warnings for RETURN_VALUE_IGNORED_NO_SIDE_EFFECT
    @SuppressFBWarnings
    void setResultSuccess();

    /**
     * Indicates on the Span that the operation failed and then ends current Span.
     * @param message descriptive cause of failure message.
     */
    //Suppressing warnings for RETURN_VALUE_IGNORED_NO_SIDE_EFFECT
    @SuppressFBWarnings
    void setResultFailure(@NonNull final String message);

    /**
     * Indicates on the Span that the operation failed and then ends current Span.
     * @param exception exception thrown upon error.
     */
    //Suppressing warnings for RETURN_VALUE_IGNORED_NO_SIDE_EFFECT
    @SuppressFBWarnings
    void setResultFailure(@NonNull final Exception exception);
}
