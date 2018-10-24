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

import static com.microsoft.identity.common.internal.dto.AccountRecord.SerializedNames.ALTERNATIVE_ACCOUNT_ID;
import static com.microsoft.identity.common.internal.dto.AccountRecord.SerializedNames.AUTHORITY_TYPE;
import static com.microsoft.identity.common.internal.dto.AccountRecord.SerializedNames.AVATAR_URL;
import static com.microsoft.identity.common.internal.dto.AccountRecord.SerializedNames.CLIENT_INFO;
import static com.microsoft.identity.common.internal.dto.AccountRecord.SerializedNames.ENVIRONMENT;
import static com.microsoft.identity.common.internal.dto.AccountRecord.SerializedNames.FAMILY_NAME;
import static com.microsoft.identity.common.internal.dto.AccountRecord.SerializedNames.FIRST_NAME;
import static com.microsoft.identity.common.internal.dto.AccountRecord.SerializedNames.HOME_ACCOUNT_ID;
import static com.microsoft.identity.common.internal.dto.AccountRecord.SerializedNames.LOCAL_ACCOUNT_ID;
import static com.microsoft.identity.common.internal.dto.AccountRecord.SerializedNames.MIDDLE_NAME;
import static com.microsoft.identity.common.internal.dto.AccountRecord.SerializedNames.NAME;
import static com.microsoft.identity.common.internal.dto.AccountRecord.SerializedNames.REALM;
import static com.microsoft.identity.common.internal.dto.AccountRecord.SerializedNames.USERNAME;

/**
 * Accounts collect user displayable information about the user in each tenant (AAD) or environment
 * (MSA). Accounts also have fields necessary to lookup credentials.
 * <p>
 * Account schema only needs to be present, if there's a user available. For scenarios, where
 * there's no user present (e.g. client credential grant), only credential schema is necessary.
 */
public class AccountRecord extends AccountCredentialBase implements IAccountRecord {

    /**
     * String name list for Account object serialization.
     */
    public static class SerializedNames {
        /**
         * JSON name of home account id.
         */
        public static final String HOME_ACCOUNT_ID = "home_account_id";

        /**
         * JSON name of the environment.
         */
        public static final String ENVIRONMENT = "environment";

        /**
         * JSON name of the realm.
         */
        public static final String REALM = "realm";

        /**
         * JSON name of the local account id.
         */
        public static final String LOCAL_ACCOUNT_ID = "local_account_id";

        /**
         * JSON name of the username.
         */
        public static final String USERNAME = "username";

        /**
         * JSON name of the authority type.
         */
        public static final String AUTHORITY_TYPE = "authority_type";

        /**
         * JSON name of the alternative account id.
         */
        public static final String ALTERNATIVE_ACCOUNT_ID = "alternative_account_id";

        /**
         * JSON name of the first name.
         */
        public static final String FIRST_NAME = "first_name";

        /**
         * JSON name of the family name.
         */
        public static final String FAMILY_NAME = "family_name";

        /**
         * JSON name of the middle_name.
         */
        public static final String MIDDLE_NAME = "middle_name";

        /**
         * JSON name of the name.
         */
        public static final String NAME = "name";

        /**
         * JSON name of the avatar url.
         */
        public static final String AVATAR_URL = "avatar_url";
        /**
         * String of client info.
         */
        public static final String CLIENT_INFO = "client_info";
    }

    /**
     * Empty constructor for Account.
     */
    public AccountRecord() {
        // Empty
    }

    /**
     * Constructor for Account.
     *
     * @param copy IAccount
     */
    public AccountRecord(final IAccountRecord copy) {
        // Required
        setHomeAccountId(copy.getHomeAccountId());
        setEnvironment(copy.getEnvironment());
        setRealm(copy.getRealm());
        setLocalAccountId(copy.getLocalAccountId());
        setUsername(copy.getUsername());
        setAuthorityType(copy.getAuthorityType());

        // Optional
        setClientInfo(copy.getClientInfo());
        setAlternativeAccountId(copy.getAlternativeAccountId());
        setFirstName(copy.getFirstName());
        setFamilyName(copy.getFamilyName());
        setMiddleName(copy.getMiddleName());
        setName(copy.getName());
        setAvatarUrl(copy.getAvatarUrl());
    }

