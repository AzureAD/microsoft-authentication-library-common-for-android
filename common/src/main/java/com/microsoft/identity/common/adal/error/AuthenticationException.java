package com.microsoft.identity.common.adal.error;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import android.content.Context;


/**
 * ADAL exception.
 */
public class AuthenticationException extends Exception {
    static final long serialVersionUID = 1;

    private ADALError mCode;

    /**
     * Default constructor for {@link AuthenticationException}.
     */
    public AuthenticationException() {
        // Default constructor, intentionally empty.
    }

    /**
     * Constructs a new AuthenticationException with error code.
     *
     * @param code {@link ADALError}
     */
    public AuthenticationException(ADALError code) {
        mCode = code;
    }

    /**
     * @param code Resource file related error code. Message will be derived
     *            from resource with using app context
     * @param details Details related to the error such as query string, request
     *            info
     */
    public AuthenticationException(ADALError code, String details) {
        super(details);
        mCode = code;
    }

    /**
     * @param code Resource file related error code. Message will be derived
     *            from resource with using app context
     * @param details Details related to the error such as query string, request
     *            info
     * @param throwable {@link Throwable}
     */
    public AuthenticationException(ADALError code, String details, Throwable throwable) {
        super(details, throwable);
        mCode = code;
    }

    /**
     * Gets {@link ADALError} code.
     *
     * @return {@link ADALError} code
     */
    public ADALError getCode() {
        return mCode;
    }

    @Override
    public String getMessage() {
        return getLocalizedMessage(null);
    }

    /**
     * Gets localized {@link ADALError} code if provided by context.
     *
     * @param context {@link Context}
     * @return Error message
     */
    public String getLocalizedMessage(Context context) {
        if (!StringExtensions.isNullOrBlank(super.getMessage())) {
            return super.getMessage();
        }
        if (mCode != null) {
            return mCode.getLocalizedDescription(context);
        }

        return null;
    }
}

