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
package com.microsoft.identity.common.java.providers.microsoft;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.java.util.CopyUtil;

import java.util.Date;

public class MicrosoftTokenResponse extends TokenResponse {

    private static final String SESSION_KEY_JWE = "session_key_jwe";
    private static final String CLIENT_INFO = "client_info";

    private static final String EXT_EXPIRES_IN = "ext_expires_in";

    private static final String FAMILY_ID = "foci";
    private static final String REFRESH_TOKEN_EXPIRES_IN = "refresh_token_expires_in";

    /**
     * Optionally extended access_token TTL. In the event of STS outage, this field may be used to
     * extend the valid lifetime of an access_token.
     */
    private Date mExtExpiresOn;

    /**
     * Get the string representation of the remaining lifetime of the refresh token.
     *
     * @return the string representation of the remaining lifetime of the refresh token, may be null.
     */
    public String getRefreshTokenExpiresIn() {
        return mRefreshTokenExpiresIn;
    }

    /**
     * Set the string representation of the remaining lifetime of the refresh token.
     *
     * @param mRefreshTokenExpiresIn the string representation of the remaining lifetime of the refresh
     *                               token, may be null.
     */
    public void setRefreshTokenExpiresIn(String mRefreshTokenExpiresIn) {
        this.mRefreshTokenExpiresIn = mRefreshTokenExpiresIn;
    }

    /**
     * If this request includes an encrypted session key, return it here.
     */
    @SerializedName(REFRESH_TOKEN_EXPIRES_IN)
    private String mRefreshTokenExpiresIn;

    /**
     * If this request includes an encrypted session key, return it here.
     */
    @SerializedName(SESSION_KEY_JWE)
    private String mSessionKeyJwe;

    /**
     * Get the session key JWE associated with this result, or null if none.
     *
     * @return the session key JWE associated with this result, or null if none.
     */
    public String getSessionKeyJwe() {
        return mSessionKeyJwe;
    }

    /**
     * Set the session key JWE associated with this result, or null if none.
     *
     * @param sessionKeyJwe the session key JWE associated with this result, or null if none.
     * @return
     */
    public void setSessionKeyJwe(final String sessionKeyJwe) {
        mSessionKeyJwe = sessionKeyJwe;
    }

    /**
     * Information to uniquely identify the tenant and the user _within_ that tenant.
     */
    @SerializedName(CLIENT_INFO)
    private String mClientInfo;

    /**
     * The client_id of the application requesting a token.
     */
    private transient String mClientId;

    @Expose()
    @SerializedName(EXT_EXPIRES_IN)
    private Long mExtendedExpiresIn;

    /**
     * Information to uniquely identify the family that the client application belongs to.
     */
    @Expose()
    @SerializedName(FAMILY_ID)
    private String mFamilyId;

    @Expose()
    @SerializedName("cloud_instance_host_name")
    private String mCloudInstanceHostName;

    private String mAuthority;

    // The token returned is cached with this authority as key.
    // We expect the subsequent requests to AcquireToken will use this authority as the authority parameter,
    // otherwise the AcquireTokenSilent will fail
    public String getAuthority() {
        return mAuthority;
    }

    public void setAuthority(final String authority) {
        mAuthority = authority;
    }

    /**
     * The deployment ring of the current request chain.
     */
    @Expose()
    private String mSpeRing;

    /**
     * The age of the RT, according to the server (x-ms-clitelem header).
     */
    @Expose()
    private String mRefreshTokenAge;

    /**
     * The error code code set as part of the client telemetry info header.  This likely will not be populated for a successful token response
     */
    private String mCliTelemErrorCode;

    /**
     * The server sub error code set as part of the client telemetry info header.  This likely will not be populated for a successful token response
     */
    private String mCliTelemSubErrorCode;

    /**
     * Gets the SPE Ring.
     *
     * @return The SPE Ring to get.
     */
    public String getSpeRing() {
        return mSpeRing;
    }

    /**
     * Sets the SPE Ring.
     *
     * @param speRing The SPR Ring to set.
     */
    public void setSpeRing(String speRing) {
        this.mSpeRing = speRing;
    }

