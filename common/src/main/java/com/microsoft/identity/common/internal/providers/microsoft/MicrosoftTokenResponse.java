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
package com.microsoft.identity.common.internal.providers.microsoft;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

import java.util.Date;

public class MicrosoftTokenResponse extends TokenResponse {

    private static final String CLIENT_INFO = "client_info";

    private static final String EXT_EXPIRES_IN = "ext_expires_in";

    private static final String FAMILY_ID = "foci";

    /**
     * Optionally extended access_token TTL. In the event of STS outage, this field may be used to
     * extend the valid lifetime of an access_token.
     */
    private Date mExtExpiresOn;

    /**
     * Information to uniquely identify the tenant and the user _within_ that tenant.
     */
    @SerializedName(CLIENT_INFO)
    private String mClientInfo;

    /**
     * The client_id of the application requesting a token.
     */
    private transient String mClientId;

    @SerializedName(EXT_EXPIRES_IN)
    private Long mExtendedExpiresIn;

    /**
     * Information to uniquely identify the family that the client application belongs to.
     */
    @SerializedName(FAMILY_ID)
    private String mFamilyId;

    private String mAuthority;

    // The token returned is cached with this authority as key.
    // We expect the subsequent requests to AcquireToken will use this authority as the authority parameter,
    // otherwise the AcquireTokenSilent will fail
    public final String getAuthority() {
        return mAuthority;
    }

    public void setAuthority(final String authority) {
        mAuthority = authority;
    }

    /**
     * The deployment ring of the current request chain.
     */
    private String mSpeRing;

    /**
     * The age of the RT, according to the server.
     */
    private String mRefreshTokenAge;

    /**
     * The error code surfaced by the server.
     */
    private String mServerErrorCode;

    /**
     * The server suberror code.
     */
    private String mServerSubErrorCode;

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
     * Gets the server error code.
     *
     * @return The server error code to get.
     */
    public String getServerErrorCode() {
        return mServerErrorCode;
    }

    /**
     * Sets the server error code.
     *
     * @param serverErrorCode The server error code to set.
     */
    public void setServerErrorCode(String serverErrorCode) {
        this.mServerErrorCode = serverErrorCode;
    }

    /**
     * Gets the server suberror code.
     *
     * @return The server suberror code to get.
     */
    public String getServerSubErrorCode() {
        return mServerSubErrorCode;
    }

    /**
     * Sets the server suberror code.
     *
     * @param serverSubErrorCode The server suberror code to get.
     */
    public void setServerSubErrorCode(String serverSubErrorCode) {
        this.mServerSubErrorCode = serverSubErrorCode;
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
        return mExtExpiresOn;
    }

    /**
     * Sets the response ext expires on.
     *
     * @param extExpiresOn The expires on to set.
     */
    public void setExtExpiresOn(final Date extExpiresOn) {
        mExtExpiresOn = extExpiresOn;
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
