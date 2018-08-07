package com.microsoft.identity.common.internal.ui.systembrowser;

import android.content.pm.PackageInfo;
import android.support.annotation.NonNull;

import java.util.Set;

/**
 * Represents a browser used for an authorization flow.
 */
public class Browser {
    private static final String DIGEST_SHA_512 = "SHA-512";

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

    /**
     * Whether it is intended that the browser will be used via a custom tab.
     */
    private final Boolean mUseCustomTab = true;

    /*public Browser(@NonNull PackageInfo packageInfo) {
        this(packageInfo.packageName

    }*/

    public Browser(@NonNull String packageName, @NonNull Set<String> signatureHashes, @NonNull String version) {
        mPackageName = packageName;
        mSignatureHashes = signatureHashes;
        mVersion = version;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public Set<String> getSignatureHashes() {
        return mSignatureHashes;
    }

    public String getVersion() {
        return mVersion;
    }

    public Boolean getUseCustomTab() {
        return mUseCustomTab;
    }

}
