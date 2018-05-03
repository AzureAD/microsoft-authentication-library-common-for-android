package com.microsoft.identity.common.internal.dto;

import com.google.gson.annotations.SerializedName;

import static com.microsoft.identity.common.internal.dto.IdToken.SerializedNames.AUTHORITY;
import static com.microsoft.identity.common.internal.dto.IdToken.SerializedNames.REALM;

public class IdToken extends Credential {

    public static class SerializedNames extends Credential.SerializedNames {
        public static final String REALM = "realm";
        public static final String AUTHORITY = "authority";
    }

    /**
     * Full tenant or organizational identifier that account belongs to. Can be null.
     */
    @SerializedName(REALM)
    private String mRealm;

    /**
     * Full authority URL of the issuer.
     */
    @SerializedName(AUTHORITY)
    private String mAuthority;

    /**
     * Gets the authority.
     *
     * @return The authority to get.
     */
    public String getAuthority() {
        return mAuthority;
    }

    /**
     * Sets the authority.
     *
     * @param authority The authority to set.
     */
    public void setAuthority(final String authority) {
        mAuthority = authority;
    }

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

        if (mRealm != null ? !mRealm.equals(idToken.mRealm) : idToken.mRealm != null) return false;
        return mAuthority != null ? mAuthority.equals(idToken.mAuthority) : idToken.mAuthority == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (mRealm != null ? mRealm.hashCode() : 0);
        result = 31 * result + (mAuthority != null ? mAuthority.hashCode() : 0);
        return result;
    }

}
