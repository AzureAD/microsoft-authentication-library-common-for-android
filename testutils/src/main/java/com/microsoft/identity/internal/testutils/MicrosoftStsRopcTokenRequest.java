package com.microsoft.identity.internal.testutils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest;

public class MicrosoftStsRopcTokenRequest extends MicrosoftStsTokenRequest {

    @Expose()
    @SerializedName("username")
    private String mUsername;

    @Expose()
    @SerializedName("password")
    private String mPassword;

    /**
     * Gets the username
     *
     * @return the user's username
     */
    public String getUsername() {
        return mUsername;
    }

    /**
     * Sets the username
     *
     * @param username the user's username
     */
    public void setUsername(final String username) {
        mUsername = username;
    }

    /**
     * Gets the password
     *
     * @return the user's password
     */
    public String getPassword() {
        return mPassword;
    }

    /**
     * Sets the password
     *
     * @param password the user's password
     */
    public void setPassword(final String password) {
        mPassword = password;
    }
}