    /**
     * Unique user identifier for a given authentication scheme.
     */
    @SerializedName(HOME_ACCOUNT_ID)
    private String mHomeAccountId;

    /**
     * Entity who issued the token represented as the host portion of a URL. For AAD it's host part
     * from the authority url with an optional port. For ADFS, it's the host part of the ADFS server
     * URL.
     */
    @SerializedName(ENVIRONMENT)
    private String mEnvironment;

    /**
     * Full tenant or organizational identifier that the Account belongs to. Can be null.
     */
    @SerializedName(REALM)
    private String mRealm;

    /**
     * Original authority specific account identifier. Can be needed for legacy purposes. OID for
     * AAD (in some unique cases subject instead of OID) and CID for MSA.
     */
    @SerializedName(LOCAL_ACCOUNT_ID)
    private String mLocalAccountId;

    /**
     * The primary username that represents the user (corresponds to the preferred_username claim
     * in the v2.0 endpoint). It could be an email address, phone number, or a generic username
     * without a specified format. Its value is mutable and might change over time. For MSA it's
     * email. For NTLM, NTLM username.
     */
    @SerializedName(USERNAME)
    private String mUsername;

    /**
     * Account’s authority type as string (ex: MSA, MSSTS, Other).
     * Set of account types is extensible.
     */
    @SerializedName(AUTHORITY_TYPE)
    private String mAuthorityType;

    /**
     * Internal representation for guest users to the tenants. Corresponds to the "altsecid" claim
     * in the id_token for AAD.
     */
    @SerializedName(ALTERNATIVE_ACCOUNT_ID)
    private String mAlternativeAccountId;

    /**
     * First name for this Account.
     */
    @SerializedName(FIRST_NAME)
    private String mFirstName;

    /**
     * Family name for this Account.
     */
    @SerializedName(FAMILY_NAME)
    private String mFamilyName;

    /**
     * Middle name for this Account.
     */
    @SerializedName(MIDDLE_NAME)
    private String mMiddleName;

    /**
     * Name for this Account.
     */
    @SerializedName(NAME)
    private String mName;

    /**
     * URL corresponding to a picture for this Account.
     */
    @SerializedName(AVATAR_URL)
    private String mAvatarUrl;


    /**
     * Full base64 encoded client info received from ESTS, if available. STS returns the clientInfo 
     * on both v1 and v2 for AAD. This field is used for extensibility purposes.
     */
    @SerializedName(CLIENT_INFO)
    private String mClientInfo;

    @Override
    public String getHomeAccountId() {
        return mHomeAccountId;
    }

    /**
     * Sets the home_account_id.
     *
     * @param homeAccountId The home_account_id to get.
     */
    public void setHomeAccountId(final String homeAccountId) {
        mHomeAccountId = homeAccountId;
    }

    @Override
    public String getEnvironment() {
        return mEnvironment;
    }

    /**
     * Sets the environment.
     *
     * @param environment The environment to set.
     */
    public void setEnvironment(final String environment) {
        mEnvironment = environment;
    }

    @Override
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

    @Override
    public String getLocalAccountId() {
        return mLocalAccountId;
    }

    /**
     * Sets the local_account_id.
     *
     * @param localAccountId The local_account_id to set.
     */
    public void setLocalAccountId(final String localAccountId) {
        mLocalAccountId = localAccountId;
    }

    @Override
    public String getUsername() {
        return mUsername;
    }

    /**
     * Sets the username.
     *
     * @param username The username to set.
     */
    public void setUsername(final String username) {
        mUsername = username;
    }

    @Override
    public String getAuthorityType() {
        return mAuthorityType;
    }

    /**
     * Sets the authority_type.
     *
     * @param authorityType The authority_type to set.
     */
    public void setAuthorityType(final String authorityType) {
        mAuthorityType = authorityType;
    }

    @Override
    public String getAlternativeAccountId() {
        return mAlternativeAccountId;
    }

