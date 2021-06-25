package com.microsoft.identity.common.exception;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.NonNull;

/**
 * A RuntimeException derivative indicating that the command cannot continue.  This is largely
 * written to be thrown in situations where there is not clear path to handling the error except
 * to abort whatever command is in progress and return an error to the user, short circuiting
 * whatever other error handling may be present in the code.
 */
@Getter
public class TerminalException extends RuntimeException implements IErrorInformation {

    final String mErrorCode;
    /**
     * Construct a TerminalException with a message and cause.
     * @param message the exception message.
     * @param cause the causing exception.  Should not be null.
     * @param errorCode the error code.  May be null.  This should be an error string from {@link ClientException}.
     */
    public TerminalException(final @Nullable String message,
                             final @NonNull Throwable cause,
                             final @Nullable String errorCode) {
        super(message, cause);
        this.mErrorCode = errorCode;
    }

    /**
     * Construct a TerminalException with a cause.
     * @param cause the causing exception.  Should not be null.
     * @param errorCode the error code.  May be null.  This should be an error string from {@link ClientException}.
     */
    public TerminalException(final @NonNull Throwable cause,
                             final @Nullable String errorCode) {
        super(cause);
        this.mErrorCode = errorCode;
    }

    @Override
    public @Nullable String getErrorCode() {
        return null;
    }
}
