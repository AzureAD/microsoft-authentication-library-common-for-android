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
package com.microsoft.identity.common.java.authscheme;

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.logging.Logger;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * Factory class for turning public scheme types into internal representations.
 */
public class AuthenticationSchemeFactory {

    private static final String TAG = AuthenticationSchemeFactory.class.getSimpleName();

    /**
     * Gets the internal scheme equivalent for the provided public api scheme.
     *
     * @param commonComponents {@link IPlatformComponents}
     * @param nameable The nameable public scheme representation.
     * @return The internal scheme representation.
     */
    public static AbstractAuthenticationScheme createScheme(@NonNull final IPlatformComponents commonComponents,
                                                            @Nullable final INameable nameable) throws ClientException {
        final String methodTag = TAG + ":createScheme";
        if (null == nameable) {
            // If null, choose Bearer for backcompat
            return new BearerAuthenticationSchemeInternal();
        }

        switch (nameable.getName()) {
            case BearerAuthenticationSchemeInternal.SCHEME_BEARER:
                Logger.verbose(
                        methodTag,
                        "Constructing Bearer Authentication Scheme."
                );

                return new BearerAuthenticationSchemeInternal();

            case PopAuthenticationSchemeInternal.SCHEME_POP:
                if (nameable instanceof IPoPAuthenticationSchemeParams) {
                    Logger.verbose(
                            methodTag,
                            "Constructing PoP Authentication Scheme."
                    );

                    final IPoPAuthenticationSchemeParams params = (IPoPAuthenticationSchemeParams) nameable;
                    return new PopAuthenticationSchemeInternal(
                            commonComponents.getClockSkewManager(),
                            commonComponents.getDefaultDevicePopManager(),
                            params.getHttpMethod(),
                            params.getUrl(),
                            params.getNonce(),
                            params.getClientClaims()
                    );
                } else {
                    throw new IllegalStateException("Unrecognized parameter type.");
                }

            case PopAuthenticationSchemeWithClientKeyInternal.SCHEME_POP_WITH_CLIENT_KEY:
                if (nameable instanceof IPoPAuthenticationSchemeParams) {
                    Logger.verbose(
                            methodTag,
                            "Constructing PoP Authentication Scheme With Client Key."
                    );

                    final IPoPAuthenticationSchemeParams params = (IPoPAuthenticationSchemeParams) nameable;
                        return new PopAuthenticationSchemeWithClientKeyInternal(
                                params.getHttpMethod(),
                                params.getUrl(),
                                params.getNonce(),
                                params.getClientClaims(),
                                params.getClientClaims()
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

    /**
     * Checks the given authentication scheme is a PoP authentication scheme or not
     * @param authenticationScheme
     * @return boolean indicating if the the authentication scheme is a PoP authentication scheme
     */
    public static boolean isPopAuthenticationScheme(@NonNull final AbstractAuthenticationScheme authenticationScheme) {
        return authenticationScheme instanceof PopAuthenticationSchemeInternal
                || authenticationScheme instanceof PopAuthenticationSchemeWithClientKeyInternal;
    }
}
