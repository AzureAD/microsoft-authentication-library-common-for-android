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
package com.microsoft.identity.common.adal.internal;

import android.net.Uri;

import com.microsoft.identity.common.adal.internal.util.DateExtensions;
import com.microsoft.identity.common.internal.cache.SchemaUtil;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAccount;
import com.microsoft.identity.common.internal.result.ILocalAuthenticationResult;

import java.util.Date;

public class ADALUserInfo {
    private String mUniqueId;

    private String mDisplayableId;

    private String mGivenName;

    private String mFamilyName;

    private String mIdentityProvider;

    private transient Uri mPasswordChangeUrl;

    private transient Date mPasswordExpiresOn;

    /**
     * Constructor of ADALUserInfo.
     *
     * @param account AzureActiveDirectoryAccount
     */
    public ADALUserInfo(AzureActiveDirectoryAccount account) {
        mUniqueId = account.getUserId();
        mDisplayableId = account.getDisplayableId();
        mGivenName = account.getFirstName();
        mFamilyName = account.getFamilyName();
        mIdentityProvider = account.getIdentityProvider();
        mPasswordChangeUrl = account.getPasswordChangeUrl();
        mPasswordExpiresOn = account.getPasswordExpiresOn();
    }

    public ADALUserInfo (ILocalAuthenticationResult localAuthenticationResult){
        mUniqueId = localAuthenticationResult.getUniqueId();
        mDisplayableId = localAuthenticationResult.getAccountRecord().getUsername();
        mGivenName = localAuthenticationResult.getAccountRecord().getFirstName();
        mFamilyName = localAuthenticationResult.getAccountRecord().getFamilyName();
        mIdentityProvider = SchemaUtil.getIdentityProvider(localAuthenticationResult.getIdToken());
    }

    /**
     * Constructor for {@link ADALUserInfo}.
     *
     * @param userid           Unique user id for the userInfo.
     * @param givenName        Given name for the userInfo.
     * @param familyName       Family name for the userInfo.
     * @param identityProvider IdentityProvider for the userInfo.
     * @param displayableId    Displayable for the userInfo.
     */
    public ADALUserInfo(String userid, String givenName, String familyName, String identityProvider,
                    String displayableId) {
        mUniqueId = userid;
        mGivenName = givenName;
        mFamilyName = familyName;
        mIdentityProvider = identityProvider;
        mDisplayableId = displayableId;
    }

    /**
     * Gets unique user id.
     *
     * @return the unique id representing an user
     */
    public String getUserId() {
        return mUniqueId;
    }

    /**
     * @param userid The unique user id to set.
     */
    void setUserId(String userid) {
        mUniqueId = userid;
    }

    /**
     * Gets given name.
     *
     * @return the given name of the user
     */
    public String getGivenName() {
        return mGivenName;
    }

    /**
     * Gets family name.
     *
     * @return the family name of the user
     */
    public String getFamilyName() {
        return mFamilyName;
    }

    /**
     * Gets Identity provider.
     *
     * @return the identity provider
     */
    public String getIdentityProvider() {
        return mIdentityProvider;
    }

    /**
     * Gets displayable user name.
     *
     * @return the displayable user name
     */
    public String getDisplayableId() {
        return mDisplayableId;
    }

    /**
     * @param displayableId The displayable Id to set.
     */
    void setDisplayableId(String displayableId) {
        mDisplayableId = displayableId;
    }

    /**
     * Gets password change url.
     *
     * @return the password change uri
     */
    public Uri getPasswordChangeUrl() {
        return mPasswordChangeUrl;
    }

    /**
     * Gets password expires on.
     *
     * @return the time when the password will expire
     */
    public Date getPasswordExpiresOn() {
        return DateExtensions.createCopy(mPasswordExpiresOn);
    }

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @Override
    public String toString() {
        return "ADALUserInfo{" +
                "mUniqueId='" + mUniqueId + '\'' +
                ", mDisplayableId='" + mDisplayableId + '\'' +
                ", mGivenName='" + mGivenName + '\'' +
                ", mFamilyName='" + mFamilyName + '\'' +
                ", mIdentityProvider='" + mIdentityProvider + '\'' +
                ", mPasswordChangeUrl=" + mPasswordChangeUrl +
                ", mPasswordExpiresOn=" + mPasswordExpiresOn +
                '}';
    }
    //CHECKSTYLE:ON
}
