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
package com.microsoft.identity.common.internal.dto;

import com.google.gson.annotations.SerializedName;

import static com.microsoft.identity.common.internal.dto.AccessToken.SerializedNames.ACCESS_TOKEN_TYPE;
import static com.microsoft.identity.common.internal.dto.AccessToken.SerializedNames.AUTHORITY;
import static com.microsoft.identity.common.internal.dto.AccessToken.SerializedNames.CLIENT_INFO;
import static com.microsoft.identity.common.internal.dto.AccessToken.SerializedNames.EXTENDED_EXPIRES_ON;
import static com.microsoft.identity.common.internal.dto.AccessToken.SerializedNames.REALM;
import static com.microsoft.identity.common.internal.dto.AccessToken.SerializedNames.TARGET;
import static com.microsoft.identity.common.internal.dto.Credential.SerializedNames.EXPIRES_ON;

public class AccessToken extends Credential {

    public static class SerializedNames extends Credential.SerializedNames {
        /**
         * String of access token type.
         */
        public static final String ACCESS_TOKEN_TYPE = "access_token_type";

        /**
         * String of authority.
         */
        public static final String AUTHORITY = "authority";

        /**
         * String of client info.
         */
        public static final String CLIENT_INFO = "client_info";

        /**
         * String of extended expires on.
         */
        public static final String EXTENDED_EXPIRES_ON = "extended_expires_on";

        /**
         * String of realm.
         */
        public static final String REALM = "realm";

        /**
         * String of target.
         */
        public static final String TARGET = "target";
    }

    /**
     * The access token type provides the client with the information required to successfully
     * utilize the access token to make a protected resource request (along with type-specific
     * attributes).
     */
    @SerializedName(ACCESS_TOKEN_TYPE)
    private String mAccessTokenType;

    /**
     * Full authority URL of the issuer.
     */
    @SerializedName(AUTHORITY)
    private String mAuthority;

    /**
     * Full base64 encoded client info received from ESTS, if available. STS returns the clientInfo 
     * on both v1 and v2 for AAD. This field is used for extensibility purposes.
     */
    @SerializedName(CLIENT_INFO)
    private String mClientInfo;

    /**
     * Additional extended expiry time until when token is valid in case of server-side outage.
     */
    @SerializedName(EXTENDED_EXPIRES_ON)
    private String mExtendedExpiresOn;

    /**
     * Full tenant or organizational identifier that account belongs to. Can be null.
     */
    @SerializedName(REALM)
    private String mRealm;

    /**
     * Permissions that are included in the token. Formats for endpoints will be different. 
     * <p>
     * Mandatory, if credential is scoped down by some parameters or requirements (e.g. by
     * resource, scopes or permissions).
     */
    @SerializedName(TARGET)
    private String mTarget;

    /**
     * Token expiry time. This value should be calculated based on the current UTC time measured
     * locally and the value expires_in returned from the service. Measured in milliseconds from
     * epoch (1970).
     */
    @SerializedName(EXPIRES_ON)
    private String mExpiresOn;

    /**
     * Gets the realm.
     *
     * @return The realm to get.
     */
    public String getRealm() {
        return mRealm;
    }

    /**
     * Sets the realm.
     *
     * @param realm The realm to set.
     */
    public void setRealm(final String realm) {
        mRealm = realm;
    }

    /**
     * Gets the target.
     *
     * @return The target to get.
     */
    public String getTarget() {
        return mTarget;
    }

    /**
     * Sets the target.
     *
     * @param target The target to set.
     */
    public void setTarget(final String target) {
        mTarget = target;
    }

    /**
     * Gets the client_info.
     *
     * @return The client_info to get.
     */
    public String getClientInfo() {
        return mClientInfo;
    }

    /**
     * Sets the client_info.
     *
     * @param clientInfo The clent_info to set.
     */
    public void setClientInfo(final String clientInfo) {
        mClientInfo = clientInfo;
    }

    /**
     * Gets the access_token_type.
     *
     * @return The access_token_type to get.
     */
    public String getAccessTokenType() {
        return mAccessTokenType;
    }

    /**
     * Sets the access_token_type.
     *
     * @param accessTokenType The access_token_type to set.
     */
    public void setAccessTokenType(final String accessTokenType) {
        mAccessTokenType = accessTokenType;
    }

    /**
     * Gets the authority.
     *
     * @return The authority to get.
     */
    public String getAuthority() {
        return mAuthority;
    }

    /**
     * Sets the authority.
     *
     * @param authority The authority to set.
     */
    public void setAuthority(final String authority) {
        mAuthority = authority;
    }

    /**
     * Gets the extended_expires_on.
     *
     * @return The extended_expires_on to get.
     */
    public String getExtendedExpiresOn() {
        return mExtendedExpiresOn;
    }

    /**
     * Sets the extended_expires_on.
     *
     * @param extendedExpiresOn The extended_expires_on to set.
     */
    public void setExtendedExpiresOn(final String extendedExpiresOn) {
        mExtendedExpiresOn = extendedExpiresOn;
    }

    /**
     * Gets the expires_on.
     *
     * @return The expires_on to get.
     */
    public String getExpiresOn() {
        return mExpiresOn;
    }

    /**
     * Sets the expires_on.
     *
     * @param expiresOn The expires_on to set.
     */
    public void setExpiresOn(final String expiresOn) {
        mExpiresOn = expiresOn;
    }
}
