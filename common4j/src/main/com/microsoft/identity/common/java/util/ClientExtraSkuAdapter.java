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
package com.microsoft.identity.common.java.util;

import lombok.Builder;
import lombok.experimental.Accessors;

/**
 * Helper class in common4j to assist in supplying "x-client-xtra-sku" field for ESTS Telemetry.
 * This will be included in the /authorize endpoint request and the /token endpoint.
 */
@Builder
@Accessors(prefix = "m")
public class ClientExtraSkuAdapter {

    @Builder.Default
    private String mSrcSku = "";

    @Builder.Default
    private String mSrcSkuVer = "";

    @Builder.Default
    private String mMsalRuntimeVer = "";

    @Builder.Default
    private String mBrowserExtSku = "";

    @Builder.Default
    private String mBrowserExtVer = "";

    @Builder.Default
    private String mBrowserCoreVer = "";

    public String toString(){
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(mSrcSku);
        stringBuilder.append(",");
        stringBuilder.append(mSrcSkuVer);
        stringBuilder.append(",");
        stringBuilder.append(mMsalRuntimeVer);
        stringBuilder.append(",");
        stringBuilder.append(mBrowserExtSku);
        stringBuilder.append(",");
        stringBuilder.append(mBrowserExtVer);
        stringBuilder.append(",");
        stringBuilder.append(mBrowserCoreVer);

        return stringBuilder.toString();
    }
}