    /**
     * Gets the refresh token age.
     *
     * @return The refresh token age to get.
     */
    public String getRefreshTokenAge() {
        return mRefreshTokenAge;
    }

    /**
     * Sets the refresh token age.
     *
     * @param refreshTokenAge The refresh token age to set.
     */
    public void setRefreshTokenAge(String refreshTokenAge) {
        this.mRefreshTokenAge = refreshTokenAge;
    }

    /**
     * Gets the server error code returned by the x-ms-clitelem header.
     *
     * @return The server error code to get.
     */
    public String getCliTelemErrorCode() {
        return mCliTelemErrorCode;
    }

    /**
     * Sets the server error code returned by the x-ms-clitelem header.
     *
     * @param serverErrorCode The server error code to set.
     */
    public void setCliTelemErrorCode(String serverErrorCode) {
        this.mCliTelemErrorCode = serverErrorCode;
    }

    /**
     * Gets the server suberror code returned by the x-ms-clitelem header.
     *
     * @return The server suberror code to get.
     */
    public String getCliTelemSubErrorCode() {
        return mCliTelemSubErrorCode;
    }

    /**
     * Sets the server suberror code returned by the x-ms-clitelem header.
     *
     * @param serverSubErrorCode The server suberror code to get.
     */
    public void setCliTelemSubErrorCode(String serverSubErrorCode) {
        this.mCliTelemSubErrorCode = serverSubErrorCode;
    }

    /**
     * Gets the ext_expires_in.
     *
     * @return The ext_expires_in to get.
     */
    public Long getExtExpiresIn() {
        return mExtendedExpiresIn;
    }

    /**
     * Sets the ext_expires_in.
     *
     * @param extExpiresIn The ext_expires_in to set.
     */
    public void setExtExpiresIn(final Long extExpiresIn) {
        mExtendedExpiresIn = extExpiresIn;
    }

    /**
     * Gets the response client_info.
     *
     * @return The client_info to get.
     */
    public String getClientInfo() {
        return mClientInfo;
    }

    /**
     * Sets the response client_info.
     *
     * @param clientInfo The client_info to set.
     */
    public void setClientInfo(final String clientInfo) {
        mClientInfo = clientInfo;
    }

    /**
     * Gets the response ext expires on.
     *
     * @return The ext expires on to get.
     */
    public Date getExtExpiresOn() {
        return CopyUtil.copyIfNotNull(mExtExpiresOn);
    }

    /**
     * Sets the response ext expires on.
     *
     * @param extExpiresOn The expires on to set.
     */
    public void setExtExpiresOn(final Date extExpiresOn) {
        mExtExpiresOn = CopyUtil.copyIfNotNull(extExpiresOn);
    }

    /**
     * Returns the family client id.
     *
     * @return mFamilyId
     */
    public String getFamilyId() {
        return mFamilyId;
    }

    /**
     * Sets the family id.
     *
     * @param familyId family id of the token.
     */
    public void setFamilyId(final String familyId) {
        mFamilyId = familyId;
    }

    /**
     * Gets the client_id.
     *
     * @return The client_id to get.
     */
    public String getClientId() {
        return mClientId;
    }

    /**
     * Sets the client_id.
     *
     * @param clientId The client_id to set.
     */
    public void setClientId(final String clientId) {
        mClientId = clientId;
    }

    /**
     * Gets cloud instance host name
     *
     * @return mCloudInstanceHostName
     */
    public String getCloudInstanceHostName() {
        return mCloudInstanceHostName;
    }

    /**
     * Sets cloud instance host name
     *
     * @param cloudInstanceHostName
     */
    public void setCloudInstanceHostName(final String cloudInstanceHostName) {
        mCloudInstanceHostName = cloudInstanceHostName;
    }

    //CHECKSTYLE:OFF
    @Override
    public String toString() {
        return "MicrosoftTokenResponse{" +
                "mExtExpiresOn=" + mExtExpiresOn +
                ", mClientInfo='" + mClientInfo + '\'' +
                ", mClientId='" + mClientId + '\'' +
                ", mExtendedExpiresIn=" + mExtendedExpiresIn +
                ", mFamilyId='" + mFamilyId + '\'' +
                "} " + super.toString();
    }
    //CHECKSTYLE:ON

}
