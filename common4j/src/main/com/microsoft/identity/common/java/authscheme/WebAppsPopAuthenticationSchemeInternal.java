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
package com.microsoft.identity.common.java.authscheme;

import static com.microsoft.identity.common.java.util.StringUtil.isNullOrEmpty;

import com.google.gson.annotations.SerializedName;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * Internal representation of a PoP Authentication Scheme for WebApps (Edge Token Broker).
 * Unlike {@link PopAuthenticationSchemeInternal}, this scheme uses a pre-generated req_cnf
 * provided in the token request, rather than one generated from the device's key store.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@Accessors(prefix = "m")
public class WebAppsPopAuthenticationSchemeInternal
        extends TokenAuthenticationScheme
        implements ITokenAuthenticationSchemeInternal {

    private static final long serialVersionUID = 1L;

    /**
     * The name of this auth scheme. Used to distinguish it from other PoP schemes.
     */
    public static final String SCHEME_POP_PREGENERATED = "PoP_Pregenerated";

    public static class SerializedNames {
        public static final String REQUEST_CONFIRMATION = "req_cnf";
    }

    @SerializedName(SerializedNames.REQUEST_CONFIRMATION)
    private String mRequestConfirmation;

    /**
     * Constructor for gson use. Package-private to restrict direct instantiation
     * while allowing Gson's reflective deserialization to construct instances.
     * {@code mRequestConfirmation} will be populated by Gson during deserialization
     * and must not be used until the object is fully initialized.
     */
    WebAppsPopAuthenticationSchemeInternal() {
        super(SCHEME_POP_PREGENERATED);
    }

    /**
     * Constructs a new WebAppsPopAuthenticationSchemeInternal.
     *
     * @param requestConfirmation The pre-generated request confirmation (req_cnf) value.
     * @throws IllegalArgumentException if requestConfirmation is null or empty.
     */
    public WebAppsPopAuthenticationSchemeInternal(@NonNull final String requestConfirmation) {
        super(SCHEME_POP_PREGENERATED);

        if (isNullOrEmpty(requestConfirmation)) {
            throw new IllegalArgumentException(
                    "requestConfirmation (req_cnf) cannot be null or empty for WebAppsPopAuthenticationSchemeInternal"
            );
        }

        mRequestConfirmation = requestConfirmation;
    }

    /**
     * Returns the access token as-is. For WebApps PoP, the access token returned from ESTS
     * is already in the appropriate PoP format.
     *
     * @param accessToken The access token to return.
     * @return The access token unchanged.
     */
    @Override
    public String getAccessTokenForScheme(@NonNull final String accessToken) {
        return accessToken;
    }
}
