// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from cache.djinni

package com.microsoft.identity.common.generated;

/** We have one account per user + authority */
public final class Account {


    /*package*/ final String mUniqueId;

    /*package*/ final String mEnvironment;

    /*package*/ final String mRealm;

    /*package*/ final String mAuthorityAccountId;

    /*package*/ final AuthorityType mAuthorityType;

    /*package*/ final String mUsername;

    /*package*/ final String mFirstName;

    /*package*/ final String mLastName;

    /*package*/ final String mAvatarUrl;

    /*package*/ final String mAdditionalFieldsJson;

    public Account(
            String uniqueId,
            String environment,
            String realm,
            String authorityAccountId,
            AuthorityType authorityType,
            String username,
            String firstName,
            String lastName,
            String avatarUrl,
            String additionalFieldsJson) {
        this.mUniqueId = uniqueId;
        this.mEnvironment = environment;
        this.mRealm = realm;
        this.mAuthorityAccountId = authorityAccountId;
        this.mAuthorityType = authorityType;
        this.mUsername = username;
        this.mFirstName = firstName;
        this.mLastName = lastName;
        this.mAvatarUrl = avatarUrl;
        this.mAdditionalFieldsJson = additionalFieldsJson;
    }

    public String getUniqueId() {
        return mUniqueId;
    }

    public String getEnvironment() {
        return mEnvironment;
    }

    /** Previously known as "tenant_id". Can be "common". */
    public String getRealm() {
        return mRealm;
    }

    /** AKA "provider_account_id" */
    public String getAuthorityAccountId() {
        return mAuthorityAccountId;
    }

    /** AKA "account_type" */
    public AuthorityType getAuthorityType() {
        return mAuthorityType;
    }

    public String getUsername() {
        return mUsername;
    }

    public String getFirstName() {
        return mFirstName;
    }

    public String getLastName() {
        return mLastName;
    }

    public String getAvatarUrl() {
        return mAvatarUrl;
    }

    /** For extensibility. "{"extra_field": "extra_value"}" */
    public String getAdditionalFieldsJson() {
        return mAdditionalFieldsJson;
    }

    @Override
    public String toString() {
        return "Account{" +
                "mUniqueId=" + mUniqueId +
                "," + "mEnvironment=" + mEnvironment +
                "," + "mRealm=" + mRealm +
                "," + "mAuthorityAccountId=" + mAuthorityAccountId +
                "," + "mAuthorityType=" + mAuthorityType +
                "," + "mUsername=" + mUsername +
                "," + "mFirstName=" + mFirstName +
                "," + "mLastName=" + mLastName +
                "," + "mAvatarUrl=" + mAvatarUrl +
                "," + "mAdditionalFieldsJson=" + mAdditionalFieldsJson +
        "}";
    }

}
