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

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.BuildConfig;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
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
import java.util.Locale;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;

public abstract class Authority {

    private static final String TAG = Authority.class.getSimpleName();

    private static final String ADFS_PATH_SEGMENT = "adfs";
    private static final String B2C_PATH_SEGMENT = "tfp";
    public static final String B2C = "B2C";
    public static final String CIAM = "CIAM";

    @SerializedName("default")
    protected boolean mIsDefault = false;

    @SerializedName("type")
    protected String mAuthorityTypeString;

    @SerializedName("authority_url")
    protected String mAuthorityUrlString;

    @SerializedName("slice")
    public AzureActiveDirectorySlice mSlice;

    public AzureActiveDirectorySlice getSlice() {
        return this.mSlice;
    }

    public void setSlice(AzureActiveDirectorySlice slice) {
        mSlice = slice;
    }

    public URI getAuthorityUri() {
        try {
            return new URI(mAuthorityUrlString);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Authority URL is not a URI.", e);
        }
    }

    public URL getAuthorityURL() {
        try {
            return getAuthorityUri().toURL();
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException("Authority URI is not a URL.", e);
        }
    }

    public boolean getDefault() {
        return mIsDefault;
    }

    public String getAuthorityTypeString() {
        return mAuthorityTypeString;
    }

    public void setDefault(Boolean isDefault) {
        mIsDefault = isDefault;
    }


    @SuppressFBWarnings(value="RpC_REPEATED_CONDITIONAL_TEST",
            justification="Somehow, spotbugs thinks that BuildConfig.SLICE and BuildConfig.DC are the same values.")
    public Authority() {
        // setting slice directly here in constructor if slice provided as command line param
        if (!StringUtil.isNullOrEmpty(BuildConfig.SLICE) || !StringUtil.isNullOrEmpty(BuildConfig.DC)) {
            final AzureActiveDirectorySlice slice = new AzureActiveDirectorySlice();
            slice.setSlice(BuildConfig.SLICE);
            slice.setDataCenter(BuildConfig.DC);
            mSlice = slice;
        }
    }

    /**
     * Returns an Authority based on an authority url.  This method attempts to parse the URL and based on the contents of it
     * determine the authority type and tenantid associated with it.
     *
     * @param authorityUrl
     * @return
     */
    public static Authority getAuthorityFromAuthorityUrl(String authorityUrl) {
        final String methodName = ":getAuthorityFromAuthorityUrl";
        final CommonURIBuilder authorityCommonUriBuilder;
        try {
            authorityCommonUriBuilder = new CommonURIBuilder(authorityUrl);
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException("Invalid authority URL");
        }

        final List<String> pathSegments = authorityCommonUriBuilder.getPathSegments();

        if (pathSegments.size() == 0) {
            if (authorityUrl.contains("ciamlogin.com")){
                // This is a CIAM authority, return CIAMTestAuthority
                return new CIAMAuthority(CIAMAuthority.getFullAuthorityUrlFromAuthorityWithoutPath(authorityUrl));
            }
            return new UnknownAuthority();
        }

        Authority authority = null; // Our result object...

        if (authorityIsKnownFromConfiguration(authorityUrl)) {
            final Authority configuredAuthority = getEquivalentConfiguredAuthority(authorityUrl);
            final String authorityTypeStr = configuredAuthority.mAuthorityTypeString;

            if (B2C.equalsIgnoreCase(authorityTypeStr)) {
                authority = new AzureActiveDirectoryB2CAuthority(authorityUrl);
            } if (CIAM.equalsIgnoreCase(authorityTypeStr)) {
                authority = new CIAMAuthority(authorityUrl);
            } else {
                authority = createAadAuthority(authorityCommonUriBuilder, pathSegments);
            }
        } else {
            String authorityType = pathSegments.get(0);

            switch (authorityType.toLowerCase(Locale.ROOT)) {
                case ADFS_PATH_SEGMENT:
                    //Return new Azure Active Directory Federation Services Authority
                    Logger.verbose(
                            TAG + methodName,
                            "Authority type is ADFS"
                    );
                    authority = new ActiveDirectoryFederationServicesAuthority(authorityUrl);
                    break;
                case B2C_PATH_SEGMENT:
                    //Return new B2C Authority
                    Logger.verbose(
                            TAG + methodName,
                            "Authority type is B2C"
                    );
                    authority = new AzureActiveDirectoryB2CAuthority(authorityUrl);
                    break;
                default:
                    Logger.verbose(
                            TAG + methodName,
                            "Authority type default: AAD"
                    );
                    authority = createAadAuthority(authorityCommonUriBuilder, pathSegments);
                    break;
            }
        }

        return authority;
    }

