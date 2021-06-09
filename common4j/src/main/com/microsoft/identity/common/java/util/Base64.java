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

import com.microsoft.identity.common.java.logging.Logger;
import com.nimbusds.jose.util.Base64URL;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * A wrapper class around Base64 operation.
 * */
public class Base64 {
    private static final String TAG = Base64.class.getSimpleName();

    @NonNull
    public static String encode(@NonNull final byte[] bytesToEncode) {
        return Base64URL.encode(bytesToEncode).toString();
    }

    @Nullable
    public static String encode(@Nullable final String stringToEncode) {
        if (StringUtil.isNullOrEmpty(stringToEncode)) {
            Logger.warn(TAG, "Failed to encode string because the input is empty.");
            return null;
        }

        return Base64URL.encode(stringToEncode).toString();
    }

    @Nullable
    public static String decode(@Nullable final String encodedString) {
        if (StringUtil.isNullOrEmpty(encodedString)) {
            Logger.warn(TAG, "Failed to decode string because the input is empty.");
            return null;
        }

        return Base64URL.from(encodedString).decodeToString();
    }
}
