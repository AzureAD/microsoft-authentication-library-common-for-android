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
package com.microsoft.identity.common.internal.ui.webview.challengehandlers;
import java.io.Serializable;
import java.util.List;

public class PKeyAuthChallenge implements Serializable {
    /**
     * Serial version id.
     */
    private static final long serialVersionUID = 1035116074451575588L;

    private String mNonce;

    private String mContext;

    /**
     * Authorization endpoint will return accepted authorities.
     * The mCertAuthorities could be empty when either no certificate or no permission for ADFS
     * service account for the Device container in AD.
     */
    private List<String> mCertAuthorities;

    /**
     * Token endpoint will return thumbprint.
     */
    private String mThumbprint;

    private String mVersion;

    private String mSubmitUrl;

    protected PKeyAuthChallenge(final Builder builder) {
        mNonce = builder.mNonce;
        mContext = builder.mContext;
        mCertAuthorities = builder.mCertAuthorities;
        mThumbprint = builder.mThumbprint;
        mVersion = builder.mVersion;
        mSubmitUrl = builder.mSubmitUrl;
    }


    public static class Builder {
        private String mNonce = "";
        private String mContext = "";
        private List<String> mCertAuthorities;
        private String mThumbprint = "";
        private String mVersion;
        private String mSubmitUrl;

        public Builder setNonce(final String nonce) {
            mNonce = nonce;
            return self();
        }

        public Builder setContext(final String context) {
            mContext = context;
            return self();
        }

        public Builder setCertAuthorities(final List<String> certAuthorities) {
            mCertAuthorities = certAuthorities;
            return self();
        }

        public Builder setThumbprint(final String thumbprint) {
            mThumbprint = thumbprint;
            return self();
        }

        public Builder setVersion(final String version) {
            mVersion = version;
            return self();
        }

        public Builder setSubmitUrl(final String submitUrl) {
            mSubmitUrl = submitUrl;
            return self();
        }

        public Builder self() {
            return this;
        }

        public PKeyAuthChallenge build() {
            return new PKeyAuthChallenge(this);
        }
    }

    public String getNonce() {
        return mNonce;
    }

    public String getContext() {
        return mContext;
    }

    public List<String> getCertAuthorities() {
        return mCertAuthorities;
    }

    public String getThumbprint() {
        return mThumbprint;
    }

    public String getVersion() {
        return mVersion;
    }

    public String getSubmitUrl() {
        return mSubmitUrl;
    }
}