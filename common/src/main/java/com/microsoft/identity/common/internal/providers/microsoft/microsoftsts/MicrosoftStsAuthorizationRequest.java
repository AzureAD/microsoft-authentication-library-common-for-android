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
package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import android.net.Uri;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationRequest;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class MicrosoftStsAuthorizationRequest extends MicrosoftAuthorizationRequest<MicrosoftStsAuthorizationRequest> {
    /**
     * Serial version id.
     */
    private static final long serialVersionUID = 6545759826515911472L;
    /**
     * Indicates the type of user interaction that is required. The only valid values at this time are 'login', 'none', and 'consent'.
     */
    @SerializedName("prompt")
    private String mPrompt;
    @SerializedName("login_req")
    private String mUid;
    @SerializedName("domain_req")
    private String mUtid;
    //@SerializedName("login_hint")
    private transient String mDisplayableId;

    private transient String mSliceParameters;

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


    protected MicrosoftStsAuthorizationRequest(final Builder builder) {
        super(builder);

        mPrompt = builder.mPrompt;
        mUid = builder.mUid;
        mUtid = builder.mUtid;
        mDisplayableId = builder.mDisplayableId;
        mSliceParameters = builder.mSliceParameters;
    }

    public static class Builder extends MicrosoftAuthorizationRequest.Builder<MicrosoftStsAuthorizationRequest.Builder> {

        private String mUid;
        private String mUtid;
        private String mDisplayableId;
        private String mSliceParameters;


        public MicrosoftStsAuthorizationRequest.Builder setUid(String uid) {
            mUid = uid;
            return self();
        }

        public MicrosoftStsAuthorizationRequest.Builder setUtid(String utid) {
            mUtid = utid;
            return self();
        }

        public MicrosoftStsAuthorizationRequest.Builder setDisplayableId(String displayableId) {
            mDisplayableId = displayableId;
            return self();
        }

        public MicrosoftStsAuthorizationRequest.Builder setSliceParameters(String sliceParameters) {
            mSliceParameters = sliceParameters;
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

    public String getUid() {
        return mUid;
    }

    public String getUtid() {
        return mUtid;
    }

    public String getDisplayableId() {
        return mDisplayableId;
    }

    public String getPrompt() {
        return mPrompt;
    }

    public String getSliceParameters() {
        return mSliceParameters;
    }

    @Override
    public Uri getAuthorizationRequestAsHttpRequest() throws UnsupportedEncodingException {
        Uri.Builder uriBuilder = Uri.parse(getAuthorizationEndpoint()).buildUpon();
        for (Map.Entry<String, String> entry : ObjectMapper.serializeObjectHashMap(this).entrySet()) {
            uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, String> entry : ObjectMapper.deserializeQueryStringToMap(getExtraQueryParam()).entrySet()) {
            uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, String> entry : ObjectMapper.deserializeQueryStringToMap(mSliceParameters).entrySet()) {
            uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
        }

        return uriBuilder.build();
    }

    @Override
    public String getAuthorizationEndpoint() {
        //TODO: Need to take authority aliasing via instance discovery into account here
        return "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
    }
}
