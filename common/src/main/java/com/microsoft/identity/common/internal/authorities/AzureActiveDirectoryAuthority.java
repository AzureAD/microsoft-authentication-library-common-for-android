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
package com.microsoft.identity.common.internal.authorities;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.WarningType;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.logging.Logger;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import lombok.Builder;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Accessors(prefix = "m")
public class AzureActiveDirectoryAuthority extends Authority {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AzureActiveDirectoryAuthority that = (AzureActiveDirectoryAuthority) o;

        if (mMultipleCloudsSupported != that.mMultipleCloudsSupported) return false;
        if (mAudience != null ? !mAudience.equals(that.mAudience) : that.mAudience != null)
            return false;
        if (mFlightParameters != null ? !mFlightParameters.equals(that.mFlightParameters) : that.mFlightParameters != null)
            return false;
        return mAzureActiveDirectoryCloud != null ? mAzureActiveDirectoryCloud.equals(that.mAzureActiveDirectoryCloud) : that.mAzureActiveDirectoryCloud == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (mAudience != null ? mAudience.hashCode() : 0);
        result = 31 * result + (mFlightParameters != null ? mFlightParameters.hashCode() : 0);
        result = 31 * result + (mMultipleCloudsSupported ? 1 : 0);
        result = 31 * result + (mAzureActiveDirectoryCloud != null ? mAzureActiveDirectoryCloud.hashCode() : 0);
        return result;
    }

    private static transient final String TAG = AzureActiveDirectoryAuthority.class.getSimpleName();

    @SerializedName("audience")
    public AzureActiveDirectoryAudience mAudience;

    @SerializedName("flight_parameters")
    public Map<String, String> mFlightParameters;

    @Builder.Default
    public boolean mMultipleCloudsSupported = false;

    private AzureActiveDirectoryCloud mAzureActiveDirectoryCloud;

    private void getAzureActiveDirectoryCloud() {
        final String methodName = ":getAzureActiveDirectoryCloud";
        AzureActiveDirectoryCloud cloud = null;

        try {
            cloud = AzureActiveDirectory.getAzureActiveDirectoryCloud(new URL(mAudience.getCloudUrl()));
            mKnownToMicrosoft = true;
        } catch (MalformedURLException e) {
            Logger.errorPII(
                    TAG + methodName,
                    "AAD cloud URL was malformed.",
                    e
            );
            cloud = null;
            mKnownToMicrosoft = false;
        }

        mAzureActiveDirectoryCloud = cloud;
    }

    public AzureActiveDirectoryAuthority(AzureActiveDirectoryAudience signInAudience) {
        mAudience = signInAudience;
        mAuthorityTypeString = "AAD";
        getAzureActiveDirectoryCloud();
    }

    public AzureActiveDirectoryAuthority() {
        //Defaulting to AllAccounts which maps to the "common" tenant
        mAudience = new AllAccounts();
        mAuthorityTypeString = "AAD";
        mMultipleCloudsSupported = false;
        getAzureActiveDirectoryCloud();
    }

    public Map<String, String> getFlightParameters() {
        return this.mFlightParameters;
    }

    public void setMultipleCloudsSupported(boolean supported) {
        mMultipleCloudsSupported = supported;
    }

    public boolean getMultipleCloudsSupported() {
        return mMultipleCloudsSupported;
    }

    @Override
    public Uri getAuthorityUri() {
        return Uri.parse(getAuthorityURI().toString());

    }

    public URI getAuthorityURI() {
        getAzureActiveDirectoryCloud();
        URI issuer;

        if (mAzureActiveDirectoryCloud == null) {
            issuer = URI.create(mAudience.getCloudUrl());
        } else {
            issuer = URI.create("https://" + mAzureActiveDirectoryCloud.getPreferredNetworkHostName());
        }

        try {
            final String tenantId = mAudience.getTenantId();
            return new URI(issuer.getScheme(), issuer.getAuthority(), issuer.getPath() + (tenantId != null ?
                    "/" + tenantId : ""),
                    issuer.getQuery(), issuer.getFragment());
        } catch (URISyntaxException e) {
            throw new RuntimeException();
        }
    }


    @Override
    public URL getAuthorityURL() {
        try {
            return new URL(this.getAuthorityURI().toString());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Authority URL is not a URL.", e);
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
            com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectorySlice slice =
                    new com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectorySlice();
            slice.setSlice(mSlice.getSlice());
            slice.setDataCenter(mSlice.getDC());
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

    public AzureActiveDirectoryAudience getAudience() {
        return mAudience;
    }

    @Override
    public String toString() {
        return "AzureActiveDirectoryAuthority{" +
                "mAudience=" + mAudience +
                ", mFlightParameters=" + mFlightParameters +
                ", mMultipleCloudsSupported=" + mMultipleCloudsSupported +
                ", mAzureActiveDirectoryCloud=" + mAzureActiveDirectoryCloud +
                ", super: { " + super.toString() +
                '}' +
                '}';
    }
}
