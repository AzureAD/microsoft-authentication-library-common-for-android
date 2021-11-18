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
package com.microsoft.identity.common.java.dto;

import static com.microsoft.identity.common.java.dto.AccessTokenRecord.SerializedNames.ACCESS_TOKEN_TYPE;
import static com.microsoft.identity.common.java.dto.AccessTokenRecord.SerializedNames.APPLICATION_IDENTIFIER;
import static com.microsoft.identity.common.java.dto.AccessTokenRecord.SerializedNames.AUTHORITY;
import static com.microsoft.identity.common.java.dto.AccessTokenRecord.SerializedNames.EXTENDED_EXPIRES_ON;
import static com.microsoft.identity.common.java.dto.AccessTokenRecord.SerializedNames.KID;
import static com.microsoft.identity.common.java.dto.AccessTokenRecord.SerializedNames.REALM;
import static com.microsoft.identity.common.java.dto.AccessTokenRecord.SerializedNames.REFRESH_ON;
import static com.microsoft.identity.common.java.dto.AccessTokenRecord.SerializedNames.REQUESTED_CLAIMS;
import static com.microsoft.identity.common.java.dto.AccessTokenRecord.SerializedNames.TARGET;
import static com.microsoft.identity.common.java.dto.AccessTokenRecord.SerializedNames.TOKEN_TYPE;

import com.google.gson.annotations.SerializedName;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class AccessTokenRecord extends Credential {

    public static class SerializedNames extends Credential.SerializedNames {
        /**
         * <strong>Deprecated<strong> string of access token type.  Prefer @link{#TOKEN_TYPE} instead.
         */
        @Deprecated
        public static final String ACCESS_TOKEN_TYPE = "access_token_type";

        /**
         * String of access token type.
         */
        public static final String TOKEN_TYPE = "token_type";

        /**
         * String of authority.
         */
        public static final String AUTHORITY = "authority";

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

        /**
         * String of kid. A thumbprint to an RSA keypair.
         */
        public static final String KID = "kid";

        /**
         * The claims string (if present) that was sent to server to produce this AT.
         */
        public static final String REQUESTED_CLAIMS = "requested_claims";

        /**
         * Client side representation of refresh_in time interval value provided from eSTS response.
         * refresh_on is an epoch time, and is calculated based on the refresh_in interval.
         */
        public static final String REFRESH_ON = "refresh_on";

        /**
         * The packagename/signature tuple of the application to which the access token was issued
         * This is required for True MAM scenarios to ensure that applications that might share a client id
         * can be correctly differentiated from one another relative to True MAM Policy status
         */
        public static final String APPLICATION_IDENTIFIER = "application_identifier";
    }

    /**
     * The JSON claims string sent to the server that produced this token. Used by MSAL C++
     */
    @SerializedName(REQUESTED_CLAIMS)
    private String mRequestedClaims;

    /**
     * A key id associating this credential to a public/private keypair.
     * <p>
     * Refer to {@link IDevicePopManager#getAsymmetricKeyThumbprint()}.
     */
    @SerializedName(KID)
    private String mKid;

    /**
     * The access token type provides the client with the information required to successfully
     * utilize the access token to make a protected resource request (along with type-specific
     * attributes).
     */
    @SerializedName(value = TOKEN_TYPE, alternate = ACCESS_TOKEN_TYPE)
    private String mAccessTokenType;

    /**
     * Full authority URL of the issuer.
     */
    @SerializedName(AUTHORITY)
    private String mAuthority;

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
    @SerializedName(Credential.SerializedNames.EXPIRES_ON)
    private String mExpiresOn;

    /**
     * Token recommended refresh time. This value should be calculated based on the current UTC time
     * measured locally and the value refresh_in returned from the service. Measured in milliseconds from
     * epoch (1970). Note that this value will not always be present, and is only a recommendation to
     * kick off an async token refresh. The token is still valid until the expires_on value.
     */
    @SerializedName(REFRESH_ON)
    private String mRefreshOn;

    /**
     * The packagename/signature tuple of the application to which the access token was issued
     * This is required for True MAM scenarios to ensure that applications that might share a client id
     * can be correctly differentiated from one another relative to True MAM Policy status
     */
    @SerializedName(APPLICATION_IDENTIFIER)
    private String mApplicationIdentifier;

    /**
     * Gets the kid.
     * <p>
     * Refer to {@link IDevicePopManager#getAsymmetricKeyThumbprint()}.
     *
     * @return The kid to get.
     */
    @Nullable
    public String getKid() {
        return mKid;
    }

    /**
     * Sets the kid.
     * <p>
     * Refer to {@link IDevicePopManager#getAsymmetricKeyThumbprint()}.
     *
     * @param kid The kid to set.
     */
    public void setKid(@Nullable final String kid) {
        mKid = kid;
    }

    /**
     * Gets the requested_claims string.
     *
     * @return The requested_claims string.
     */
    public String getRequestedClaims() {
        return mRequestedClaims;
    }

    /**
     * Sets the requested_claims string
     *
     * @param requestedClaims The claims string to set.
     */
    public void setRequestedClaims(final String requestedClaims) {
        mRequestedClaims = requestedClaims;
    }

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

    /**
     * Gets the refresh_on timestamp.
     *
     * @return The refresh_on to get.
     */
    public String getRefreshOn() {
        return mRefreshOn;
    }

    /**
     * Convenience method for determining if refreshIn is returned from server.
     *
     * @return RefreshOn is active.
     */
    public boolean refreshOnIsActive() {
        return !getExpiresOn().equals(getRefreshOn());
    }

    /**
     * Sets the refresh_on timestamp.
     *
     * @param refreshOn The refresh_on to set.
     */
    public void setRefreshOn(final String refreshOn) {
        mRefreshOn = refreshOn;
    }

    /**
     * Gets the application identifier of the application to which the token was issued.
     * @return String applicationIdentifier
     */
    public String getApplicationIdentifier() { return mApplicationIdentifier; }

    /**
     * Sets the application identifier of the application to which the token was issued.
     * @param applicationIdentifier
     */
    public void setApplicationIdentifier(final String applicationIdentifier) { mApplicationIdentifier = applicationIdentifier; }

    private boolean isExpired(final String expires) {
        // Init a Calendar for the current time/date
        final Calendar calendar = Calendar.getInstance();
        final Date validity = calendar.getTime();
        // Init a Date for the accessToken's expiry
        long epoch = Long.parseLong(expires);
        final Date expiresOn = new Date(
                TimeUnit.SECONDS.toMillis(epoch)
        );
        return expiresOn.before(validity);
    }

    @Override
    public boolean isExpired() {
        return isExpired(getExpiresOn());
    }

    /**
     * If the AT has a refresh_on timestamp (typically used for LLT's),
     * this function will return true after the server recommended refresh
     * interval has elapsed, prompting the library to kick off a background refresh operation.
     *
     * If the AT does NOT have a refresh_on timestamp, this function will always
     * return false. Fallback to standard token expiration/refresh logic.
     *
     * @return Should the library kick off a background token refresh operation for this token
     * after returning the token to the caller.
     */
    public boolean shouldRefresh() {
        final String refreshOn = getRefreshOn();
        if (refreshOn != null && !refreshOn.isEmpty()) {
            return isExpired(refreshOn);
        } else {
            return isExpired();
        }
    }
}
