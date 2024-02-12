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
package com.microsoft.identity.common.java.providers.microsoft;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.platform.Device;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.DefaultStateGenerator;
import com.microsoft.identity.common.java.providers.oauth2.PkceChallenge;
import com.microsoft.identity.common.java.ui.PreferredAuthMethod;
import com.microsoft.identity.common.java.util.StringUtil;

import java.net.URL;
import java.util.UUID;

import javax.annotation.Nullable;

import cz.msebera.android.httpclient.extras.Base64;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

public abstract class MicrosoftAuthorizationRequest<T extends MicrosoftAuthorizationRequest<T>> extends AuthorizationRequest<T> {
    /**
     * Serial version id.
     */
    private static final long serialVersionUID = 6873634931996113294L;

    private static final String TAG = MicrosoftAuthorizationRequest.class.getSimpleName();
    /**
     * String for the instance aware extra query parameter.
     */
    public static final String INSTANCE_AWARE = "instance_aware";

    /**
     * Required.
     */
    @Getter
    @Accessors(prefix = "m")
    private final transient URL mAuthority; //Not going to be serialized into request url parameters

    /**
     * Can be used to pre-fill the username/email address field of the sign-in page for the user, if you know their username ahead of time.
     */
    @Getter
    @Accessors(prefix = "m")
    @SerializedName("login_hint")
    private final String mLoginHint;

    /**
     * Correlation ID.
     */
    @Expose()
    @Getter
    @Accessors(prefix = "m")
    @SerializedName("client-request-id")
    private final UUID mCorrelationId;

    @Getter
    @Accessors(prefix = "m")
    @SerializedName("code_challenge")
    private final String mPkceCodeChallenge;

    @Getter
    @Accessors(prefix = "m")
    @SerializedName("code_challenge_method")
    private final String mPkceCodeChallengeMethod;

    @Getter
    @Accessors(prefix = "m")
    private transient final String mPkceCodeVerifier;

    @Getter
    @Accessors(prefix = "m")
    private final String mDc;

    /**
     * The version of the calling library.
     */
    @Expose()
    @Getter
    @Accessors(prefix = "m")
    @SerializedName("x-client-Ver")
    private final String mLibraryVersion;

    @Expose()
    @Getter
    @Accessors(prefix = "m")
    @SerializedName("x-client-SKU")
    private final String mLibraryName;

    @Expose()
    @Getter
    @Accessors(prefix = "m")
    @SerializedName("x-client-OS")
    private final String mDiagnosticOS;

    @Expose()
    @Getter
    @Accessors(prefix = "m")
    @SerializedName("x-client-CPU")
    private final String mDiagnosticCPU;

    @Expose()
    @Getter
    @Accessors(prefix = "m")
    @SerializedName("x-client-DM")
    private final String mDiagnosticDM;

    @Expose()
    @Getter
    @Accessors(prefix = "m")
    @SerializedName(INSTANCE_AWARE)
    private final Boolean mMultipleCloudAware;

    @Expose()
    @Getter
    @Accessors(prefix = "m")
    @SerializedName("pc")
    private final String mPreferredAuthMethodCode;


    /**
     * Constructor of MicrosoftAuthorizationRequest.
     */
    protected MicrosoftAuthorizationRequest(@SuppressWarnings(WarningType.rawtype_warning) final Builder builder) {
        super(builder);
        mAuthority = builder.mAuthority;
        mLoginHint = builder.mLoginHint;
        mCorrelationId = builder.mCorrelationId;

        final PkceChallenge challenge = builder.mPkceChallenge == null ?
                PkceChallenge.newPkceChallenge() :
                builder.mPkceChallenge;
        mPkceCodeChallengeMethod = challenge.getCodeChallengeMethod();
        mPkceCodeChallenge = challenge.getCodeChallenge();
        mPkceCodeVerifier = challenge.getCodeVerifier();

        mDc = builder.mDc;

        mMultipleCloudAware = builder.mMultipleCloudAware;
        mLibraryVersion = builder.mLibraryVersion;
        mLibraryName = builder.mLibraryName;
        mPreferredAuthMethodCode = builder.mPreferredAuthMethod == null ?
                null :
                String.valueOf(builder.mPreferredAuthMethod.code);

        mDiagnosticOS = Device.getOsForEsts();
        mDiagnosticDM = Device.getModel();
        mDiagnosticCPU = Device.getCpu();
    }

    public abstract static class Builder<B extends MicrosoftAuthorizationRequest.Builder<B>> extends AuthorizationRequest.Builder<B> {
        /**
         * Required.
         */
        private URL mAuthority;
        /**
         * The version of the calling library.
         */
        private String mLibraryVersion;
        /**
         * The name of the calling library.
         */
        private String mLibraryName;
        private Boolean mMultipleCloudAware;
        private UUID mCorrelationId;
        private String mLoginHint;
        private PkceChallenge mPkceChallenge;
        private String mDc;
        private PreferredAuthMethod mPreferredAuthMethod;

        public Builder() {
            setState(new DefaultStateGenerator().generate());
        }

        public B setAuthority(URL authority) {
            mAuthority = authority;
            return self();
        }

        public B setLibraryVersion(String libraryVersion) {
            mLibraryVersion = libraryVersion;
            return self();
        }

        public B setLibraryName(String libraryName) {
            mLibraryName = libraryName;
            return self();
        }

        public B setMultipleCloudAware(boolean multipleCloudAware) {
            mMultipleCloudAware = multipleCloudAware;
            return self();
        }

        public B setCorrelationId(UUID correlationId) {
            mCorrelationId = correlationId;
            return self();
        }

        public B setLoginHint(String loginHint) {
            mLoginHint = loginHint;
            return self();
        }

        public B setPreferredAuthMethod(@Nullable final PreferredAuthMethod preferredAuthMethod) {
            mPreferredAuthMethod = preferredAuthMethod;
            return self();
        }
        
        /**
         * Used to secure authorization code grants via Proof Key for Code Exchange (PKCE) from a native client.
         */
        public B setPkceChallenge(@NonNull final PkceChallenge pkceChallenge) {
            mPkceChallenge = pkceChallenge;
            return self();
        }

        public B setDc(String dc) {
            mDc = dc;
            return self();
        }

        public abstract B self();
    }
}
