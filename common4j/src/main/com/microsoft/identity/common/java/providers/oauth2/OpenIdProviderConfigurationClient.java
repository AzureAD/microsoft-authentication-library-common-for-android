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
package com.microsoft.identity.common.java.providers.oauth2;

import static com.microsoft.identity.common.java.exception.ServiceException.OPENID_PROVIDER_CONFIGURATION_FAILED_TO_LOAD;

import com.google.gson.Gson;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.net.HttpClient;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.OTelUtility;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;
import com.microsoft.identity.common.java.util.StringUtil;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import lombok.NonNull;

/**
 * A class for pulling the OpenIdConfiguration document from the OpenID Provider server.
 */
public class OpenIdProviderConfigurationClient {

    private static final String TAG = OpenIdProviderConfigurationClient.class.getSimpleName();

    private static final String V2_0_PATH = "/v2.0";
    private static final String WELL_KNOWN_CONFIG_PATH = "/.well-known/openid-configuration";

    private static final Map<URI, OpenIdProviderConfiguration> sConfigCache = new HashMap<>();
    private static final HttpClient httpClient = UrlConnectionHttpClient.getDefaultInstance();

    private static final LongCounter sOpenIdProviderConfigurationIssuerValidationFailed = OTelUtility.createLongCounter(
            "openid_provider_configuration_issuer_validation_failed",
            "Track failure in validating OpenID Provider configuration issuer"
    );

    private static final Gson GSON = new Gson();

    /**
     * Get OpenID provider configuration.
     *
     * @return OpenIdProviderConfiguration
     */
    public synchronized OpenIdProviderConfiguration loadOpenIdProviderConfigurationFromAuthority(@NonNull final String authorityUrl)
            throws ServiceException {
        return loadOpenIdProviderConfigurationInternal(authorityUrl, null);
    }

    /**
     * Get OpenID provider configuration.
     *
     * @return OpenIdProviderConfiguration
     */
    public synchronized OpenIdProviderConfiguration loadOpenIdProviderConfigurationFromAuthorityWithExtraParams(@NonNull final String authorityUrl, @NonNull final String extraParams)
            throws ServiceException {
        return loadOpenIdProviderConfigurationInternal(authorityUrl, extraParams);
    }

