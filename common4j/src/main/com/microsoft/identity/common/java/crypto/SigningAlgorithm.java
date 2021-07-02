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
package com.microsoft.identity.common.java.crypto;

import lombok.NonNull;

/**
 * Signing algorithms supported by our underlying keystore. Not all algs available at all device
 * levels.
 */
public enum SigningAlgorithm {
    //@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    MD5_WITH_RSA("MD5withRSA"),

    //@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    NONE_WITH_RSA("NONEwithRSA"),

    SHA_1_WITH_RSA("SHA1withRSA"),

    //@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    SHA_256_WITH_RSA("SHA256withRSA"),

    //@RequiresApi(Build.VERSION_CODES.M)
    SHA_256_WITH_RSA_PSS("SHA256withRSA/PSS"),

    //@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    SHA_384_WITH_RSA("SHA384withRSA"),

    //@RequiresApi(Build.VERSION_CODES.M)
    SHA_384_WITH_RSA_PSS("SHA384withRSA/PSS"),

    //@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    SHA_512_WITH_RSA("SHA512withRSA"),

    //@RequiresApi(Build.VERSION_CODES.M)
    SHA_512_WITH_RSA_PSS("SHA512withRSA/PSS");

    private final String mValue;

    SigningAlgorithm(@NonNull final String value) {
        mValue = value;
    }

    @Override
    @NonNull
    public String toString() {
        return mValue;
    }
}
