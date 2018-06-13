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

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class MicrosoftStsBrokerAuthorizationRequest extends MicrosoftStsAuthorizationRequest {
    private String mCallingPackage;
    private String mSignatureDigest;

    public String getCallingPackage() {
        return mCallingPackage;
    }

    public void setCallingPackage(final String callingPackage) {
        mCallingPackage = callingPackage;
    }

    public String getSignatureDigest() {
        return mSignatureDigest;
    }

    public void setSignatureDigest(final String signatureDigest) {
        mSignatureDigest = signatureDigest;
    }

    @Override
    public String getAuthorizationStartUrl() throws ClientException, UnsupportedEncodingException {
        final String startUrl = super.getAuthorizationStartUrl();
        if (!StringExtensions.isNullOrBlank(mCallingPackage)
                && !StringExtensions.isNullOrBlank(mSignatureDigest)) {
            return startUrl + "&package_name="
                    + URLEncoder.encode(mCallingPackage, ENCODING_UTF8)
                    + "&signature="
                    + URLEncoder.encode(mSignatureDigest, ENCODING_UTF8);
        }

        return startUrl;
    }
}
