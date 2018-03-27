package com.microsoft.identity.common.internal.dto;

import com.google.gson.annotations.SerializedName;

public class IdToken extends Credential {

    /**
     * Full tenant or organizational identifier that account belongs to. Can be null.
     */
    @SerializedName("realm")
    private String mRealm;

    /**
     * Gets the realm.
     *
     * @return The realm to get.
     */
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        IdToken idToken = (IdToken) o;

        return mRealm != null ? mRealm.equals(idToken.mRealm) : idToken.mRealm == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (mRealm != null ? mRealm.hashCode() : 0);
        return result;
    }
}
