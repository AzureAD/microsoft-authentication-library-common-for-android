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

import com.google.gson.annotations.SerializedName;

import java.net.URL;
import java.util.UUID;

import static com.microsoft.identity.common.internal.authscheme.ProofOfPossessionParameters.SerializedNames.HTTP_METHOD;
import static com.microsoft.identity.common.internal.authscheme.ProofOfPossessionParameters.SerializedNames.NONCE;
import static com.microsoft.identity.common.internal.authscheme.ProofOfPossessionParameters.SerializedNames.URL;

/**
 * Required Params for a Proof-of-Possession protected request.
 */
public class ProofOfPossessionParameters
        extends AuthenticationSchemeParameters
        implements IPoPAuthenticationSchemeParams {

    public static final class SerializedNames {
        public static final String HTTP_METHOD = "http_method";
        public static final String URL = "url";
        public static final String NONCE = "nonce";
    }

    @SerializedName(HTTP_METHOD)
    private final String mHttpMethod;

    @SerializedName(URL)
    private final URL mUrl;

    @SerializedName(NONCE)
    private final String mNonce;

    /**
     * Constructs a new ProofOfPossessionParameters.
     *
     * @param method The HTTP method of the resource request.
     * @param url    The URL of PoP token recipient (resource).
     */
    public ProofOfPossessionParameters(@NonNull final URL url,
                                       @NonNull final String method) {
        super(PopAuthenticationSchemeInternal.SCHEME_POP);
        mUrl = url;
        mHttpMethod = method;
        mNonce = UUID.randomUUID().toString();
    }

    /**
     * Gets the HTTP method.
     *
     * @return The HttpMethod to get.
     */
    @Override
    public String getHttpMethod() {
        return mHttpMethod;
    }

    /**
     * Gets the url.
     *
     * @return The url to get.
     */
    @Override
    public URL getUrl() {
        return mUrl;
    }

    /**
     * Gets the nonce.
     *
     * @return The nonce to get.
     */
    @Override
    public String getNonce() {
        return mNonce;
    }
}
