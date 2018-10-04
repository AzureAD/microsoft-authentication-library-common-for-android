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
package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectoryb2c;

import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationRequest;

/**
 * Azure Active Directory B2C Authorization Request.
 */
public class AzureActiveDirectoryB2CAuthorizationRequest extends MicrosoftAuthorizationRequest<AzureActiveDirectoryB2CAuthorizationRequest> {
    private String mPrompt;

    private AzureActiveDirectoryB2CAuthorizationRequest(final Builder builder) {
        super(builder);
    }

    public static final class Builder extends MicrosoftAuthorizationRequest.Builder<AzureActiveDirectoryB2CAuthorizationRequest.Builder> {
        private String mPrompt;

        public Builder() {
        }

        public Builder setPrompt(String prompt) {
            mPrompt = prompt;
            return this;
        }

        public AzureActiveDirectoryB2CAuthorizationRequest build() {
            return new AzureActiveDirectoryB2CAuthorizationRequest(this);
        }

        @Override
        public AzureActiveDirectoryB2CAuthorizationRequest.Builder self() {
            return this;
        }


    }

    public String getPrompt() {
        return mPrompt;
    }

    @Override
    public String getAuthorizationEndpoint() {
        throw new UnsupportedOperationException("Not implemented.");
    }
}
