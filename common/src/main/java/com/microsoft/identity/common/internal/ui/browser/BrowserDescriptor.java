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
package com.microsoft.identity.common.internal.ui.browser;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

public class BrowserDescriptor implements Serializable {
    @SerializedName("browser_package_name")
    private String mPackageName;

    @SerializedName("browser_signature_hashes")
    private Set<String> mSignatureHashes;

    @SerializedName("browser_use_customTab")
    private boolean mUseCustomTab;

    @SerializedName("browser_version_lower_bound")
    private String mVersionLowerBound;

    @SerializedName("browser_version_upper_bound")
    private String mVersionUpperBound;

    public BrowserDescriptor(
            @NonNull final String packageName,
            @NonNull final Set<String> signatureHashes,
            final boolean useCustomTab,
            @Nullable final String versionLowerBound,
            @Nullable final String versionUpperBound) {
        mPackageName = packageName;
        mSignatureHashes = signatureHashes;
        mUseCustomTab = useCustomTab;
        mVersionLowerBound = versionLowerBound;
        mVersionUpperBound = versionUpperBound;
    }

    public BrowserDescriptor(
            @NonNull final String packageName,
            @NonNull final String signatureHash,
            final boolean useCustomTab,
            @Nullable final String versionLowerBound,
            @Nullable final String versionUpperBound) {
        mPackageName = packageName;
        mSignatureHashes = Collections.singleton(signatureHash);
        mUseCustomTab = useCustomTab;
        mVersionLowerBound = versionLowerBound;
        mVersionUpperBound = versionUpperBound;
    }

    public boolean matches(@NonNull Browser browser) {
        if (!mPackageName.equalsIgnoreCase(browser.getPackageName())) {
            return false;
        }

        if (!mSignatureHashes.equals(browser.getSignatureHashes())) {
            return false;
        }

        if (mUseCustomTab != browser.isCustomTabsServiceSupported()) {
            return false;
        }

        if (!StringUtil.isEmpty(mVersionLowerBound)
                && compareSemanticVersion(browser.getVersion(), mVersionLowerBound) == -1) {
            return false;
        }

        if (!StringUtil.isEmpty(mVersionUpperBound)
                && compareSemanticVersion(browser.getVersion(), mVersionUpperBound) == 1) {
            return false;
        }

        return true;
    }

    /**
     * The function to compare the two versions.
     *
     * @param thisVersion
     * @param thatVersion
     * @return int -1 if thisVersion is smaller than thatVersion,
     *         1 if thisVersion is larger than thatVersion,
     *         0 if thisVersion is equal to thatVersion.
     */
    private int compareSemanticVersion(
            @NonNull final String thisVersion,
            @NonNull final String thatVersion) {
        final String[] thisParts = thisVersion.split("\\.");
        final String[] thatParts = thatVersion.split("\\.");
        final int length = Math.max(thisParts.length, thatParts.length);
        for(int i = 0; i < length; i++) {
            int thisPart = i < thisParts.length ?
                    Integer.parseInt(thisParts[i]) : 0;
            int thatPart = i < thatParts.length ?
                    Integer.parseInt(thatParts[i]) : 0;

            if(thisPart < thatPart) {
                return -1;
            }

            if(thisPart > thatPart) {
                return 1;
            }
        }

        return 0;
    }

}
