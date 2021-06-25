package com.microsoft.identity.common.exception;

import javax.annotation.Nullable;

/**
 * An interface that indicates that there is structured data that can be exctracted from the
 * containing class.  Specifically, this indicates the presence of a string that represents
 * the error code to return to the user.  This interface is specifically written with descendants
 * of (@link Throwable} in mind.
 */
public interface IErrorInformation {
    /**
     * Get the error code associated with this operation.  May be null.
     * @return the associated error code.
     */
    @Nullable
    String getErrorCode();
}
