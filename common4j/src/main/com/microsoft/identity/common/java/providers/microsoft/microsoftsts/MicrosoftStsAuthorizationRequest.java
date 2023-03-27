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
package com.microsoft.identity.common.java.providers.microsoft.microsoftsts;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAuthorizationRequest;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectorySlice;
import com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration;
import com.microsoft.identity.common.java.util.CommonURIBuilder;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.UrlUtil;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.experimental.Accessors;

public class MicrosoftStsAuthorizationRequest extends MicrosoftAuthorizationRequest<MicrosoftStsAuthorizationRequest> {
    private static final Object TAG = MicrosoftStsAuthorizationRequest.class.getSimpleName();

    /**
     * Serial version id.
     */
    private static final long serialVersionUID = 6545759826515911472L;

    private static final String AUTHORIZATION_ENDPOINT = "oAuth2/v2.0/authorize";

    /**
     * Indicates the type of user interaction that is required. The only valid values at this time are 'login', 'none', and 'consent'.
     */
    @Expose()
    @Getter
    @Accessors(prefix = "m")
    @SerializedName("prompt")
    private final String mPrompt;

    @Getter
    @Accessors(prefix = "m")
    @SerializedName("login_req")
    private final String mUid;

    @Getter
    @Accessors(prefix = "m")
    @SerializedName("domain_req")
    private final String mUtid;

    /**
     * Since some physical application packages share client ids we want to be able to distinguish between
     * these relative to access tokens in the cache.
     */
    @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    @Getter
    @Accessors(prefix = "m")
    private final transient String mApplicationIdentifier;

    /**
     * Since some physical application packages share client ids we want to be able to distinguish between
     * these relative to access tokens in the cache.
     */
    @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    @Getter
    @Accessors(prefix = "m")
    private final transient String mMamEnrollmentIdentifier;

    /**
     * Version name of the installed Company Portal app.
     */
    @Expose()
    @SerializedName("cpVersion")
    private final String mCompanyPortalVersion;

    @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    @Getter
    @Accessors(prefix = "m")
    private final transient String mDisplayableId;

    /**
     * The original scope of the request.
     * Does not contain extra scopes to consent.
     * <p>
     * TODO: We might want to remove this at some point.
     * I (Dome) don't think this belongs here.
     * It seems like this is plugged into AuthRequest just to be passed to TokenRequest.
     * That said, I'm not sure if removing this will have any unintended side effects,
     * so I'm keeping it here for now.
     */
    @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    @Getter
    @Accessors(prefix = "m")
    private final transient String mTokenScope;

    protected transient AzureActiveDirectorySlice mSlice;

    @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    protected transient Map<String, String> mFlightParameters;

    // TODO private transient InstanceDiscoveryMetadata mInstanceDiscoveryMetadata;
    // TODO private boolean mIsExtendedLifetimeEnabled = false;

    public static final class Prompt {
        /**
         * AcquireToken will send prompt=select_account to the authorize endpoint. Shows a list of users from which can be
         * selected for authentication.
         */
        public static final String SELECT_ACCOUNT = "select_account";

        /**
         * AcquireToken will send prompt=login to the authorize endpoint.  The user will always be prompted for credentials by the service.
         */
        public static final String FORCE_LOGIN = "login";

        /**
         * AcquireToken will send prompt=consent to the authorize endpoint.  The user will be prompted to consent even if consent was granted before.
         */
        public static final String CONSENT = "consent";
    }

    /**
     * If hsu=1 is passed, eSTS will hide the option which allows user to switch login hint.
     * This can only be passed if login_hint is provided.
     */
    public static final String HIDE_SWITCH_USER_QUERY_PARAMETER = "hsu";

    /**
     * Add this field to store the openIdConfiguration passed as part of the builder.
     * This will be used to fetch the atuorization endpoint from openID Configutration if it was
     * loaded.
     */
    private transient final OpenIdProviderConfiguration mOpenIdProviderConfiguration;

    protected MicrosoftStsAuthorizationRequest(final Builder builder) {
        super(builder);

        mPrompt = builder.mPrompt;
        mUid = builder.mUid;
        mUtid = builder.mUtid;
        mCompanyPortalVersion = builder.mCompanyPortalVersion;
        mDisplayableId = builder.mDisplayableId;
        mTokenScope = builder.mTokenScope;
        mSlice = builder.mSlice;
        mFlightParameters = builder.mFlightParameters;
        mApplicationIdentifier = builder.mApplicationIdentifier;
        mMamEnrollmentIdentifier = builder.mMamEnrollmentIdentifier;
        mOpenIdProviderConfiguration = builder.mOpenIdProviderConfiguration;
    }

