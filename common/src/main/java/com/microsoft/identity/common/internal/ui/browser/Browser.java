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

import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import androidx.annotation.NonNull;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a browser used for an authorization flow.
 */
public class Browser {
    private static final String DIGEST_SHA_512 = "SHA-512";
    private static final int PRIME_HASH_FACTOR = 92821;

    /**
     * The package name of the browser app.
     */
    private final String mPackageName;

    /**
     * The set of {@link android.content.pm.Signature signatures} of the browser app,
     * which have been hashed with SHA-512, and Base-64 URL-safe encoded.
     */
    private final Set<String> mSignatureHashes;

    /**
     * The version string of the browser app.
     */
    private final String mVersion;

    private final Boolean mIsCustomTabsServiceSupported; //NOPMD

    /**
     * Creates a browser object from a {@link PackageInfo} object returned from the
     * {@link android.content.pm.PackageManager}. The object is expected to include the
     * signatures of the app, which can be retrieved with the
     * {@link android.content.pm.PackageManager#GET_SIGNATURES GET_SIGNATURES} flag when
     * calling {@link android.content.pm.PackageManager#getPackageInfo(String, int)}.
     */
    public Browser(@NonNull PackageInfo packageInfo) {
        this(packageInfo.packageName, generateSignatureHashes(packageInfo.signatures), packageInfo.versionName, false);
    }

    public Browser(@NonNull PackageInfo packageInfo, final Boolean isCustomTabsServiceSupported) {
        this(packageInfo.packageName, generateSignatureHashes(packageInfo.signatures), packageInfo.versionName, isCustomTabsServiceSupported);
    }

    /**
     * Creates a browser object with the core properties.
     *
     * @param packageName     The Android package name of the browser.
     * @param signatureHashes The set of SHA-512, Base64 url safe encoded signatures for the app.
     * @param version         The version name of the browser.
     */
    public Browser(@NonNull String packageName, @NonNull Set<String> signatureHashes, @NonNull String version, boolean isCustomTabsServiceSupported) {
        mPackageName = packageName;
        mSignatureHashes = signatureHashes;
        mVersion = version;
        mIsCustomTabsServiceSupported = isCustomTabsServiceSupported;
    }

    /**
     * Return the package name.
     *
     * @return String of package name.
     */
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * Return the signature hashes of the browser application.
     *
     * @return Set of String
     */
    public Set<String> getSignatureHashes() {
        return mSignatureHashes;
    }

    /**
     * Return the version of the browser application.
     *
     * @return String of the version
     */
    public String getVersion() {
        return mVersion;
    }

    /**
     * Generates a set of SHA-512, Base64 url-safe encoded signature hashes from the provided
     * array of signatures.
     */
    @NonNull
    public static Set<String> generateSignatureHashes(@NonNull Signature[] signatures) {
        Set<String> signatureHashes = new HashSet<>();
        for (Signature signature : signatures) {
            try {
                MessageDigest digest = MessageDigest.getInstance(DIGEST_SHA_512);
                byte[] hashBytes = digest.digest(signature.toByteArray());
                signatureHashes.add(Base64.encodeToString(hashBytes, Base64.URL_SAFE | Base64.NO_WRAP));
            } catch (final NoSuchAlgorithmException e) {
                throw new IllegalStateException(
                        "Platform does not support" + DIGEST_SHA_512 + " hashing");
            }
        }

        return signatureHashes;
    }

    public boolean isCustomTabsServiceSupported() {
        return mIsCustomTabsServiceSupported;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof Browser)) {
            return false;
        }

        final Browser other = (Browser) obj;
        return mPackageName.equals(other.getPackageName())
                && mVersion.equals(other.getVersion())
                && mSignatureHashes.equals(other.getSignatureHashes());
    }

    @Override
    public int hashCode() {
        int hash = mPackageName.hashCode();

        hash = PRIME_HASH_FACTOR * hash + mVersion.hashCode();
        hash = PRIME_HASH_FACTOR * hash + (mIsCustomTabsServiceSupported ? 1 : 0);

        for (String signatureHash : mSignatureHashes) {
            hash = PRIME_HASH_FACTOR * hash + signatureHash.hashCode();
        }

        return hash;
    }
}