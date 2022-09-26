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
import static com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeInternal.SerializedNames.KID;
import static com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeInternal.SerializedNames.NONCE;
import static com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeInternal.SerializedNames.URL;

import com.google.gson.annotations.SerializedName;
import com.nimbusds.jose.util.Base64URL;

import org.json.JSONObject;

import java.net.URL;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * Internal representation of PoP Authentication Scheme where Key is owned by
 * Client application.
 */
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@Accessors(prefix = "m")
public class PopAuthenticationSchemeWithClientKeyInternal
        extends TokenAuthenticationScheme
        implements IPoPAuthenticationSchemeParams {

    private static final long serialVersionUID = 788393037295696359L;
    /**
     * The name of this auth scheme as supplied in the Authorization header value.
     */
    public static final String SCHEME_POP_WITH_CLIENT_KEY = "PoP_With_Client_Key";

    @SerializedName(HTTP_METHOD)
    private String mHttpMethod;

    @SerializedName(URL)
    private URL mUrl;

    @SerializedName(NONCE)
    private String mNonce;

    @SerializedName(CLIENT_CLAIMS)
    private String mClientClaims;

    @SerializedName(KID)
    private String mKid;

    /**
     * Constructor for gson use.
     */
    PopAuthenticationSchemeWithClientKeyInternal() {
        super(SCHEME_POP_WITH_CLIENT_KEY);
    }

    /**
     * Constructs a new PopAuthenticationSchemeWithClientKeyInternal.
     *
     * @param httpMethod   The HTTP method associated with this request. Optional.
     * @param url          The resource URL of future-recipient of this SHR.
     * @param nonce        Client nonce value; for replay protection.
     * @param clientClaims Optional claims provided by the caller to embed in the client_claims
     *                     property of the resulting SHR.
     */
    public PopAuthenticationSchemeWithClientKeyInternal(@Nullable final String httpMethod,
                                                        @NonNull final URL url,
                                                        @Nullable final String nonce,
                                                        @Nullable final String clientClaims,
                                                        @NonNull final String kid) {
        super(SCHEME_POP_WITH_CLIENT_KEY);
        mHttpMethod = httpMethod;
        mUrl = url;
        mNonce = nonce;
        mClientClaims = clientClaims;
        mKid = kid;
    }

    @Override
    public String getAccessTokenForScheme(@NonNull final String accessToken) {
        return accessToken;
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

    public String getKid() {
        return mKid;
    }

    /**
     * Gets the req_cnf value to be sent to server in token request
     * @return json representation including the kid info for req_cnf
     */
    public String getRequestConfirmation() {
        final String reqCnfJson = new JSONObject().put(KID, mKid).toString();
        return Base64URL.encode(reqCnfJson).toString();
    }
}
