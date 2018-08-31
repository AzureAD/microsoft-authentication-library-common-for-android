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
package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import android.support.annotation.NonNull;

import java.net.URL;

public class AzureActiveDirectoryBrokerAuthorizationRequest extends AzureActiveDirectoryAuthorizationRequest {
    private String mCallingPackage;
    private String mSignatureDigest;

    public AzureActiveDirectoryBrokerAuthorizationRequest(final Builder builder) {
        super(builder);
        mCallingPackage = builder.mCallingPackage;
        mSignatureDigest = builder.mSignatureDigest;
    }

    public String getCallingPackage() {
        return mCallingPackage;
    }

    public String getSignatureDigest() {
        return mSignatureDigest;
    }

    public static class Builder extends AzureActiveDirectoryAuthorizationRequest.Builder<AzureActiveDirectoryBrokerAuthorizationRequest> {
        private String mCallingPackage;
        private String mSignatureDigest;

        public Builder(@NonNull final String clientId,
                       @NonNull final String redirectUri,
                       @NonNull final String resource) {
            super(clientId, redirectUri,resource);
        }

        public Builder setCallingPackage(final String callingPackage) {
            mCallingPackage = callingPackage;
            return this;
        }

        public Builder setSignatureDigest(final String signatureDigest) {
            mSignatureDigest = signatureDigest;
            return this;
        }
    }
}
