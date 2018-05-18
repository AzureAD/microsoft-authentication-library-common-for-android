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
package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenResponse;

import java.util.Date;

/**
 * {@link MicrosoftTokenResponse} subclass for Azure AD.
 */
public class AzureActiveDirectoryTokenResponse extends MicrosoftTokenResponse {

    /**
     * The time when the access token expires. The date is represented as the number of seconds
     * from 1970-01-01T0:0:0Z UTC until the expiration time. This value is used to determine the
     * lifetime of cached tokens.
     *
     * @See <a href="https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-protocols-oauth-code">Authorize access to web applications using OAuth 2.0 and Azure Active Directory</a>
     */
    private Date mExpiresOn;

    /**
     * The App ID URI of the web API (secured resource).
     *
     * @See <a href="https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-protocols-oauth-code">Authorize access to web applications using OAuth 2.0 and Azure Active Directory</a>
     */
    private String mResource;

    /**
     * Optionally extended access_token TTL. In the event of STS outage, this field may be used to
     * extend the valid lifetime of an access_token.
     */
    private Date mExtExpiresOn;

    /**
     * The time after which the issued access_token may be used.
     */
    private String mNotBefore;

    /**
     * Information to uniquely identify the tenant and the user _within_ that tenant.
     */
    private String mClientInfo;

    /**
     * Information to uniquely identify the family that the client application belongs to.
     */
    private String mFamilyId;

    /**
     * The client_id of the application requesting a token.
     */
    private transient String mClientId;

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
     * @param familyId family ID of the token
     */
    public void setFamilyId(final String familyId) {
        mFamilyId = familyId;
    }

    /**
     * The SPE Ring from which this token was issued.
     */
    private String mSpeRing;

    /**
     * Gets the response expires_on.
     *
     * @return The expires_on to get.
     */
    public Date getExpiresOn() {
        return mExpiresOn;
    }

    /**
     * Sets the response expires_on.
     *
     * @param expiresOn The expires_on to set.
     */
    public void setExpiresOn(final Date expiresOn) {
        mExpiresOn = expiresOn;
    }

    /**
     * Gets the response resource.
     *
     * @return The resource to get.
     */
    public String getResource() {
        return mResource;
    }

    /**
     * Sets the response resource.
     *
     * @param resource The resource to set.
     */
    public void setResource(final String resource) {
        mResource = resource;
    }

    /**
     * Gets the response ext_expires_in.
     *
     * @return The ext_expires_in to get.
     */
    public Date getExtExpiresOn() {
        return mExtExpiresOn;
    }

    /**
     * Sets the response ext_expires_in.
     *
     * @param extExpiresOn The ext_expires_in to set.
     */
    public void setExtExpiresOn(final Date extExpiresOn) {
        mExtExpiresOn = extExpiresOn;
    }

    /**
     * Gets the response not_before.
     *
     * @return The not_before to get.
     */
    public String getNotBefore() {
        return mNotBefore;
    }

    /**
     * Set the response not_before.
     *
     * @param notBefore The not_before to set.
     */
    public void setNotBefore(final String notBefore) {
        mNotBefore = notBefore;
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
     * Gets the response spe ring (x-ms-clitelem).
     *
     * @return The spe ring.
     */
    public String getSpeRing() {
        return mSpeRing;
    }

    /**
     * Sets the response spe ring (x-ms-clitelem).
     *
     * @param speRing The spe ring to set.
     */
    public void setSpeRing(final String speRing) {
        mSpeRing = speRing;
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
}
