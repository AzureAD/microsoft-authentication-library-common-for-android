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

/**
 * This is a generic credential schema that should be used as a reference to define schemas for
 * specific credential types.
 */
public abstract class Credential {

    /**
     * The client id of application, as defined in the app developer portal.
     */
    @SerializedName("client_id")
    private String mClientId;

    /**
     * A designated {@link CredentialType} represnted as a String.
     */
    @SerializedName("credential_type")
    private String mCredentialType;

    /**
     * Entity who issued the token represented as a full host of it. For AAD it's host part from
     * the authority url with an optional port. For ADFS, it's the host part of the ADFS server URL.
     */
    @SerializedName("environment")
    private String mEnvironment;

    /**
     * The credential as a String.
     */
    @SerializedName("secret")
    private String mSecret;

    /**
     * Unique user identifier for that authentication scheme.  
     * <p>
     * For AAD/MSA: <code><uid>.<utid></code> <br />
     * STS returns the clientInfo on both v1 and v2 for AAD. This value is combined from two fields
     * in the client Info. It should be a unique user identifier across tenants (e.g. the value will
     * be the same for user’s home tenant and a guest tenant). 
     * </p>
     * <p>
     * For NTLM: Canonicalized username
     * </p>
     * This field is optional if there's no user present for the flow (e.g. client credential
     * grants)
     */
    @SerializedName("unique_id")
    private String mUniqueId;

    /**
     * Absolute device time when entry was created in cache in milliseconds from epoch (1970).
     */
    @SerializedName("cached_at")
    private String mCachedAt;

    /**
     * Token expiry time. This value should be calculated based on the current UTC time measured
     * locally and the value expires_in returned from the service. Measured in milliseconds from
     * epoch (1970).
     */
    @SerializedName("expires_on")
    private String mExpiresOn;

    /**
     * Gets the unique_id.
     *
     * @return The unique_id to get.
     */
    public String getUniqueId() {
        return mUniqueId;
    }

    /**
     * Sets the unique_id.
     *
     * @param uniqueId The unique_id to set.
     */
    public void setUniqueId(final String uniqueId) {
        mUniqueId = uniqueId;
    }

    /**
     * Gets the environment.
     *
     * @return The environment to get.
     */
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

    /**
     * Gets the credential_type.
     *
     * @return The credential_type to get.
     */
    public String getCredentialType() {
        return mCredentialType;
    }

    /**
     * Sets the credential_type.
     *
     * @param credentialType The credential_type to set.
     */
    public void setCredentialType(final String credentialType) {
        mCredentialType = credentialType;
    }

    /**
     * Gets the client_id.
     *
     * @return The client_id to get.
     */
    public String getClientId() {
        return mClientId;
    }

    /**
     * Sets the client_id.
     *
     * @param clientId The client_id to set.
     */
    public void setClientId(final String clientId) {
        mClientId = clientId;
    }

    /**
     * Gets the secret.
     *
     * @return The secret to get.
     */
    public String getSecret() {
        return mSecret;
    }

    /**
     * Sets the secret.
     *
     * @param secret The secret to set.
     */
    public void setSecret(final String secret) {
        mSecret = secret;
    }

    /**
     * Gets the cached_at.
     *
     * @return The cached_at to get.
     */
    public String getCachedAt() {
        return mCachedAt;
    }

    /**
     * Sets the cached_at.
     *
     * @param cachedAt The cached_at to set.
     */
    public void setCachedAt(final String cachedAt) {
        mCachedAt = cachedAt;
    }

    /**
     * Gets the expires_on.
     *
     * @return The expires_on to get.
     */
    public String getExpiresOn() {
        return mExpiresOn;
    }

    /**
     * Sets the expires_on.
     *
     * @param expiresOn The expires_on to set.
     */
    public void setExpiresOn(final String expiresOn) {
        mExpiresOn = expiresOn;
    }
}
