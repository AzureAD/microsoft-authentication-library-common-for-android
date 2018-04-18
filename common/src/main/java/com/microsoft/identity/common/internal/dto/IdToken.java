package com.microsoft.identity.common.internal.dto;

import com.google.gson.annotations.SerializedName;

import static com.microsoft.identity.common.internal.dto.IdToken.SerializedNames.REALM;

public class IdToken extends Credential {

    public static class SerializedNames extends Credential.SerializedNames {
        public static final String REALM = "realm";
    }

    /**
     * Full tenant or organizational identifier that account belongs to. Can be null.
     */
    @SerializedName(REALM)
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
