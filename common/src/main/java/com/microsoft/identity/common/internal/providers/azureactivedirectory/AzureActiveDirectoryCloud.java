package com.microsoft.identity.common.internal.providers.azureactivedirectory;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains information about a specific Azure Active Directory Cloud.  Azure Active Directory
 * as a service is available in multiple clouds.  World wide is the default; however their are sovereign clouds
 * available for Germany, China, etc....
 *
 */
public class AzureActiveDirectoryCloud {

    private final String mPreferredNetworkHostName;
    private final String mPreferredCacheHostName;
    private final List<String> mCloudHostAliases = new ArrayList();
    private final boolean mIsValidated;

    AzureActiveDirectoryCloud(boolean isValidated) {
        mIsValidated = isValidated;

        mPreferredNetworkHostName = null;
        mPreferredCacheHostName = null;
    }

    AzureActiveDirectoryCloud(final String preferredNetwork, final String preferredCache, final List<String> aliases) {
        mPreferredNetworkHostName = preferredNetwork;
        mPreferredCacheHostName = preferredCache;
        mCloudHostAliases.addAll(aliases);
        mIsValidated = true;
    }

    AzureActiveDirectoryCloud(final String preferredNetwork, final String preferredCache) {
        mPreferredNetworkHostName = preferredNetwork;
        mPreferredCacheHostName = preferredCache;
        mIsValidated = true;
    }

    String getPreferredNetworkHostName() {
        return mPreferredNetworkHostName;
    }

    String getPreferredCacheHostName() {
        return mPreferredCacheHostName;
    }

    List<String> getHostAliases() {
        return mCloudHostAliases;
    }

    //TODO: This is set to true if we were able to load the list of valid clouds from the server
    //TODO: Should we really be loading it at all if we were not able to find it?
    boolean isValidated() {
        return mIsValidated;
    }

}
