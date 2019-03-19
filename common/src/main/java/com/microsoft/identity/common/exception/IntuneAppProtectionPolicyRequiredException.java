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

package com.microsoft.identity.common.exception;

public class IntuneAppProtectionPolicyRequiredException extends ServiceException {

    private String mAccountUpn;
    private String mAccountUserId;
    private String mTenantId;
    private String mAuthorityUrl;


    public IntuneAppProtectionPolicyRequiredException(final String errorCode,
                                                      final String errorMessage) {
        this(errorCode, errorMessage, null);
    }

    public IntuneAppProtectionPolicyRequiredException(final String errorCode,
                                                      final String errorMessage,
                                                      final Throwable throwable) {
        super(errorCode, errorMessage, throwable);
    }

    public String getAccountUpn() {
        return mAccountUpn;
    }

    public void setAccountUpn(String accountUpn) {
        mAccountUpn = accountUpn;
    }

    public String getAccountUserId() {
        return mAccountUserId;
    }

    public void setAccountUserId(String accountUserId) {
        mAccountUserId = accountUserId;
    }

    public String getTenantId() {
        return mTenantId;
    }

    public void setTenantId(String tenantId) {
        mTenantId = tenantId;
    }

    public String getAuthorityUrl() {
        return mAuthorityUrl;
    }

    public void setAuthorityUrl(String authorityUrl) {
        mAuthorityUrl = authorityUrl;
    }
}
