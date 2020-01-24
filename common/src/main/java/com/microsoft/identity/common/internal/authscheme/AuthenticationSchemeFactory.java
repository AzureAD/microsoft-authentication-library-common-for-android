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

import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.logging.Logger;

/**
 * Factory class for turning public scheme types into internal representations.
 */
public class AuthenticationSchemeFactory {

    private static final String TAG = AuthenticationSchemeFactory.class.getSimpleName();

    /**
     * Gets the internal scheme equivalent for the provided public api scheme.
     *
     * @param nameable The nameable public scheme representation.
     * @return The internal scheme representation.
     */
    public static AbstractAuthenticationScheme createScheme(@Nullable final INameable nameable) {
        if (null == nameable) {
            // If null, choose Bearer for backcompat
            return new BearerAuthenticationSchemeInternal();
        }

        switch (nameable.getName()) {
            case BearerAuthenticationSchemeInternal.SCHEME_BEARER:
                Logger.verbose(
                        TAG,
                        "Constructing Bearer Authentication Scheme."
                );

                return new BearerAuthenticationSchemeInternal();

            case PopAuthenticationSchemeInternal.SCHEME_POP:
                if (nameable instanceof IPoPAuthenticationSchemeParams) {
                    Logger.verbose(
                            TAG,
                            "Constructing PoP Authentication Scheme."
                    );

                    final IPoPAuthenticationSchemeParams params = (IPoPAuthenticationSchemeParams) nameable;

                    return new PopAuthenticationSchemeInternal(
                            params.getHttpMethod(),
                            params.getUrl(),
                            params.getNonce()
                    );
                } else {
                    throw new IllegalStateException("Unrecognized parameter type.");
                }

            default:
                throw new UnsupportedOperationException(
                        "Unknown or unsupported scheme: "
                                + nameable.getName()
                );
        }
    }

}
