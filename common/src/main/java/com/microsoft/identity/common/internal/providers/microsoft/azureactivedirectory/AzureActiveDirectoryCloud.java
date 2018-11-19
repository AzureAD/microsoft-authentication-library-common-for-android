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

    public AzureActiveDirectoryCloud(boolean isValidated) {
        mIsValidated = isValidated;

        mPreferredNetworkHostName = null;
        mPreferredCacheHostName = null;
    }

    /**
     * Constructor of AzureActiveDirectoryCloud.
     *
     * @param preferredNetwork preferred network
     * @param preferredCache   preferred cache
     * @param aliases          aliases
     */
    public AzureActiveDirectoryCloud(final String preferredNetwork,
                                     final String preferredCache,
                                     final List<String> aliases) {
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

    /**
     * @return mPreferredNetworkHostName of the AzureActiveDirectoryCloud object
     */
    public String getPreferredNetworkHostName() {
        return mPreferredNetworkHostName;
    }

    /**
     * @return mPreferredCacheHostName of the AzureActiveDirectoryCloud object
     */
    public String getPreferredCacheHostName() {
        return mPreferredCacheHostName;
    }

    /**
     * @return mCloudHostAliases of the AzureActiveDirectoryCloud object
     */
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
