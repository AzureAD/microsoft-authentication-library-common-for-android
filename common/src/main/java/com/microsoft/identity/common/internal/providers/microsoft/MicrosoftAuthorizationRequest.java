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
package com.microsoft.identity.common.internal.providers.microsoft;

import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.util.Pair;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectorySlice;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.PkceChallenge;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    private transient URL mAuthority; //Not going to be serialized into request url parameters

    /**
     * Can be used to pre-fill the username/email address field of the sign-in page for the user, if you know their username ahead of time.
     */
    @SerializedName("login_hint")
    private String mLoginHint;

    /**
     * Correlation ID.
     */
    @Expose()
    @SerializedName("client-request-id")
    private UUID mCorrelationId;

    /**
     * Used to secure authorization code grants via Proof Key for Code Exchange (PKCE) from a native client.
     */
    private transient PkceChallenge mPkceChallenge;

    @SerializedName("code_challenge")
    private String mCodeChallenge;

    @SerializedName("code_challenge_method")
    private String mCodeChallengeMethod;

    /**
     * The version of the calling library.
     */
    @Expose()
    @SerializedName("x-client-Ver")
    private String mLibraryVersion;

    @Expose()
    @SerializedName("x-client-SKU")
    private String mLibraryName;

    @Expose()
    @SerializedName("x-client-OS")
    private String mDiagnosticOS;

    @Expose()
    @SerializedName("x-client-CPU")
    private String mDiagnosticCPU;

    @Expose()
    @SerializedName("x-client-DM")
    private String mDiagnosticDM;

    @Expose()
    @SerializedName(INSTANCE_AWARE)
    private Boolean mMultipleCloudAware;

    protected transient AzureActiveDirectorySlice mSlice;

    protected transient Map<String, String> mFlightParameters;

    /**
     * Constructor of MicrosoftAuthorizationRequest.
     */
    protected MicrosoftAuthorizationRequest(final Builder builder) {
        super(builder);
        mAuthority = builder.mAuthority;
        mLoginHint = builder.mLoginHint;
        mCorrelationId = builder.mCorrelationId;

        mPkceChallenge = PkceChallenge.newPkceChallenge();
        mCodeChallengeMethod = mPkceChallenge.getCodeChallengeMethod();
        mCodeChallenge = mPkceChallenge.getCodeChallenge();
        mState = generateEncodedState();

        if (builder.mSlice != null) {
            mSlice = builder.mSlice;
        }
        mFlightParameters = builder.mFlightParameters;

        mMultipleCloudAware = builder.mMultipleCloudAware;

        //Initialize the diagnostic properties.

        //TODO: Need to figure out how to flow this information down
        mLibraryVersion =  builder.mLibraryVersion;
        mLibraryName = builder.mLibraryName;
        mDiagnosticOS = String.valueOf(Build.VERSION.SDK_INT);
        mDiagnosticDM = android.os.Build.MODEL;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mDiagnosticCPU = Build.CPU_ABI;
        } else {
            final String[] supportedABIs = Build.SUPPORTED_ABIS;
            if (supportedABIs != null && supportedABIs.length > 0) {
                mDiagnosticCPU = supportedABIs[0];
            }
        }
    }


    public abstract static class Builder<B extends MicrosoftAuthorizationRequest.Builder<B>> extends AuthorizationRequest.Builder<B> {
        /**
         * Required.
         */
        private URL mAuthority;
        /**
         * Used to secure authorization code grants via Proof Key for Code Exchange (PKCE) from a native client.
         */
        private PkceChallenge mPkceChallenge;
        /**
         * The version of the calling library.
         */
        private String mLibraryVersion;

        /**
         * The name of the calling library.
         */
        private String mLibraryName;

        private AzureActiveDirectorySlice mSlice;

        private Map<String, String> mFlightParameters = new HashMap<>();

        private Boolean mMultipleCloudAware;

        public Builder() {
        }

        public B setAuthority(URL authority) {
            mAuthority = authority;
            return self();
        }

        public B setPkceChallenge(PkceChallenge pkceChallenge) {
            mPkceChallenge = pkceChallenge;
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

        public B setSlice(AzureActiveDirectorySlice slice) {
            mSlice = slice;
            return self();
        }

        public B setFlightParameters(Map<String, String> flightParameters) {
            mFlightParameters = flightParameters;
            return self();
        }

        public B setMultipleCloudAware(boolean multipleCloudAware) {
            mMultipleCloudAware = multipleCloudAware;
            return self();
        }

        public abstract B self();

    }

    public URL getAuthority() {
        return mAuthority;
    }

    public String getLoginHint() {
        return mLoginHint;
    }

    public UUID getCorrelationId() {
        return mCorrelationId;
    }

    public PkceChallenge getPkceChallenge() {
        return mPkceChallenge;
    }

    public String getLibraryVersion() {
        return mLibraryVersion;
    }

    public String getLibraryName() {
        return mLibraryName;
    }

    public String getDiagnosticOS() {
        return mDiagnosticOS;
    }

    public String getDiagnosticCPU() {
        return mDiagnosticCPU;
    }

    public String getDiagnosticDM() {
        return mDiagnosticDM;
    }

    public Boolean getMultipleCloudAware() {return mMultipleCloudAware;}

    public String getCodeChallenge() { return mCodeChallenge;}

    public String getCodeChallengeMethod() { return mCodeChallengeMethod;}

    public static String generateEncodedState() {
        final UUID stateUUID1 = UUID.randomUUID();
        final UUID stateUUID2 = UUID.randomUUID();
        final String state = stateUUID1.toString() + "-" + stateUUID2.toString();

        String encodedState;

        try {
            encodedState = Base64.encodeToString(state.getBytes("UTF-8"), Base64.NO_PADDING | Base64.URL_SAFE | Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException("Error generating encoded state parameter for Authorization Request", e);
        }

        return encodedState;

    }

    public static String decodeState(final String encodedState) {
        if (StringUtil.isEmpty(encodedState)) {
            Logger.warn(TAG, "Decode state failed because the input state is empty.");
            return null;
        }

        final byte[] stateBytes = Base64.decode(encodedState, Base64.NO_PADDING | Base64.URL_SAFE);
        return new String(stateBytes, Charset.defaultCharset());
    }
}
