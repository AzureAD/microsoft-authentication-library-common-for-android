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

import com.nimbusds.jose.util.Base64URL;

import java.security.InvalidParameterException;

import lombok.NonNull;

/**
 * A wrapper class around Base64 operation.
 * */
public class Base64 {

    /**
     * URL-encodes the given byte array.
     * URL unsafe characters in the result will be replaced.
     */
    @NonNull
    public static String encode(@NonNull final byte[] bytesToEncode) {
        return Base64URL.encode(bytesToEncode).toString();
    }

    /**
     * Encodes the given byte array.
     * The result might contain URL unsafe characters.
     */
    @NonNull
    public static String encodeURLUnsafe(@NonNull final byte[] bytesToEncode) {
        return com.nimbusds.jose.util.Base64.encode(bytesToEncode).toString();
    }

    /**
     * URL-encodes the given string.
     * URL unsafe characters in the result will be replaced.
     */
    @NonNull
    public static String encode(@NonNull final String stringToEncode) {
        return Base64URL.encode(stringToEncode).toString();
    }

    /**
     * Decodes a URL-safe string.
     */
    @NonNull
    public static String decode(@NonNull final String encodedString) {
        return Base64URL.from(encodedString).decodeToString();
    }

    /**
     * Decodes a string which might contain URL unsafe characters.
     */
    @NonNull
    public static byte[] decodeURLUnsafeToByteArray(@NonNull final String encodedString) {
        return com.nimbusds.jose.util.Base64.from(encodedString).decode();
    }
}
