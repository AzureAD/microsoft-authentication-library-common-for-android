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
package com.microsoft.identity.common.internal.ui.webview.certbasedauth;

import android.webkit.ClientCertRequest;

import androidx.annotation.Nullable;

import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter @Accessors(prefix = "m")
public class TestClientCertRequest extends ClientCertRequest {

    private boolean mProceeded;
    private boolean mCancelled;
    private final String[] mKeyTypes;
    private final Principal[] mPrincipals;

    TestClientCertRequest() {
        mKeyTypes = new String[0];
        mPrincipals = new Principal[0];
    }

    TestClientCertRequest(@Nullable final String[] keyTypes,
                          @Nullable final Principal[] principals) {
        mKeyTypes = keyTypes;
        mPrincipals = principals;
    }


    @Nullable
    @Override
    public String[] getKeyTypes() {
        return mKeyTypes;
    }

    @Nullable
    @Override
    public Principal[] getPrincipals() {
        return mPrincipals;
    }

    @Override
    public String getHost() {
        return null;
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public void proceed(PrivateKey privateKey, X509Certificate[] chain) {
        mProceeded = true;
    }

    @Override
    public void ignore() {

    }

    @Override
    public void cancel() {
        mCancelled = true;
    }
}
