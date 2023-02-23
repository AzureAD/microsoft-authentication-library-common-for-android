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
package com.microsoft.identity.common.java.jwt;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.util.StringUtil;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Setter;
import lombok.experimental.Accessors;

@SuppressFBWarnings({"URF_UNREAD_FIELD", "URF_UNREAD_FIELD"})
public abstract class AbstractJwtRequest {

    @Setter
    @Accessors(prefix = "m")
    @SerializedName("ctx")
    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "This is output through serialization")
    private String mCtx;

    @Setter
    @Accessors(prefix = "m")
    @SerializedName("refresh_token")
    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "This is output through serialization")
    private String mRefreshToken;

    @Setter
    @Accessors(prefix = "m")
    @SerializedName("x5c")
    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "This is output through serialization")
    private String mCert;

    @Setter
    @Accessors(prefix = "m")
    @SerializedName("client_id")
    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "This is output through serialization")
    private String mClientId;

    @Setter
    @Accessors(prefix = "m")
    @SerializedName("use")
    private String mUse;

    @SerializedName("resource")
    private String mResource;

    // HACKHACK: once AAD fixes the bug where it cannot accept scopes unless there is a resource,
    // remove this and fix all compiler errors and unused variable warnings
    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "This may be required for some time")
    public void setResource(final String resource) {
        if (!StringUtil.isNullOrEmpty(resource)) {
            mResource = resource;
        }
    }
}