    /**
     * Sets the alternative_account_id.
     *
     * @param alternativeAccountId The alternative_account_id to set.
     */
    public void setAlternativeAccountId(final String alternativeAccountId) {
        mAlternativeAccountId = alternativeAccountId;
    }

    @Override
    public String getFirstName() {
        return mFirstName;
    }

    /**
     * Sets the first_name.
     *
     * @param firstName The first_name to set.
     */
    public void setFirstName(final String firstName) {
        mFirstName = firstName;
    }

    @Override
    public String getFamilyName() {
        return mFamilyName;
    }

    /**
     * Sets the middle_name.
     *
     * @param middleName The middle_name to set.
     */
    public void setMiddleName(final String middleName) {
        mMiddleName = middleName;
    }

    @Override
    public String getMiddleName() {
        return mMiddleName;
    }

    /**
     * Sets the name.
     *
     * @param name The name to set.
     */
    public void setName(final String name) {
        mName = name;
    }

    @Override
    public String getName() {
        return mName;
    }

    /**
     * Sets the family_name.
     *
     * @param familyName The family_name to set.
     */
    public void setFamilyName(final String familyName) {
        mFamilyName = familyName;
    }

    @Override
    public String getAvatarUrl() {
        return mAvatarUrl;
    }

    /**
     * Sets the avatar_url.
     *
     * @param avatarUrl The avatar_url to set.
     */
    public void setAvatarUrl(final String avatarUrl) {
        mAvatarUrl = avatarUrl;
    }


    /**
     * Gets the client_info.
     *
     * @return The client_info to get.
     */
    @Override
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

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccountRecord account = (AccountRecord) o;

        if (mHomeAccountId != null ? !mHomeAccountId.equals(account.mHomeAccountId) : account.mHomeAccountId != null)
            return false;
        if (mEnvironment != null ? !mEnvironment.equals(account.mEnvironment) : account.mEnvironment != null)
            return false;
        if (mRealm != null ? !mRealm.equals(account.mRealm) : account.mRealm != null) return false;
        if (mLocalAccountId != null ? !mLocalAccountId.equals(account.mLocalAccountId) : account.mLocalAccountId != null)
            return false;
        if (mUsername != null ? !mUsername.equals(account.mUsername) : account.mUsername != null)
            return false;
        if (mAuthorityType != null ? !mAuthorityType.equals(account.mAuthorityType) : account.mAuthorityType != null)
            return false;
        if (mAlternativeAccountId != null ? !mAlternativeAccountId.equals(account.mAlternativeAccountId) : account.mAlternativeAccountId != null)
            return false;
        if (mFirstName != null ? !mFirstName.equals(account.mFirstName) : account.mFirstName != null)
            return false;
        if (mFamilyName != null ? !mFamilyName.equals(account.mFamilyName) : account.mFamilyName != null)
            return false;
        return mAvatarUrl != null ? mAvatarUrl.equals(account.mAvatarUrl) : account.mAvatarUrl == null;
    }
    //CHECKSTYLE:ON

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @Override
    public int hashCode() {
        int result = mHomeAccountId != null ? mHomeAccountId.hashCode() : 0;
        result = 31 * result + (mEnvironment != null ? mEnvironment.hashCode() : 0);
        result = 31 * result + (mRealm != null ? mRealm.hashCode() : 0);
        result = 31 * result + (mLocalAccountId != null ? mLocalAccountId.hashCode() : 0);
        result = 31 * result + (mUsername != null ? mUsername.hashCode() : 0);
        result = 31 * result + (mAuthorityType != null ? mAuthorityType.hashCode() : 0);
        result = 31 * result + (mAlternativeAccountId != null ? mAlternativeAccountId.hashCode() : 0);
        result = 31 * result + (mFirstName != null ? mFirstName.hashCode() : 0);
        result = 31 * result + (mFamilyName != null ? mFamilyName.hashCode() : 0);
        result = 31 * result + (mAvatarUrl != null ? mAvatarUrl.hashCode() : 0);
        return result;
    }
    //CHECKSTYLE:ON

}