    public static class Builder extends MicrosoftAuthorizationRequest.Builder<MicrosoftStsAuthorizationRequest.Builder> {

        private String mUid;
        private String mUtid;
        private String mDisplayableId;
        private String mApplicationIdentifier;
        private String mMamEnrollmentIdentifier;
        private String mTokenScope;
        private String mCompanyPortalVersion;
        private String mPrompt;
        private AzureActiveDirectorySlice mSlice;
        private Map<String, String> mFlightParameters = new HashMap<>();
        private OpenIdProviderConfiguration mOpenIdProviderConfiguration;

        public MicrosoftStsAuthorizationRequest.Builder setUid(String uid) {
            mUid = uid;
            return self();
        }

        public MicrosoftStsAuthorizationRequest.Builder setUtid(String utid) {
            mUtid = utid;
            return self();
        }

        public MicrosoftStsAuthorizationRequest.Builder setApplicationIdentifier(String appliationIdentifier) {
            mApplicationIdentifier = appliationIdentifier;
            return self();
        }

        public MicrosoftStsAuthorizationRequest.Builder setMamEnrollmentIdentifier(String mamEnrollmentIdentifier) {
            mMamEnrollmentIdentifier = mamEnrollmentIdentifier;
            return self();
        }

        public MicrosoftStsAuthorizationRequest.Builder setDisplayableId(String displayableId) {
            mDisplayableId = displayableId;
            return self();
        }

        public MicrosoftStsAuthorizationRequest.Builder setTokenScope(String tokenScope) {
            mTokenScope = tokenScope;
            return self();
        }

        public MicrosoftStsAuthorizationRequest.Builder setInstalledCompanyPortalVersion(String companyPortalVersion) {
            mCompanyPortalVersion = companyPortalVersion;
            return self();
        }

        public MicrosoftStsAuthorizationRequest.Builder setPrompt(String prompt) {
            mPrompt = prompt;
            return self();
        }

        public MicrosoftStsAuthorizationRequest.Builder setSlice(AzureActiveDirectorySlice slice) {
            mSlice = slice;
            return self();
        }

        public MicrosoftStsAuthorizationRequest.Builder setFlightParameters(Map<String, String> flightParameters) {
            mFlightParameters = flightParameters;
            return self();
        }

        public MicrosoftStsAuthorizationRequest.Builder setOpenIdProviderConfiguration(OpenIdProviderConfiguration config) {
            mOpenIdProviderConfiguration = config;
            return self();
        }

        @Override
        public MicrosoftStsAuthorizationRequest.Builder self() {
            return this;
        }

        public MicrosoftStsAuthorizationRequest build() {
            return new MicrosoftStsAuthorizationRequest(this);
        }
    }

    @Override
    public URI getAuthorizationRequestAsHttpRequest() throws ClientException {
        final CommonURIBuilder builder = new CommonURIBuilder(super.getAuthorizationRequestAsHttpRequest());
        builder.addParametersIfAbsent(mFlightParameters);

        if (mSlice != null) {
            if (!StringUtil.isNullOrEmpty(mSlice.getSlice())) {
                builder.addParameterIfAbsent(AzureActiveDirectorySlice.SLICE_PARAMETER, mSlice.getSlice());
            }
            if (!StringUtil.isNullOrEmpty(mSlice.getDataCenter())) {
                builder.addParameterIfAbsent(AzureActiveDirectorySlice.DC_PARAMETER, mSlice.getDataCenter());
            }
        }

        // If login_hint is provided, block the user from switching user during login.
        // hsu = HideSwitchUser
        if (!StringUtil.isNullOrEmpty(getLoginHint())) {
            builder.addParameterIfAbsent(HIDE_SWITCH_USER_QUERY_PARAMETER, "1");
        }

        try {
            return builder.build();
        } catch (final URISyntaxException e) {
            throw new ClientException(ClientException.MALFORMED_URL, e.getMessage(), e);
        }
    }

    @Override
    public String getAuthorizationEndpoint() throws ClientException {
        final String methodName = ":getAuthorizationEndpoint";

        // If the openid configuration was passed, use it to fetch the authorization endpoint
        if (mOpenIdProviderConfiguration != null) {
            return mOpenIdProviderConfiguration.getAuthorizationEndpoint();
        }

        if (this.getAuthority() == null) {
            Logger.error(TAG + methodName, "Authority is null. " +
                    "cannot construct authorization endpoint URL.", null);
            throw new IllegalStateException("Authority is null.");
        }

        try {
            return UrlUtil.appendPathToURL(this.getAuthority(), AUTHORIZATION_ENDPOINT).toString();
        } catch (final URISyntaxException | MalformedURLException e) {
            throw new ClientException(ClientException.MALFORMED_URL, e.getMessage(), e);
        }
    }
}

