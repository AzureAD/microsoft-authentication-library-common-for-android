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

import static com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeInternal.SerializedNames.CLIENT_CLAIMS;
import static com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeInternal.SerializedNames.HTTP_METHOD;
import static com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeInternal.SerializedNames.NONCE;
import static com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeInternal.SerializedNames.URL;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.crypto.IDevicePopManager;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.util.IClockSkewManager;

import java.net.URL;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * Internal representation of PoP Authentication Scheme.
 */
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@Accessors(prefix = "m")
public class PopAuthenticationSchemeInternal
        extends TokenAuthenticationScheme
        implements IPoPAuthenticationSchemeParams {

    private static final long serialVersionUID = 788393037295696358L;

    public static final class SerializedNames {
        public static final String HTTP_METHOD = "http_method";
        public static final String URL = "url";
        public static final String NONCE = "nonce";
        public static final String CLIENT_CLAIMS = "client_claims";
        public static final String KID = "kid";
    }

    /**
     * The name of this auth scheme as supplied in the Authorization header value.
     */
    public static final String SCHEME_POP = "PoP";

    // Transient because this class maintains a reference to a Context
    private transient IClockSkewManager mClockSkewManager;

    @SerializedName(HTTP_METHOD)
    private String mHttpMethod;

    @SerializedName(URL)
    private URL mUrl;

    @SerializedName(NONCE)
    private String mNonce;

    @SerializedName(CLIENT_CLAIMS)
    private String mClientClaims;

    private transient IDevicePopManager mPopManager;

    /**
     * Constructor for gson use.
     */
    PopAuthenticationSchemeInternal() {
        super(SCHEME_POP);
    }

    /**
     * Constructs a new PopAuthenticationSchemeInternal.
     *
     * @param clockSkewManager Used to compute and compensate for any client clock-skew
     *                         (relative to AAD).
     * @param popManager
     * @param httpMethod       The HTTP method associated with this request. Optional.
     * @param url              The resource URL of future-recipient of this SHR.
     * @param nonce            Client nonce value; for replay protection.
     * @param clientClaims     Optional claims provided by the caller to embed in the client_claims
     *                         property of the resulting SHR.
     */
    public PopAuthenticationSchemeInternal(@NonNull final IClockSkewManager clockSkewManager,
                                           @NonNull final IDevicePopManager popManager,
                                           @Nullable final String httpMethod,
                                           @NonNull final URL url,
                                           @Nullable final String nonce,
                                           @Nullable final String clientClaims) {
        super(SCHEME_POP);
        mClockSkewManager = clockSkewManager;
        mPopManager = popManager;
        mHttpMethod = httpMethod;
        mUrl = url;
        mNonce = nonce;
        mClientClaims = clientClaims;
    }

    /**
     * This constructor intended to be used when this class is acting as a DTO between
     * MSAL to Broker. Because no {@link IClockSkewManager} is supplied, functions related to access
     * token signing cannot be used.
     *
     * @param popManager
     * @param httpMethod   The HTTP method associated with this request. Optional.
     * @param url          The resource URL of future-recipient of this SHR.
     * @param nonce        Client nonce value; for replay protection.
     * @param clientClaims Optional claims provided by the caller to embed in the client_claims
     *                     property of the resulting SHR.
     */
    public PopAuthenticationSchemeInternal(@NonNull final IDevicePopManager popManager,
                                           @Nullable final String httpMethod,
                                           @NonNull final URL url,
                                           @Nullable final String nonce,
                                           @Nullable final String clientClaims) {
        super(SCHEME_POP);
        mClockSkewManager = null;
        mPopManager = popManager;
        mHttpMethod = httpMethod;
        mUrl = url;
        mNonce = nonce;
        mClientClaims = clientClaims;
    }

    @Override
    public String getAccessTokenForScheme(@NonNull final String accessToken) throws ClientException {
        if (null == mClockSkewManager) {
            // Shouldn't happen, would indicate a development-time bug.
            throw new RuntimeException("IClockSkewManager not initialized.");
        }

        final long ONE_SECOND_MILLIS = 1000L;
        final long timestampMillis = mClockSkewManager.getAdjustedReferenceTime().getTime();

        return mPopManager.mintSignedAccessToken(
                getHttpMethod(),
                timestampMillis / ONE_SECOND_MILLIS,
                getUrl(),
                accessToken,
                getNonce(),
                getClientClaims()
        );
    }

    public void setClockSkewManager(@NonNull final IClockSkewManager clockSkewManager) {
        mClockSkewManager = clockSkewManager;
    }

    public void setDevicePopManager(@NonNull final IDevicePopManager devicePopManager){
        mPopManager = devicePopManager;
    }

    @Override
    @Nullable
    public String getHttpMethod() {
        return mHttpMethod;
    }

    @Override
    public URL getUrl() {
        return mUrl;
    }

    @Override
    public String getClientClaims() {
        return mClientClaims;
    }

    @Override
    @Nullable
    public String getNonce() {
        return mNonce;
    }
}
