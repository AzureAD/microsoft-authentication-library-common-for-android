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

import lombok.NonNull;

/**
 * Internal exception representing a Conditional-Access "broker installation required" response
 * (error {@code broker_needs_to_be_installed}) during an interactive request.
 * <p>
 * Unlike the generic {@link ServiceException} that the SDK returns today for this case, this
 * exception carries the WPJ username (UPN) and the Play Store install link so the MAM broker-install
 * request-resume engine can park the request and later replay it silently through the freshly
 * installed broker (Company Portal) with {@code login_hint = UPN}.
 * <p>
 * This exception is only produced when {@code CommonFlight.ENABLE_BROKER_INSTALL_RESUME} is on; with
 * the flight off, the SDK continues to return the pre-existing {@link ServiceException} unchanged. It
 * is internal to the resume path and is not surfaced to the application when the flow is engaged.
 */
public final class BrokerInstallationRequiredException extends BaseException {

    private static final long serialVersionUID = 7401329131099683829L;

    public static final String sName =
            "com.microsoft.identity.common.exception.BrokerInstallationRequiredException";

    /**
     * The Play Store install link ({@code app_link}) for the broker, if available. May be {@code null}
     * because the value is not currently attached to the authorization error response; the actual
     * store launch reads it from the redirect parameters directly.
     */
    private final String mInstallLink;

    /**
     * @param errorCode        the service error code (typically {@code broker_needs_to_be_installed}).
     * @param errorDescription the human-readable error description.
     * @param userName         the WPJ username (UPN) returned by the service; used as {@code login_hint}
     *                         on the resume retry. May be {@code null}.
     * @param installLink      the broker install {@code app_link}, if known. May be {@code null}.
     */
    public BrokerInstallationRequiredException(@NonNull final String errorCode,
                                               @NonNull final String errorDescription,
                                               final String userName,
                                               final String installLink) {
        super(errorCode, errorDescription);
        super.setUsername(userName);
        this.mInstallLink = installLink;
    }

    /**
     * @return the broker install link, or {@code null} if it was not carried on the error response.
     */
    public String getInstallLink() {
        return mInstallLink;
    }

    @Override
    public String getExceptionName() {
        return sName;
    }
}