    @Nullable
    private static Authority getEquivalentConfiguredAuthority(@NonNull final String authorityStr) {
        Authority result = null;

        try {
            final URL authorityUrl = new URL(authorityStr);
            final String httpAuthority = authorityUrl.getAuthority();

            // Iterate over all of the developer trusted authorities and check if the authorities
            // are the same...
            for (final Authority currentAuthority : knownAuthorities) {
                if (!StringUtil.isNullOrEmpty(currentAuthority.mAuthorityUrlString)) {
                    final URL currentAuthorityUrl = new URL(currentAuthority.mAuthorityUrlString);
                    final String currentHttpAuthority = currentAuthorityUrl.getAuthority();

                    if (httpAuthority.equalsIgnoreCase(currentHttpAuthority)) {
                        result = currentAuthority;
                        break;
                    }
                }
            }
        } catch (MalformedURLException e) {
            // Shouldn't happen
            Logger.errorPII(
                    TAG,
                    "Error parsing authority",
                    e
            );
        }

        return result;
    }

    private static boolean authorityIsKnownFromConfiguration(@NonNull final String authorityStr) {
        return null != getEquivalentConfiguredAuthority(authorityStr);
    }

    private static Authority createAadAuthority(@NonNull final CommonURIBuilder authorityCommonUriBuilder,
                                                @NonNull final List<String> pathSegments) {
        AzureActiveDirectoryAudience audience = AzureActiveDirectoryAudience.getAzureActiveDirectoryAudience(
                authorityCommonUriBuilder.getScheme() + "://" + authorityCommonUriBuilder.getHost(),
                pathSegments.get(0)
        );

        return new AzureActiveDirectoryAuthority(audience);
    }

