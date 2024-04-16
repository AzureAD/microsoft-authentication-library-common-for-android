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

import static com.microsoft.identity.common.java.authorities.AllAccounts.ALL_ACCOUNTS_TENANT_ID;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration;
import com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfigurationClient;
import com.microsoft.identity.common.java.util.CommonURIBuilder;
import com.microsoft.identity.common.java.util.StringUtil;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import lombok.NonNull;

public abstract class AzureActiveDirectoryAudience {

    private static final String TAG = AzureActiveDirectoryAudience.class.getSimpleName();

    private String mCloudUrl;
    @SerializedName("tenant_id")
    private String mTenantId;

    public static final String ORGANIZATIONS = "organizations";
    public static final String CONSUMERS = "consumers";
    public static final String ALL = "common";
    public static final String MSA_MEGA_TENANT_ID = "9188040d-6c67-4c5b-b112-36a304b66dad";

    public String getCloudUrl() {
        if (mCloudUrl == null) {
            return AzureActiveDirectory.getDefaultCloudUrl();
        } else {
            return mCloudUrl;
        }
    }

    public void setCloudUrl(String cloudUrl) {
        mCloudUrl = cloudUrl;
    }

    public String getTenantId() {
        return mTenantId;
    }


    /**
     * Must be called on a worker thread.
     * <p>
     * Method which queries the {@link OpenIdProviderConfiguration}
     * to get tenant UUID for the authority with tenant alias.
     */
    //@WorkerThread
    public String getTenantUuidForAlias(@NonNull final String authority)
            throws ServiceException, ClientException {
        // if the tenant id is already a UUID, return
        if (StringUtil.isUuid(mTenantId)) {
            return mTenantId;
        }

        final OpenIdProviderConfiguration providerConfiguration =
                loadOpenIdProviderConfigurationMetadata(authority);

        final String issuer = providerConfiguration.getIssuer();
        final CommonURIBuilder issuerUri;
        try {
            issuerUri = new CommonURIBuilder(issuer);
        } catch (final URISyntaxException e) {
            throw new ClientException(ClientException.MALFORMED_URL,
                    "Failed to construct issuerUri", e);
        }
        final List<String> paths = issuerUri.getPathSegments();

        if (paths.isEmpty()) {
            final String errMsg = "OpenId Metadata did not contain a path to the tenant";

            Logger.error(
                    TAG,
                    errMsg,
                    null
            );

            throw new ClientException(errMsg);
        }
        final String tenantUUID = paths.get(0);

        if (!StringUtil.isUuid(tenantUUID)) {
            final String errMsg = "OpenId Metadata did not contain UUID in the path ";
            Logger.error(
                    TAG,
                    errMsg,
                    null
            );

            throw new ClientException(errMsg);
        }
        return tenantUUID;
    }

    /**
     * Util method which returns true if the tenant alias is "common" ,
     * "organizations" or "consumers" indicating that it's the user's home tenant
     *
     * @param tenantId
     * @return
     */
    public static boolean isHomeTenantAlias(@NonNull final String tenantId) {
        return tenantId.equalsIgnoreCase(ALL_ACCOUNTS_TENANT_ID)
                || tenantId.equalsIgnoreCase(AnyPersonalAccount.ANY_PERSONAL_ACCOUNT_TENANT_ID)
                || tenantId.equalsIgnoreCase(ORGANIZATIONS);
    }

    private static OpenIdProviderConfiguration loadOpenIdProviderConfigurationMetadata(
            @NonNull final String requestAuthority) throws ServiceException, ClientException {
        final String methodName = ":loadOpenIdProviderConfigurationMetadata";

        Logger.info(
                TAG + methodName,
                "Loading OpenId Provider Metadata..."
        );

        final OpenIdProviderConfigurationClient client =
                new OpenIdProviderConfigurationClient();
        return client.loadOpenIdProviderConfigurationFromAuthority(requestAuthority);
    }

    public void setTenantId(String tenantId) {
        mTenantId = tenantId;
    }

    public static AzureActiveDirectoryAudience getAzureActiveDirectoryAudience(final String cloudUrl,
                                                                               final String tenantId) {
        final String methodName = ":getAzureActiveDirectoryAudience";
        AzureActiveDirectoryAudience audience = null;

        switch (tenantId.toLowerCase(Locale.ROOT)) {
            case ORGANIZATIONS:
                Logger.verbose(
                        TAG + methodName,
                        "Audience: AnyOrganizationalAccount"
                );
                audience = new AnyOrganizationalAccount(cloudUrl);
                break;
            case CONSUMERS:
                Logger.verbose(
                        TAG + methodName,
                        "Audience: AnyPersonalAccount"
                );
                audience = new AnyPersonalAccount(cloudUrl);
                break;
            case ALL:
                Logger.verbose(
                        TAG + methodName,
                        "Audience: AllAccounts"
                );
                audience = new AllAccounts(cloudUrl);
                break;
            default:
                Logger.verbose(
                        TAG + methodName,
                        "Audience: AccountsInOneOrganization"
                );
                audience = new AccountsInOneOrganization(cloudUrl, tenantId);
        }

        return audience;
    }

}
