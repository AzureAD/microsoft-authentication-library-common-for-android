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

    /**
     * The time when the access token expires. The date is represented as the number of seconds
     * from 1970-01-01T0:0:0Z UTC until the expiration time. This value is used to determine the
     * lifetime of cached tokens.
     *
     * @See <a href="https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-protocols-oauth-code">Authorize access to web applications using OAuth 2.0 and Azure Active Directory</a>
     */
    private Date mExpiresOn;

    /**
     * Optionally extended access_token TTL. In the event of STS outage, this field may be used to
     * extend the valid lifetime of an access_token.
     */
    private Date mExtExpiresOn;

    /**
     * Information to uniquely identify the tenant and the user _within_ that tenant.
     */
    @SerializedName("client_info")
    private String mClientInfo;

    /**
     * The client_id of the application requesting a token.
     */
    private transient String mClientId;

    @SerializedName("ext_expires_in")
    private Long mExtendedExpiresIn;

    /**
     * Information to uniquely identify the family that the client application belongs to.
     */
    private String mFamilyId;

    protected Long getExtendedExpiresIn() {
        return mExtendedExpiresIn;
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
     * Gets the response expires on.
     *
     * @return The expires on to get.
     */
    public Date getExpiresOn() {
        return mExpiresOn;
    }

    /**
     * Sets the response expires on.
     *
     * @param expiresOn The expires on to set.
     */
    public void setExpiresOn(final Date expiresOn) {
        mExpiresOn = expiresOn;
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
                "mExpiresOn=" + mExpiresOn +
                ", mExtExpiresOn=" + mExtExpiresOn +
                ", mClientInfo='" + mClientInfo + '\'' +
                ", mClientId='" + mClientId + '\'' +
                ", mExtendedExpiresIn=" + mExtendedExpiresIn +
                ", mFamilyId='" + mFamilyId + '\'' +
                "} " + super.toString();
    }
    //CHECKSTYLE:ON

}
