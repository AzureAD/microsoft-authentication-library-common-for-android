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
package com.microsoft.identity.common.java.exception;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * A RuntimeException derivative indicating that the command cannot continue.  This is largely
 * written to be thrown in situations where there is not clear path to handling the error except
 * to abort whatever command is in progress and return an error to the user, short circuiting
 * whatever other error handling may be present in the code.
 */
@Getter
@Accessors(prefix = "m")
public class TerminalException extends RuntimeException implements IErrorInformation {

    private final String mErrorCode;

    /**
     * Construct a TerminalException with a message and cause.
     *
     * @param message   the exception message.
     * @param cause     the causing exception.  Should not be null.
     * @param errorCode the error code.  May not be null.  This should be an error string from {@link ClientException}.
     */
    public TerminalException(final @Nullable String message,
                             final @NonNull Throwable cause,
                             final @NonNull String errorCode) {
        super(message, cause);
        this.mErrorCode = errorCode;
    }

    /**
     * Construct a TerminalException with a cause.
     *
     * @param cause     the causing exception.  Should not be null.
     * @param errorCode the error code.  May not be null.  This should be an error string from {@link ClientException}.
     */
    public TerminalException(final @NonNull Throwable cause,
                             final @NonNull String errorCode) {
        super(cause);
        this.mErrorCode = errorCode;
    }

    /**
     * Construct a TerminalException.
     *
     * @param errorCode the error code.  May not be null.
     */
    public TerminalException(final @NonNull String errorCode) {
        super();
        this.mErrorCode = errorCode;
    }

    /**
     * Construct a TerminalException with a message.
     *
     * @param message   the exception message.
     * @param errorCode the error code.  May not be null.  This should be an error string from {@link ClientException}.
     */
    public TerminalException(final @Nullable String message,
                             final @NonNull String errorCode) {
        super(message);
        this.mErrorCode = errorCode;
    }
}