    /**
     * Get OpenID provider configuration.
     *
     * @return OpenIdProviderConfiguration
     */
    private synchronized OpenIdProviderConfiguration loadOpenIdProviderConfigurationInternal(@NonNull final String tenantedAuthorityString, final String extraParams)
            throws ServiceException {
        final String methodName = ":loadOpenIdProviderConfigurationInternal";

        try {
            final String baseConfigUrlStr = getConfigRequestBaseUrl(tenantedAuthorityString);
            final String configUriStr;
            if (extraParams != null) {
                configUriStr = baseConfigUrlStr + WELL_KNOWN_CONFIG_PATH + extraParams;
            } else {
                configUriStr = baseConfigUrlStr + WELL_KNOWN_CONFIG_PATH;
            }
            final URI configUri = new URI(configUriStr);

            // Check first for a cached copy...
            final OpenIdProviderConfiguration cacheResult = sConfigCache.get(configUri);

            // If we found a result, return it...
            if (null != cacheResult) {
                Logger.info(
                        TAG + methodName,
                        "Using cached metadata result."
                );
                return cacheResult;
            }

            Logger.verbose(
                    TAG + methodName,
                    "Config URL is valid."
            );

            Logger.verbosePII(
                    TAG + methodName,
                    "Using request URL: " + configUri
            );

            final HttpResponse providerConfigResponse = httpClient.get(configUri.toURL(),
                    new HashMap<String, String>());

            final int statusCode = providerConfigResponse.getStatusCode();

            if (HttpURLConnection.HTTP_OK != statusCode
                    || StringUtil.isNullOrEmpty(providerConfigResponse.getBody())) {
                throw new ServiceException(
                        OPENID_PROVIDER_CONFIGURATION_FAILED_TO_LOAD,
                        "OpenId Provider Configuration metadata failed to load with status: "
                                + statusCode,
                        null
                );
            }

            final OpenIdProviderConfiguration parsedConfig = parseMetadata(
                    providerConfigResponse.getBody()
            );

            if (CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_OPENID_ISSUER_VALIDATION_REPORTING)) {
                validateIssuer(parsedConfig, baseConfigUrlStr);
            }
            // Cache our config in memory for later
            cacheConfiguration(configUri, parsedConfig);

            return parsedConfig;
        } catch (final ClientException | MalformedURLException | URISyntaxException e) {
            throw new ServiceException(
                    OPENID_PROVIDER_CONFIGURATION_FAILED_TO_LOAD,
                    "IOException while requesting metadata",
                    e
            );
        }
    }

    private void cacheConfiguration(@NonNull final URI configUrl,
                                    @NonNull final OpenIdProviderConfiguration parsedConfig) {
        sConfigCache.put(configUrl, parsedConfig);
    }

    private OpenIdProviderConfiguration parseMetadata(@NonNull final String body) {
        return GSON.fromJson(body, OpenIdProviderConfiguration.class);
    }

    /**
     * Validate the issuer from the metadata document against the request authority, which was used to
     * request the openid-configuration document.
     * @param config OpenID Provider configuration
     * @param requestAuthorityStr The authority URL string used to request the configuration document.
     */
    private void validateIssuer(
            @NonNull final OpenIdProviderConfiguration config,
            @NonNull final String requestAuthorityStr
    ) {
        final String methodTag = TAG + ":validateIssuer";

        final AttributesBuilder attributesBuilder = Attributes.builder();
        if (StringUtil.isNullOrEmpty(config.getIssuer())) {
            Logger.warn(methodTag, "Issuer is missing in the metadata.");
            attributesBuilder
                    .put(AttributeName.openid_issuer_invalid_reason.name(), "issuer_missing")
                    .build();
            sOpenIdProviderConfigurationIssuerValidationFailed.add(1, attributesBuilder.build());
            return;
        }

        // 1. exact match
        final String issuer = config.getIssuer();
        if (requestAuthorityStr.equals(issuer)) {
            return;
        }

        attributesBuilder.put(AttributeName.openid_config_request_authority.name(), requestAuthorityStr);
        attributesBuilder.put(AttributeName.openid_issuer.name(), issuer);
        final URL issuerUrl;
        try {
            issuerUrl = new URL(issuer);
        } catch (final MalformedURLException e) {
            Logger.warn(methodTag, "Issuer URL is malformed. " + e.getMessage());
            attributesBuilder
                    .put(AttributeName.openid_issuer_invalid_reason.name(), "issuer_malformed")
                    .build();
            sOpenIdProviderConfigurationIssuerValidationFailed.add(1, attributesBuilder.build());
            return;
        }

        // 2. Known Microsoft Cloud issuer validation
        try {
            AzureActiveDirectory.ensureCloudDiscoveryComplete();
            final URL requestAuthorityUrl = new URL(requestAuthorityStr);
            final AzureActiveDirectoryCloud requestCloud = AzureActiveDirectory.getAzureActiveDirectoryCloud(requestAuthorityUrl);
            if (requestCloud != null && requestCloud.isValidated()) {
                // request target is valid AAD cloud (public or sovereign)
                // if request url and issuer url belong to the same cloud, it's valid
                final AzureActiveDirectoryCloud issuerCloud = AzureActiveDirectory.getAzureActiveDirectoryCloud(issuerUrl);
                if (Objects.equals(requestCloud, issuerCloud)) {
                    return;
                }

                // Clouds don't match, but if request targets public AAD cloud, it's still valid
                if (AzureActiveDirectory.isPublicAzureActiveDirectoryCloud(requestAuthorityUrl)) {
                    return;
                }
                attributesBuilder
                        .put(AttributeName.openid_issuer_invalid_reason.name(), "issuer_aad_host_mismatch")
                        .build();
                sOpenIdProviderConfigurationIssuerValidationFailed.add(1, attributesBuilder.build());
            }
        } catch (final MalformedURLException e) {
            Logger.error(
                    methodTag,
                    "Issuer URL is malformed.",
                    e
            );
        } catch (ClientException e) {
            Logger.error(
                    methodTag,
                    "Failed to complete AAD cloud discovery.",
                    e
            );
        }

        // For other authorities (e.g. B2C, CIAM), we skip issuer validation
        // since we don't have a clear format of known valid issuers
        attributesBuilder
                .put(AttributeName.openid_issuer_invalid_reason.name(), "issuer_validation_skipped") // not necessarily invalid
                .build();
        sOpenIdProviderConfigurationIssuerValidationFailed.add(1, attributesBuilder.build());
    }

    private String getConfigRequestBaseUrl(@NonNull final String issuerUrl) {
        String sanitizedIssuer = issuerUrl.trim();

        if (issuerUrl.endsWith("/")) { // Remove any trailing slash
            sanitizedIssuer = issuerUrl.substring(0, sanitizedIssuer.length() - 1);
        }

        return sanitizedIssuer + V2_0_PATH;
    }
}
