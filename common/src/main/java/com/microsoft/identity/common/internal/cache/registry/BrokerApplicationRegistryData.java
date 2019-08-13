package com.microsoft.identity.common.internal.cache.registry;

import com.google.gson.annotations.SerializedName;

public class BrokerApplicationRegistryData {

    private static final class SerializedNames {
        public static final String CLIENT_ID = "client_id";
        static final String ENVIRONMENT = "environment";
        static final String APPLICATION_UID = "application_uid";
        static final String ALLOW_WPJ_ACCESS = "wpj_account_access_allowed";
    }

    @SerializedName(SerializedNames.CLIENT_ID)
    private String mClientId;

    @SerializedName(SerializedNames.ENVIRONMENT)
    private String mEnvironment;

    @SerializedName(SerializedNames.APPLICATION_UID)
    private int mUid;

    @SerializedName(SerializedNames.ALLOW_WPJ_ACCESS)
    private boolean mWpjAccountAccessAllowed;

    public String getClientId() {
        return mClientId;
    }

    public void setClientId(final String mClientId) {
        this.mClientId = mClientId;
    }

    public String getEnvironment() {
        return mEnvironment;
    }

    public void setEnvironment(final String mEnvironment) {
        this.mEnvironment = mEnvironment;
    }

    public int getUid() {
        return mUid;
    }

    public void setUid(final int mUid) {
        this.mUid = mUid;
    }

    public boolean isWpjAccountAccessAllowed() {
        return mWpjAccountAccessAllowed;
    }

    public void setWpjAccountAccessAllowed(final boolean allow) {
        mWpjAccountAccessAllowed = allow;
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

        BrokerApplicationRegistryData that = (BrokerApplicationRegistryData) o;

        if (mUid != that.mUid) return false;
        if (mWpjAccountAccessAllowed != that.mWpjAccountAccessAllowed) return false;
        if (!mClientId.equals(that.mClientId)) return false;
        return mEnvironment.equals(that.mEnvironment);
    }
    //CHECKSTYLE:ON

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public int hashCode() {
        int result = mClientId.hashCode();
        result = 31 * result + mEnvironment.hashCode();
        result = 31 * result + mUid;
        result = 31 * result + (mWpjAccountAccessAllowed ? 1 : 0);
        return result;
    }
    //CHECKSTYLE:ON
}
