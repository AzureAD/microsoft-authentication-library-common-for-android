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

import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdToken;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;

import java.io.Serializable;
import java.util.Map;


/**
 * Contains information of a single user.
 */
public class UserInfo implements Serializable {

    /**
     * Universal version identifier for UserInfo class.
     */
    private static final long serialVersionUID = 8790127561636702672L;

    private String mUniqueId;

    private String mDisplayableId;

    private String mGivenName;

    private String mFamilyName;

    private String mIdentityProvider;

    private transient Uri mPasswordChangeUrl;

  //  private transient Date mPasswordExpiresOn;

    /**
     * Default constructor for {@link UserInfo}.
     */
    public UserInfo() {
        // Default constructor, intentionally empty.
    }

    /**
     * Constructor for {@link UserInfo}.
     *
     * @param upn Upn that is used to construct the {@link UserInfo}.
     */
    public UserInfo(String upn) {
        mDisplayableId = upn;
    }

    /**
     * Constructor for {@link UserInfo}.
     *
     * @param userid           Unique user id for the userInfo.
     * @param givenName        Given name for the userInfo.
     * @param familyName       Family name for the userInfo.
     * @param identityProvider IdentityProvider for the userInfo.
     * @param displayableId    Displayable for the userInfo.
     */
    public UserInfo(String userid, String givenName, String familyName, String identityProvider,
                    String displayableId) {
        mUniqueId = userid;
        mGivenName = givenName;
        mFamilyName = familyName;
        mIdentityProvider = identityProvider;
        mDisplayableId = displayableId;
    }

    /**
     * Constructor for creating {@link UserInfo} from {@link IDToken}.
     *
     * @param idToken The {@link IDToken} to create {@link UserInfo}.
     */
    public UserInfo(IDToken idToken) {
        Map<String, String> claims = idToken.getTokenClaims();
        mUniqueId = claims.get(MicrosoftIdToken.OBJECT_ID);
        mGivenName = claims.get(AuthenticationConstants.OAuth2.ID_TOKEN_GIVEN_NAME);
        mFamilyName = claims.get(AuthenticationConstants.OAuth2.ID_TOKEN_FAMILY_NAME);
        mIdentityProvider = claims.get(AuthenticationConstants.OAuth2.ID_TOKEN_IDENTITY_PROVIDER);
        mDisplayableId =  claims.get(IDToken.PREFERRED_USERNAME);
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

    void setGivenName(String name) {
        mGivenName = name;
    }

    /**
     * Gets family name.
     *
     * @return the family name of the user
     */
    public String getFamilyName() {
        return mFamilyName;
    }

    void setFamilyName(String familyName) {
        mFamilyName = familyName;
    }

    /**
     * Gets Identity provider.
     *
     * @return the identity provider
     */
    public String getIdentityProvider() {
        return mIdentityProvider;
    }

    void setIdentityProvider(String provider) {
        mIdentityProvider = provider;
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

    void setPasswordChangeUrl(Uri passwordChangeUrl) {
        this.mPasswordChangeUrl = passwordChangeUrl;
    }

//    /**
//     * Gets password expires on.
//     *
//     * @return the time when the password will expire
//     */
//    public Date getPasswordExpiresOn() {
//        return DateExtensions.createCopy(mPasswordExpiresOn);
//    }

//    void setPasswordExpiresOn(Date passwordExpiresOn) {
//        if (null == passwordExpiresOn) {
//            mPasswordExpiresOn= null;
//        } else {
//            mPasswordExpiresOn = new Date(passwordExpiresOn.getTime());
//        }
//    }
}
