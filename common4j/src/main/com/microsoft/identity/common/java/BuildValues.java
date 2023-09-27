//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java;

import javax.annotation.Nonnull;

//Used as a wrapper for setting and accessing values through either the generated BuildConfig.java class,
//or from parameters set via the NativeAuthPublicClientApplicationConfiguration.kt file
public class BuildValues {
    //Appended to the URL constructed in NativeAuthOAuth2Configuration,
    // used for making calls to tenants on test slices
    @Nonnull
    private static String DC = BuildConfig.DC;

    public static String getDC()
    {
        return DC;
    }

    public static void setDC(String dc) {
        DC = dc;
    }

    //The mock API authority used for testing will be rejected by validation logic run on instantiation. This flag is used to bypass those checks in various points in the application
    @Nonnull
    private static Boolean USE_REAL_AUTHORITY = BuildConfig.USE_REAL_AUTHORITY;

    public static Boolean shouldUseRealAuthority()
    {
        return USE_REAL_AUTHORITY;
    }

    public static void setUseRealAuthority(Boolean ura) {
        USE_REAL_AUTHORITY = ura;
    }
}
