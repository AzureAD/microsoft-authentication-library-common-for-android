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
public class Credential {

    ///////////////
    // Required fields
    ///////////////

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
     * Entity who issued the token represented as a full host of it. For AAD it's host part from
     * the authority url with an optional port. For ADFS, it's the host part of the ADFS server URL.
     */
    @SerializedName("environment")
    private String mEnvironment;

    /**
     * A designated {@link CredentialType} represnted as a String.
     */
    @SerializedName("credential_type")
    private String mCredentialType;

    /**
     * The client id of application, as defined in the app developer portal.
     */
    @SerializedName("client_id")
    private String mClientId;

    /**
     * The credential as a String.
     */
    @SerializedName("secret")
    private String mSecret;

    ///////////////
    // Optional Fields
    ///////////////

    /**
     * Permissions that are included in the token. Formats for endpoints will be different. 
     * <p>
     * Mandatory, if credential is scoped down by some parameters or requirements (e.g. by
     * resource, scopes or permissions).
     */
    @SerializedName("target")
    private String mTarget;

    /**
     * Full tenant or organizational identifier that account belongs to. Can be null.
     */
    @SerializedName("realm")
    private String mRealm;

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
     * Full base64 encoded client info received from ESTS, if available. STS returns the clientInfo 
     * on both v1 and v2 for AAD. This field is used for extensibility purposes.
     */
    @SerializedName("client_info")
    private String mClientInfo;

    ///////////////
    // Accessor Methods
    ///////////////

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
     * Gets the target.
     *
     * @return The target to get.
     */
    public String getTarget() {
        return mTarget;
    }

    /**
     * Sets the target.
     *
     * @param target The target to set.
     */
    public void setTarget(final String target) {
        mTarget = target;
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

    /**
     * Gets the client_info.
     *
     * @return The client_info to get.
     */
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
}
