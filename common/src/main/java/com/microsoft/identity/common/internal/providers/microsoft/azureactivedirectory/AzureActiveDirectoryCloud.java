package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains information about a specific Azure Active Directory Cloud.  Azure Active Directory
 * as a service is available in multiple clouds.  World wide is the default; however their are sovereign clouds
 * available for Germany, China, etc....
 */
public class AzureActiveDirectoryCloud {

    @SerializedName("preferred_network")
    private final String mPreferredNetworkHostName;

    @SerializedName("preferred_cache")
    private final String mPreferredCacheHostName;

    @SerializedName("aliases")
    private List<String> mCloudHostAliases;

    private boolean mIsValidated;

    AzureActiveDirectoryCloud(boolean isValidated) {
        mIsValidated = isValidated;

        mPreferredNetworkHostName = null;
        mPreferredCacheHostName = null;
    }

    public AzureActiveDirectoryCloud(final String preferredNetwork, final String preferredCache, final List<String> aliases) {
        mPreferredNetworkHostName = preferredNetwork;
        mPreferredCacheHostName = preferredCache;
        mCloudHostAliases = new ArrayList<>();
        mCloudHostAliases.addAll(aliases);
        mIsValidated = true;
    }

    AzureActiveDirectoryCloud(final String preferredNetwork, final String preferredCache) {
        mPreferredNetworkHostName = preferredNetwork;
        mPreferredCacheHostName = preferredCache;
        mIsValidated = true;
    }

    public String getPreferredNetworkHostName() {
        return mPreferredNetworkHostName;
    }

    public String getPreferredCacheHostName() {
        return mPreferredCacheHostName;
    }

    public List<String> getHostAliases() {
        return mCloudHostAliases;
    }

    //TODO: This is set to true if we were able to load the list of valid clouds from the server
    //TODO: Should we really be loading it at all if we were not able to find it?
    boolean isValidated() {
        return mIsValidated;
    }

    /**
     * Sets the validation status.
     *
     * @param isValidated The validation status to set.
     */
    void setIsValidated(final boolean isValidated) {
        mIsValidated = isValidated;
    }

}
