package com.microsoft.identity.common.internal.cache.registry;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.cache.AbstractApplicationMetadata;

public class BrokerApplicationRegistryData extends AbstractApplicationMetadata {

    private static final class SerializedNames extends AbstractApplicationMetadata.SerializedNames {
        static final String ALLOW_WPJ_ACCESS = "wpj_account_access_allowed";
    }

    @SerializedName(SerializedNames.ALLOW_WPJ_ACCESS)
    private boolean mWpjAccountAccessAllowed;

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
        if (!super.equals(o)) return false;

        BrokerApplicationRegistryData that = (BrokerApplicationRegistryData) o;

        return mWpjAccountAccessAllowed == that.mWpjAccountAccessAllowed;
    }
    //CHECKSTYLE:ON

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (mWpjAccountAccessAllowed ? 1 : 0);
        return result;
    }
    //CHECKSTYLE:ON
}
