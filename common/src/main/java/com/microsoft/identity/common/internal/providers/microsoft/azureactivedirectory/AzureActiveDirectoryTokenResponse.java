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
     * The time after which the issued access_token may be used.
     */
    private String mNotBefore;

    /**
     * The SPE Ring from which this token was issued.
     */
    private String mSpeRing;

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

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @Override
    public String toString() {
        return "AzureActiveDirectoryTokenResponse{" +
                "mExpiresOn=" + mExpiresOn +
                ", mResource='" + mResource + '\'' +
                ", mNotBefore='" + mNotBefore + '\'' +
                ", mSpeRing='" + mSpeRing + '\'' +
                "} " + super.toString();
    }
    //CHECKSTYLE:ON
}