    // Suppressing rawtype warnings due to the generic type OAuth2Strategy
    @SuppressWarnings(WarningType.rawtype_warning)
    public abstract OAuth2Strategy createOAuth2Strategy(@NonNull final OAuth2StrategyParameters parameters) throws ClientException;

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Authority)) return false;

        Authority authority = (Authority) o;

        if (!mAuthorityTypeString.equals(authority.mAuthorityTypeString)) return false;
        return getAuthorityUri().equals(authority.getAuthorityUri());
    }
    //CHECKSTYLE:ON

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public int hashCode() {
        int result = mAuthorityTypeString.hashCode();
        result = 31 * result + getAuthorityUri().hashCode();
        return result;
    }
    //CHECKSTYLE:ON

    /**
     * These are authorities that the developer based on configuration of the public client application are known and trusted by the developer using the public client
     * application.  In order for the public client application to make a request to an authority.  That authority must be known by Microsoft or the developer
     * configuring the public client application.
     */
    private static final List<Authority> knownAuthorities = new ArrayList<>();
    private static final Object sLock = new Object();

    private static void performCloudDiscovery()
            throws IOException, URISyntaxException {
        final String methodName = ":performCloudDiscovery";
        Logger.info(
                TAG + methodName,
                "Performing cloud discovery..."
        );
        synchronized (sLock) {
            Logger.info(TAG + methodName, "Acquired lock.");
            if (!AzureActiveDirectory.isInitialized()) {
                Logger.info(TAG + methodName, "Not initialized. Starting request.");
                AzureActiveDirectory.performCloudDiscovery();
                Logger.info(TAG + methodName, "Loaded cloud metadata.");
            }
        }
    }

    public static void addKnownAuthorities(List<Authority> authorities) {
        synchronized (sLock) {
            knownAuthorities.addAll(authorities);
        }
    }

    /**
     * Authorities are either known by the developer and communicated to the library via configuration or they
     * are known to Microsoft based on the list of clouds returned from:
     *
     * @param authority Authority to check against.
     * @return True if the authority is known to Microsoft or defined in the configuration.
     */
    public static boolean isKnownAuthority(Authority authority) {
        final String methodName = ":isKnownAuthority";
        boolean knownToDeveloper = false;
        boolean knownToMicrosoft;

        if (authority == null) {
            Logger.warn(
                    TAG + methodName,
                    "Authority is null"
            );
            return false;
        }

        //Check if authority was added to configuration
        for (final Authority currentAuthority : knownAuthorities) {
            if (currentAuthority.mAuthorityUrlString != null &&
                    authority.getAuthorityURL() != null &&
                    authority.getAuthorityURL().getAuthority() != null &&
                    currentAuthority.mAuthorityUrlString.toLowerCase(Locale.ROOT).contains(
                            authority
                                    .getAuthorityURL()
                                    .getAuthority()
                                    .toLowerCase(Locale.ROOT))) {
                knownToDeveloper = true;
                break;
            }
        }

        // Check whether the authority is known to Microsoft or not.  Microsoft can recognize authorities that exist within public clouds.
        // Microsoft does not maintain a list of B2C authorities or a list of ADFS or 3rd party authorities (issuers).
        knownToMicrosoft = AzureActiveDirectory.hasCloudHost(authority.getAuthorityURL());

        final boolean isKnown = (knownToDeveloper || knownToMicrosoft);

        Logger.verbose(
                TAG + methodName,
                "Authority is known to developer? [" + knownToDeveloper + "]"
        );

        Logger.verbose(
                TAG + methodName,
                "Authority is known to Microsoft? [" + knownToMicrosoft + "]"
        );

        return isKnown;
    }

    public static KnownAuthorityResult getKnownAuthorityResult(Authority authority) {
        final String methodName = ":getKnownAuthorityResult";
        Logger.verbose(
                TAG + methodName,
                "Getting known authority result..."
        );
        ClientException clientException = null;
        boolean known = false;

        try {
            Logger.info(
                    TAG + methodName,
                    "Performing cloud discovery"
            );
            performCloudDiscovery();
        } catch (final IOException ex) {
            clientException = new ClientException(
                    ClientException.IO_ERROR,
                    "Unable to perform cloud discovery",
                    ex
            );
        } catch (final URISyntaxException ex) {
            clientException = new ClientException(
                    ClientException.MALFORMED_URL,
                    "Unable to construct cloud discovery URL",
                    ex
            );
        }

        Logger.info(TAG + methodName, "Cloud discovery complete.");

        if (clientException == null) {
            if (!isKnownAuthority(authority)) {
                clientException = new ClientException(
                        ClientException.UNKNOWN_AUTHORITY,
                        "Provided authority is not known.  MSAL will only make requests to known authorities"
                );
            } else {
                Logger.info(TAG + methodName, "Cloud is known.");
                known = true;
            }
        }

        return new KnownAuthorityResult(known, clientException);
    }

    public static class KnownAuthorityResult {
        private boolean mKnown;
        private ClientException mClientException;

        KnownAuthorityResult(boolean known, ClientException exception) {
            mKnown = known;
            mClientException = exception;
        }

        public boolean getKnown() {
            return mKnown;
        }

        public ClientException getClientException() {
            return mClientException;
        }
    }
}
