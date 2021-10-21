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
package com.microsoft.identity.common.java.ui;

import com.google.gson.annotations.SerializedName;

import edu.umd.cs.findbugs.annotations.Nullable;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

@Getter
@Accessors(prefix = "m")
public class BrowserDescriptor implements Serializable {
    private static final long serialVersionUID = 3745812401643512530L;

    @SerializedName("browser_package_name")
    private String mPackageName;

    @SerializedName("browser_signature_hashes")
    private Set<String> mSignatureHashes;

    @SerializedName("browser_version_lower_bound")
    private String mVersionLowerBound;

    @SerializedName("browser_version_upper_bound")
    private String mVersionUpperBound;

    public BrowserDescriptor(
            @NonNull final String packageName,
            @NonNull final Set<String> signatureHashes,
            @Nullable final String versionLowerBound,
            @Nullable final String versionUpperBound) {
        mPackageName = packageName;
        mSignatureHashes = signatureHashes;
        mVersionLowerBound = versionLowerBound;
        mVersionUpperBound = versionUpperBound;
    }

    public BrowserDescriptor(
            @NonNull final String packageName,
            @NonNull final String signatureHash,
            @Nullable final String versionLowerBound,
            @Nullable final String versionUpperBound) {
        mPackageName = packageName;
        mSignatureHashes = Collections.singleton(signatureHash);
        mVersionLowerBound = versionLowerBound;
        mVersionUpperBound = versionUpperBound;
    }
}
