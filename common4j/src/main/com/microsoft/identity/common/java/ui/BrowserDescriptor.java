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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

@Getter
@Accessors(prefix = "m")
public class BrowserDescriptor implements Serializable {
    private static final long serialVersionUID = 3745812401643512530L;

    @SerializedName("browser_package_name")
    final private String mPackageName;

    @SerializedName("browser_signature_hashes")
    final private Set<String> mSignatureHashes;

    @SerializedName("browser_version_lower_bound")
    final private String mVersionLowerBound;

    @SerializedName("browser_version_upper_bound")
    final private String mVersionUpperBound;

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

    static private BrowserDescriptor getBrowserDescriptorForEdge() {
        final HashSet<String> edgeSignatureHashes = new HashSet<>();
        edgeSignatureHashes.add("Ivy-Rk6ztai_IudfbyUrSHugzRqAtHWslFvHT0PTvLMsEKLUIgv7ZZbVxygWy_M5mOPpfjZrd3vOx3t-cA6fVQ==");
        return new BrowserDescriptor(
                "com.microsoft.emmx",
                edgeSignatureHashes,
                null,
                null
        );
    }

    static private BrowserDescriptor getBrowserDescriptorForChrome() {
        final HashSet<String> signatureHashes = new HashSet<>();
        signatureHashes.add("7fmduHKTdHHrlMvldlEqAIlSfii1tl35bxj1OXN5Ve8c4lU6URVu4xtSHc3BVZxS6WWJnxMDhIfQN0N0K2NDJg==");
        return new BrowserDescriptor(
                "com.android.chrome",
                signatureHashes,
                null,
                null
        );
    }

    static private BrowserDescriptor getBrowserDescriptorForAea() {
        final HashSet<String> signatureHashes = new HashSet<>();
        signatureHashes.add("Nd3EDftVD0lR3Lz0Odq8NMkWWyM5CT8lahePkMtzvS6YkVYne_Hn5jaDSxrdXkN1s4AywAnav2RnarZvcqVFJQ==");
        return new BrowserDescriptor(
                "com.amazon.enterprise.access.android",
                signatureHashes,
                null,
                null
        );
    }

    /**
     * Return a list of BrowserDescriptors that are considered safe for the Switch to browser flow.
     */
    static public List<BrowserDescriptor> getBrowserSafeListForSwitchBrowser() {
        final List<BrowserDescriptor> browserDescriptors = new ArrayList<>();
        browserDescriptors.add(getBrowserDescriptorForChrome());
        browserDescriptors.add(getBrowserDescriptorForEdge());
        browserDescriptors.add(getBrowserDescriptorForAea());
        return browserDescriptors;
    }

    /**
     * List of System Browsers which can be used from broker, currently only Chrome is supported.
     * This information here is populated from the default browser safe-list in MSAL.
     *
     * @return List of BrowserDescriptors which are considered safe for the broker.
     */
    static public List<BrowserDescriptor> getBrowserSafeListForBroker() {
        final List<BrowserDescriptor> browserDescriptors = new ArrayList<>();
        browserDescriptors.add(getBrowserDescriptorForChrome());
        return browserDescriptors;
    }
}
