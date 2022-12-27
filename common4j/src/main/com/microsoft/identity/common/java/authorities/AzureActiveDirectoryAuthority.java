//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.authorities;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectorySlice;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.CommonURIBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Accessors(prefix = "m")
public class AzureActiveDirectoryAuthority extends Authority {

    private static transient final String TAG = AzureActiveDirectoryAuthority.class.getSimpleName();

    @Getter
    @SerializedName("audience")
    public AzureActiveDirectoryAudience mAudience;

    @SerializedName("flight_parameters")
    public Map<String, String> mFlightParameters;

    @Getter
    @Setter
    private boolean mMultipleCloudsSupported = false;

    /** Gets {@link AzureActiveDirectoryCloud}, if the cloud metadata is already initialized. */
    @Nullable
    private static synchronized AzureActiveDirectoryCloud getAzureActiveDirectoryCloud(
            @NonNull final AzureActiveDirectoryAudience audience) {
        final String methodName = ":getAzureActiveDirectoryCloud";

        try {
            return AzureActiveDirectory.getAzureActiveDirectoryCloud(new URL(audience.getCloudUrl()));
        } catch (MalformedURLException e) {
            Logger.errorPII(
                    TAG + methodName,
                    "AAD cloud URL was malformed.",
                    e
            );
        }

        return null;
    }

    public AzureActiveDirectoryAuthority(AzureActiveDirectoryAudience signInAudience) {
        mAudience = signInAudience;
        mAuthorityTypeString = "AAD";
    }

    public AzureActiveDirectoryAuthority() {
        //Defaulting to AllAccounts which maps to the "common" tenant
        mAudience = new AllAccounts();
        mAuthorityTypeString = "AAD";
        mMultipleCloudsSupported = false;
    }

    @Override
    public URI getAuthorityUri() {
        try {
            final AzureActiveDirectoryCloud cloud = getAzureActiveDirectoryCloud(mAudience);
            CommonURIBuilder issuer;

            if (cloud == null) {
                issuer = new CommonURIBuilder(mAudience.getCloudUrl());
            } else {
                issuer = new CommonURIBuilder("https://" + cloud.getPreferredNetworkHostName());
            }

            if (!StringUtil.isNullOrEmpty(mAudience.getTenantId())) {
                final List<String> pathSegments = new ArrayList<>(issuer.getPathSegments());
                pathSegments.add(mAudience.getTenantId());
                issuer.setPathSegments(pathSegments);
            }

            return issuer.build();
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException("Authority URI is invalid.", e);
        }
    }

    protected MicrosoftStsOAuth2Configuration createOAuth2Configuration() {
        final String methodName = ":createOAuth2Configuration";
        Logger.verbose(
                TAG + methodName,
                "Creating OAuth2Configuration"
        );
        MicrosoftStsOAuth2Configuration config = new MicrosoftStsOAuth2Configuration();
        config.setAuthorityUrl(this.getAuthorityURL());

        if (mSlice != null) {
            Logger.info(
                    TAG + methodName,
                    "Setting slice parameters..."
            );
            final AzureActiveDirectorySlice slice = new AzureActiveDirectorySlice();
            slice.setSlice(mSlice.getSlice());
            slice.setDataCenter(mSlice.getDataCenter());
            config.setSlice(slice);
        }

        if (mFlightParameters != null) {
            Logger.info(
                    TAG + methodName,
                    "Setting flight parameters..."
            );
            //GSON Returns a LinkedTreeMap which implement AbstractMap....
            for (Map.Entry<String, String> entry : mFlightParameters.entrySet()) {
                config.getFlightParameters().put(entry.getKey(), entry.getValue());
            }
        }


        config.setMultipleCloudsSupported(mMultipleCloudsSupported);
        return config;
    }

    // Suppressing rawtype warnings due to the generic type OAuth2Strategy
    @SuppressWarnings(WarningType.rawtype_warning)
    @Override
    public OAuth2Strategy createOAuth2Strategy(@NonNull final OAuth2StrategyParameters parameters) throws ClientException {
        MicrosoftStsOAuth2Configuration config = createOAuth2Configuration();
        return new MicrosoftStsOAuth2Strategy(config, parameters);
    }

    /**
     * Checks if current authority belongs to same cloud as the passed in authority.
     *
     * @param authorityToCheck authority to check against.
     * @return true if both authorities belong to same cloud, otherwise false.
     */
    //@WorkerThread
    public synchronized boolean isSameCloudAsAuthority(@NonNull final AzureActiveDirectoryAuthority authorityToCheck)
            throws IOException, URISyntaxException {
        if (!AzureActiveDirectory.isInitialized()) {
            // Cloud discovery is needed in order to make sure that we have a preferred_network_host_name to cloud aliases mappings
            AzureActiveDirectory.performCloudDiscovery();
        }

        final AzureActiveDirectoryCloud cloudOfThisAuthority = getAzureActiveDirectoryCloud(mAudience);
        final AzureActiveDirectoryCloud cloudOfAuthorityToCheck = getAzureActiveDirectoryCloud(authorityToCheck.getAudience());

        // Can't verify, return false.
        if (cloudOfThisAuthority == null && cloudOfAuthorityToCheck == null) {
            return false;
        }

        return Objects.equals(cloudOfThisAuthority, cloudOfAuthorityToCheck);
    }
}
