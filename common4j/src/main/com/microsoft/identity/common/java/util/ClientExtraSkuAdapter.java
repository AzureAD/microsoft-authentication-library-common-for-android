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
 * This will be included in the requests to /authorize endpoint and the /token endpoint. As of
 * the writing of this JavaDoc, this field is only passed in brokered scenarios.<br>
 * The schema of this new field is index-based, and is as follows:<br>
 * <ul>
 *     <li>Index 0 – Auth SDK name and version </li>
 *     <ul><li>Example: MSAL.Android|5.4.0</li></ul>
 *     <li>Index 1 – MSAL runtime version. MSAL runtime name is omitted as static</li>
 *     <ul><li>Example: |1.2.5</li></ul>
 *     <li>Index 2 - Browser extension name and version</li>
 *     <ul><li>Example: Chrome|1.0.7</li></ul>
 *     <li>Index 3 -  Browser core version. Browser core name is omitted as static</li>
 *     <ul><li>Example: |2.5.7</li></ul>
 * </ul>
 * Example output where no MSAL.Runtime or Browser Core is used: MSAL.Android|5.4.0,|,Chrome|1.0.7,|
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
        // Index 0 - Auth SDK
        stringBuilder.append(mSrcSku);
        stringBuilder.append("|");
        stringBuilder.append(mSrcSkuVer);
        stringBuilder.append(",");

        // Index 1 - MSAL runtime version
        // We don't send anything for the name, since MSAL.Runtime name is static, this can change in
        // the future as needed
        stringBuilder.append("|");
        stringBuilder.append(mMsalRuntimeVer);
        stringBuilder.append(",");

        // Index 2 - Browser Extension
        stringBuilder.append(mBrowserExtSku);
        stringBuilder.append("|");
        stringBuilder.append(mBrowserExtVer);
        stringBuilder.append(",");

        // Index 3 - Browser Core
        // We don't send anything for the name, since Browser Core name is static, this can change in
        // the future as needed
        stringBuilder.append("|");
        stringBuilder.append(mBrowserCoreVer);

        return stringBuilder.toString();
    }
}
