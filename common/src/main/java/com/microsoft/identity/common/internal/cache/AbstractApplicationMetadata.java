package com.microsoft.identity.common.internal.cache;

import com.google.gson.annotations.SerializedName;

public abstract class AbstractApplicationMetadata {

    protected static class SerializedNames {
        public static final String CLIENT_ID = "client_id";
        static final String ENVIRONMENT = "environment";
        static final String APPLICATION_UID = "application_uid";
    }

    @SerializedName(SerializedNames.CLIENT_ID)
    private String mClientId;

    @SerializedName(SerializedNames.ENVIRONMENT)
    private String mEnvironment;

    @SerializedName(SerializedNames.APPLICATION_UID)
    private int mUid;

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

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractApplicationMetadata that = (AbstractApplicationMetadata) o;

        if (mUid != that.mUid) return false;
        if (mClientId != null ? !mClientId.equals(that.mClientId) : that.mClientId != null)
            return false;
        return mEnvironment != null ? mEnvironment.equals(that.mEnvironment) : that.mEnvironment == null;
    }
    //CHECKSTYLE:ON

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public int hashCode() {
        int result = mClientId != null ? mClientId.hashCode() : 0;
        result = 31 * result + (mEnvironment != null ? mEnvironment.hashCode() : 0);
        result = 31 * result + mUid;
        return result;
    }
    //CHECKSTYLE:ON
}
