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
package com.microsoft.identity.common.internal.authscheme;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.exception.ClientException;

import java.net.URL;

/**
 * Internal representation of PoP Authentication Scheme.
 */
public class PopAuthenticationSchemeInternal
        extends TokenAuthenticationScheme
        implements IPoPAuthenticationSchemeParams {

    /**
     * The name of this auth scheme as supplied in the Authorization header value.
     */
    public static final String SCHEME_POP = "PoP";

    /**
     * User supplied params.
     */
    private final IPoPAuthenticationSchemeParams mParams;

    /**
     * Delegate object for handling PoP-related crypto/HSM functions.
     */
    private transient IDevicePopManager mPopManager;

    /**
     * Constructs a new PopAuthenticationSchemeInternal.
     *
     * @param params The params from which to derive this object.
     */
    PopAuthenticationSchemeInternal(@NonNull final IPoPAuthenticationSchemeParams params) {
        super(SCHEME_POP);
        mParams = params;
    }

    /**
     * Sets the DevicePopManager delegate instance
     *
     * @param popManager The delegate to set.
     */
    public void setDevicePopManager(@NonNull final IDevicePopManager popManager) {
        mPopManager = popManager;
    }

    @Override
    public String getAuthorizationRequestHeader() throws ClientException {
        return getName()
                + " "
                + mPopManager.getAuthorizationHeaderValue(
                getHttpMethod(),
                getUrl(),
                getAccessToken(),
                getNonce()
        );
    }

    @Override
    public String getHttpMethod() {
        return mParams.getHttpMethod();
    }

    @Override
    public URL getUrl() {
        return mParams.getUrl();
    }

    @Override
    public String getNonce() {
        return mParams.getNonce();
    }
}
