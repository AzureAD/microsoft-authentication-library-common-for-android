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
package com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAuthorizationRequest;
import com.microsoft.identity.common.java.util.ClientExtraSkuAdapter;

// Suppressing rawtype warnings due to the generic type MicrosoftAuthorizationRequest
@SuppressWarnings(WarningType.rawtype_warning)
public class AzureActiveDirectoryAuthorizationRequest extends MicrosoftAuthorizationRequest {
    private static final long serialVersionUID = 6813760067123426470L;
    /**
     * The App ID URI of the target web API.
     * This is required in one of either the authorization or token requests.
     * To ensure fewer authentication prompts place it in the authorization request to
     * ensure consent is received from the user.
     */
    @SerializedName("resource")
    private String mResource;
    //TODO The microsoft doc is different with V1 has currently.
    /**
     * Optional. Indicate the type of user interaction that is required.
     */
    @SerializedName("prompt")
    private String mPrompt;

    @SerializedName("claims")
    private String mClaimsChallenge;

    public static final class Prompt {
        /**
         * Acquire token will prompt the user for credentials only when necessary.
         */
        public static final String AUTO = "none";

        /**
         * The user will be prompted for credentials even if it is available in the
         * cache or in the form of refresh token. New acquired access token and
         * refresh token will be used to replace previous value. If Settings
         * switched to Auto, new request will use this latest token from cache.
         */
        public static final String ALWAYS = "login";

        /**
         * Re-authorizes (through displaying webview) the resource usage, making
         * sure that the resulting access token contains the updated claims. If user
         * logon cookies are available, the user will not be asked for credentials
         * again and the logon dialog will dismiss automatically. This is equivalent
         * to passing prompt=refresh_session as an extra query parameter during
         * the authorization.
         */
        public static final String REFRESH_SESSION = "refresh_session";

        /**
         * If Azure Authenticator or Company Portal is installed, this flag will have
         * the broker app force the prompt behavior, otherwise it will be same as Always.
         * If using embedded flow, please keep using Always, if FORCE_PROMPT is set for
         * embedded flow, the sdk will re-intepret it to Always.
         */
        public static final String FORCE_PROMPT = "login";

        /**
         * The user is prompted to select an account, interrupting single sign on.
         * The user may select an existing signed-in account, enter their credentials for
         * a remembered account, or choose to use a different account altogether.
         */
        public static final String SELECT_ACCOUNT = "select_account";

        /**
         * User consent has been granted, but needs to be updated.
         * The user should be prompted to consent.
         */
        public static final String CONSENT = "consent";

        /**
         * An administrator should be prompted to consent on behalf of all users in their organization.
         */
        public static final String ADMIN_CONSENT = "admin_consent";
    }

    protected AzureActiveDirectoryAuthorizationRequest(final Builder builder) {
        super(builder);
        mResource = builder.mResource;
        mPrompt = builder.mPrompt;
        mClaimsChallenge = builder.mClaimsChallenge;
    }

    public static class Builder extends MicrosoftAuthorizationRequest.Builder<AzureActiveDirectoryAuthorizationRequest.Builder> {
        /**
         * The App ID URI of the target web API.
         * This is required in one of either the authorization or token requests.
         * To ensure fewer authentication prompts place it in the authorization request to
         * ensure consent is received from the user.
         */
        private String mResource;

        //TODO The microsoft doc is different with V1 has currently.
        /**
         * Optional. Indicate the type of user interaction that is required.
         */
        private String mPrompt;

        private String mClaimsChallenge;


        public Builder setResource(final String resource) {
            mResource = resource;
            return this;
        }

        public Builder setPrompt(final String prompt) {
            mPrompt = prompt;
            return this;
        }

        public Builder setClaimsChallenge(final String claimsChallenge) {
            mClaimsChallenge = claimsChallenge;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        public AzureActiveDirectoryAuthorizationRequest build() {
            this.setLibraryName("ADAL.Android");
            this.setLibraryVersion("1.15.2");

            final ClientExtraSkuAdapter clientExtraSkuAdapter = new ClientExtraSkuAdapter();
            this.setClientExtraSky(clientExtraSkuAdapter.toString());

            return new AzureActiveDirectoryAuthorizationRequest(this);
        }


    }

    public String getResource() {
        return mResource;
    }

    public String getPrompt() {
        return mPrompt;
    }

    public String getClaimsChallenge() {
        return mClaimsChallenge;
    }

    @Override
    public String getAuthorizationEndpoint() {
        return null;
    }
}
