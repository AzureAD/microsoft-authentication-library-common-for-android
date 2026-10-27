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
package com.microsoft.identity.common.java.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.components.MockPlatformComponentsFactory;
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.authorities.Environment;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsProvider;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.net.HttpUrlConnectionFactory;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAuthorizationResponse;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.util.ObjectMapper;

import com.microsoft.identity.http.MockConnection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.net.ssl.HttpsURLConnection;

import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * Tests for {@link MicrosoftStsOAuth2Strategy}
 */
@RunWith(JUnit4.class)
public class MicrosoftStsOAuth2StrategyTest {
    private static final String ORIGINAL_TOKEN_ENDPOINT =
            "https://login.microsoftonline.com/common/oauth2/v2.0/token";
    private static final String UNRECOGNIZED_CLOUD_HOST = "login.example.test";
    private static final String VALIDATED_CLOUD_HOST = "login.microsoftonline.us";

    @Before
    public void setUp() {
        CommonFlightsManager.INSTANCE.resetFlightsManager();
        AzureActiveDirectory.setEnvironment(Environment.PreProduction);
        AzureActiveDirectory.setEnvironment(Environment.Production);
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
        registerValidatedCloud("login.microsoftonline.com");
        registerValidatedCloud(VALIDATED_CLOUD_HOST);
    }

    @After
    public void tearDown() {
        CommonFlightsManager.INSTANCE.resetFlightsManager();
        AzureActiveDirectory.setEnvironment(Environment.PreProduction);
        AzureActiveDirectory.setEnvironment(Environment.Production);
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
    }

    private static final String MOCK_TOKEN_SUCCESS_RESPONSE = "{\n" +
            "\t\"token_type\": \"Bearer\",\n" +
            "\t\"scope\": \"mock_scope_1\",\n" +
            "\t\"expires_in\": 3599,\n" +
            "\t\"ext_expires_in\": 3599,\n" +
            "\t\"access_token\": \"b06d0810-12ff-4a4e-850b-4bda1540d895\",\n" +
            "\t\"refresh_token\": \"6b80f5b5-d53c-4c46-992d-66c5dcd4cfb1\",\n" +
            "\t\"id_token\": \"95608142-3a7a-4643-a543-6db44e403e97\",\n" +
            "\t\"client_info\": \"2245f73e-287a-41c4-ba87-560809ad06b9\"\n" +
            "}";

    @SneakyThrows
    @Test
    public void testGetTokenResultFromHttpResponse() {
        final IPlatformComponents mockPlatformComponents = MockPlatformComponentsFactory.getNonFunctionalBuilder().build();
        final MicrosoftStsOAuth2Configuration mockConfig = new MicrosoftStsOAuth2Configuration();
        mockConfig.setAuthorityUrl(new URL("https://login.microsoftonline.com/common"));
        final OAuth2StrategyParameters parameters = OAuth2StrategyParameters.builder()
                .platformComponents(mockPlatformComponents)
                .usingOpenIdConfiguration(false)
                .build();
        final MicrosoftStsOAuth2Strategy microsoftStsOAuth2Strategy = new MicrosoftStsOAuth2Strategy(mockConfig, parameters);
        final HttpResponse mockHttpResponse = new HttpResponse(200, MOCK_TOKEN_SUCCESS_RESPONSE, null);
        final TokenResult tokenResult = microsoftStsOAuth2Strategy.getTokenResultFromHttpResponse(mockHttpResponse);
        Assert.assertNotNull(tokenResult);
        Assert.assertNotNull(tokenResult.getSuccessResponse());
        Assert.assertTrue(tokenResult.getSuccess());
    }

    @SneakyThrows
    @Test
    public void testRequestToken_WithResponseHandler() {
        final IPlatformComponents mockPlatformComponents = MockPlatformComponentsFactory.getNonFunctionalBuilder().build();
        final MicrosoftStsOAuth2Configuration mockConfig = new MicrosoftStsOAuth2Configuration();
        mockConfig.setAuthorityUrl(new URL("https://login.microsoftonline.com/common"));
        final OAuth2StrategyParameters parameters = OAuth2StrategyParameters.builder()
                .platformComponents(mockPlatformComponents)
                .usingOpenIdConfiguration(false)
                .build();

        final HttpResponse mockHttpResponse = new HttpResponse(200, "response_body", null);
        final MicrosoftStsOAuth2Strategy microsoftStsOAuth2Strategy = new MicrosoftStsOAuth2Strategy(mockConfig, parameters) {
            @Override
            public HttpResponse performTokenRequest(@NonNull MicrosoftStsTokenRequest requestContext) {
                return mockHttpResponse;
            }
        };
        final AbstractMicrosoftStsTokenResponseHandler mockTokenResponseHandler = new AbstractMicrosoftStsTokenResponseHandler() {
            @Override
            protected MicrosoftStsTokenResponse getSuccessfulResponse(@NonNull HttpResponse httpResponse) {
                return ObjectMapper.deserializeJsonStringToObject(MOCK_TOKEN_SUCCESS_RESPONSE, MicrosoftStsTokenResponse.class);
            }
        };
        final MicrosoftStsTokenRequest mockTokenRequest = new MicrosoftStsTokenRequest();
        final TokenResult tokenResult = microsoftStsOAuth2Strategy.requestToken(mockTokenRequest, mockTokenResponseHandler);
        Assert.assertNotNull(tokenResult);
        Assert.assertNotNull(tokenResult.getSuccessResponse());
        Assert.assertTrue(tokenResult.getSuccess());
        Assert.assertNotNull(tokenResult.getSuccessResponse().getAuthority());
    }


    @SneakyThrows
    @Test
    public void testRequestToken() {
        final IPlatformComponents mockPlatformComponents = MockPlatformComponentsFactory.getNonFunctionalBuilder().build();
        final MicrosoftStsOAuth2Configuration mockConfig = new MicrosoftStsOAuth2Configuration();
        mockConfig.setAuthorityUrl(new URL("https://login.microsoftonline.com/common"));
        final OAuth2StrategyParameters parameters = OAuth2StrategyParameters.builder()
                .platformComponents(mockPlatformComponents)
                .usingOpenIdConfiguration(false)
                .build();

        final HttpResponse mockHttpResponse = new HttpResponse(200, MOCK_TOKEN_SUCCESS_RESPONSE, null);
        final MicrosoftStsOAuth2Strategy microsoftStsOAuth2Strategy = new MicrosoftStsOAuth2Strategy(mockConfig, parameters) {
            @Override
            public HttpResponse performTokenRequest(@NonNull MicrosoftStsTokenRequest requestContext) {
                return mockHttpResponse;
            }
        };

        final MicrosoftStsTokenRequest mockTokenRequest = new MicrosoftStsTokenRequest();
        final TokenResult tokenResult = microsoftStsOAuth2Strategy.requestToken(mockTokenRequest);
        Assert.assertNotNull(tokenResult);
        Assert.assertNotNull(tokenResult.getSuccessResponse());
        Assert.assertTrue(tokenResult.getSuccess());
        Assert.assertNotNull(tokenResult.getSuccessResponse().getAuthority());
    }

    @Test
    @SneakyThrows
    public void testCloudEndpoint_flightOff_unrecognizedHostPreservesLegacyBehavior() {
        assertCloudEndpointAccepted(false, UNRECOGNIZED_CLOUD_HOST);
    }

    @Test
    @SneakyThrows
    public void testCloudEndpoint_flightOff_validatedHostWorks() {
        assertCloudEndpointAccepted(false, VALIDATED_CLOUD_HOST);
    }

    @Test
    @SneakyThrows
    public void testCloudEndpoint_flightOn_unrecognizedHostRejected() {
        setCloudEndpointValidationEnabled(true);
        final TestMicrosoftStsOAuth2Strategy strategy = createStrategy(true);
        assertCloudEndpointRejected(strategy, createAuthorizationRequest(false),
                UNRECOGNIZED_CLOUD_HOST);
    }

    @Test
    @SneakyThrows
    public void testCloudEndpoint_flightOn_validatedHostWorks() {
        assertCloudEndpointAccepted(true, VALIDATED_CLOUD_HOST);
    }

    @Test
    @SneakyThrows
    public void testCloudEndpoint_flightOn_unvalidatedCachedHostRejected() {
        setCloudEndpointValidationEnabled(true);
        AzureActiveDirectory.putCloud(UNRECOGNIZED_CLOUD_HOST,
                new AzureActiveDirectoryCloud(false));
        assertCloudEndpointRejected(createStrategy(true), createAuthorizationRequest(false),
                UNRECOGNIZED_CLOUD_HOST);
    }

    @Test
    @SneakyThrows
    public void testCloudEndpoint_flightOn_validatedAliasAndCaseAccepted() {
        final String alias = "alias.example.test";
        AzureActiveDirectory.putCloud(alias, new AzureActiveDirectoryCloud(
                VALIDATED_CLOUD_HOST, VALIDATED_CLOUD_HOST, Collections.singletonList(alias)));
        assertCloudEndpointAccepted(true, "ALIAS.EXAMPLE.TEST");
    }

    @Test
    @SneakyThrows
    public void testCloudEndpoint_flightOn_preseededSovereignCloudAccepted() {
        assertCloudEndpointAccepted(true, AzureActiveDirectoryCloud.BLEU_CLOUD_HOST);
    }

    @Test
    @SneakyThrows
    public void testCloudEndpoint_flightOn_requestMultipleCloudAwareAlsoValidated() {
        setCloudEndpointValidationEnabled(true);
        assertCloudEndpointRejected(createStrategy(false), createAuthorizationRequest(true),
                UNRECOGNIZED_CLOUD_HOST);
    }

    @Test
    @SneakyThrows
    public void testCloudEndpoint_flightOn_routingDisabledIgnoresHost() {
        setCloudEndpointValidationEnabled(true);
        final TestMicrosoftStsOAuth2Strategy strategy = createStrategy(false);
        strategy.createTokenRequest(createAuthorizationRequest(false),
                createAuthorizationResponse(UNRECOGNIZED_CLOUD_HOST),
                new BearerAuthenticationSchemeInternal());
        Assert.assertEquals(ORIGINAL_TOKEN_ENDPOINT, strategy.getTokenEndpoint());
    }

    @Test
    @SneakyThrows
    public void testCloudEndpoint_flightOn_missingOrEmptyHostPreservesEndpoint() {
        setCloudEndpointValidationEnabled(true);
        final TestMicrosoftStsOAuth2Strategy strategy = createStrategy(true);
        for (final String host : new String[]{null, ""}) {
            strategy.createTokenRequest(createAuthorizationRequest(false),
                    createAuthorizationResponse(host), new BearerAuthenticationSchemeInternal());
            Assert.assertEquals(ORIGINAL_TOKEN_ENDPOINT, strategy.getTokenEndpoint());
        }
    }

    @Test
    @SneakyThrows
    public void testCloudEndpoint_flightOn_discoveryLoadsValidatedHost() {
        setCloudEndpointValidationEnabled(true);
        resetCloudMetadata();
        final HttpsURLConnection discovery = enqueueCloudDiscoveryResponse();
        final TestMicrosoftStsOAuth2Strategy strategy = createStrategy(true);
        strategy.createTokenRequest(createAuthorizationRequest(false),
                createAuthorizationResponse(VALIDATED_CLOUD_HOST),
                new BearerAuthenticationSchemeInternal());
        Assert.assertEquals(ORIGINAL_TOKEN_ENDPOINT.replace("login.microsoftonline.com",
                VALIDATED_CLOUD_HOST), strategy.getTokenEndpoint());
        Assert.assertEquals(0, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
        Mockito.verify(discovery).setRequestMethod("GET");
        Mockito.verify(discovery, Mockito.never()).getOutputStream();
    }

    @Test
    @SneakyThrows
    public void testCloudEndpoint_flightOn_discoveryDoesNotAuthorizeUnknownHost() {
        setCloudEndpointValidationEnabled(true);
        resetCloudMetadata();
        final HttpsURLConnection discovery = enqueueCloudDiscoveryResponse();
        assertCloudEndpointRejected(createStrategy(true), createAuthorizationRequest(false),
                UNRECOGNIZED_CLOUD_HOST);
        Assert.assertEquals(0, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
        Mockito.verify(discovery).setRequestMethod("GET");
        Mockito.verify(discovery, Mockito.never()).getOutputStream();
    }

    @Test
    @SneakyThrows
    public void testCloudEndpoint_flightOn_failedDiscoveryRejectsSelection() {
        setCloudEndpointValidationEnabled(true);
        resetCloudMetadata();
        final HttpsURLConnection discovery =
                MockConnection.getMockedConnectionWithFailureResponse(400);
        HttpUrlConnectionFactory.addMockedConnection(discovery);
        assertCloudEndpointRejected(createStrategy(true), createAuthorizationRequest(false),
                VALIDATED_CLOUD_HOST);
        Assert.assertEquals(0, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
        Mockito.verify(discovery, Mockito.never()).getOutputStream();
    }

    @Test
    @SneakyThrows
    public void testCloudEndpoint_flightOn_discoveryExceptionStopsSelection() {
        setCloudEndpointValidationEnabled(true);
        resetCloudMetadata();
        final HttpURLConnection discovery =
                MockConnection.getMockedConnectionWithSocketTimeout();
        HttpUrlConnectionFactory.addMockedConnection(discovery);
        HttpUrlConnectionFactory.addMockedConnection(discovery);
        final TestMicrosoftStsOAuth2Strategy strategy = createStrategy(true);
        final ClientException exception = Assert.assertThrows(ClientException.class,
                () -> strategy.createTokenRequest(createAuthorizationRequest(false),
                        createAuthorizationResponse(VALIDATED_CLOUD_HOST),
                        new BearerAuthenticationSchemeInternal()));
        Assert.assertNotEquals(ClientException.UNKNOWN_AUTHORITY, exception.getErrorCode());
        Assert.assertNotNull(exception.getCause());
        Assert.assertEquals(ORIGINAL_TOKEN_ENDPOINT, strategy.getTokenEndpoint());
        Assert.assertEquals(0, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
        Mockito.verify(discovery, Mockito.times(2)).getInputStream();
    }

    @Test
    @SneakyThrows
    public void testCloudEndpoint_flightOff_doesNotPerformDiscovery() {
        setCloudEndpointValidationEnabled(false);
        resetCloudMetadata();
        final HttpsURLConnection discovery = enqueueCloudDiscoveryResponse();
        final TestMicrosoftStsOAuth2Strategy strategy = createStrategy(true);
        strategy.createTokenRequest(createAuthorizationRequest(false),
                createAuthorizationResponse(UNRECOGNIZED_CLOUD_HOST),
                new BearerAuthenticationSchemeInternal());
        Assert.assertEquals(1, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
        Mockito.verifyNoInteractions(discovery);
    }

    private void assertCloudEndpointAccepted(final boolean validationEnabled,
                                             final String host) throws Exception {
        setCloudEndpointValidationEnabled(validationEnabled);
        final TestMicrosoftStsOAuth2Strategy strategy = createStrategy(true);
        final String endpoint = ORIGINAL_TOKEN_ENDPOINT + "?existing=value";
        strategy.useTokenEndpoint(endpoint);
        final MicrosoftStsAuthorizationRequest request = createAuthorizationRequest(false);
        final MicrosoftStsTokenRequest tokenRequest = strategy.createTokenRequest(request,
                createAuthorizationResponse(host), new BearerAuthenticationSchemeInternal());
        Assert.assertEquals(endpoint.replace("login.microsoftonline.com", host),
                strategy.getTokenEndpoint());
        Assert.assertEquals("test-code", tokenRequest.getCode());
        Assert.assertEquals(request.getPkceCodeVerifier(), tokenRequest.getCodeVerifier());
        Assert.assertEquals(request.getClientId(), tokenRequest.getClientId());
        Assert.assertEquals(request.getRedirectUri(), tokenRequest.getRedirectUri());
    }

    private void assertCloudEndpointRejected(final TestMicrosoftStsOAuth2Strategy strategy,
                                             final MicrosoftStsAuthorizationRequest request,
                                             final String host) {
        final ClientException exception = Assert.assertThrows(ClientException.class,
                () -> strategy.createTokenRequest(request, createAuthorizationResponse(host),
                        new BearerAuthenticationSchemeInternal()));
        Assert.assertEquals(ClientException.UNKNOWN_AUTHORITY, exception.getErrorCode());
        Assert.assertEquals(ORIGINAL_TOKEN_ENDPOINT, strategy.getTokenEndpoint());
    }

    private TestMicrosoftStsOAuth2Strategy createStrategy(final boolean multipleCloudsSupported)
            throws Exception {
        final MicrosoftStsOAuth2Configuration config = new MicrosoftStsOAuth2Configuration();
        config.setAuthorityUrl(new URL("https://login.microsoftonline.com/common"));
        config.setMultipleCloudsSupported(multipleCloudsSupported);
        final OAuth2StrategyParameters parameters = OAuth2StrategyParameters.builder()
                .platformComponents(MockPlatformComponentsFactory.getNonFunctionalBuilder().build())
                .usingOpenIdConfiguration(false)
                .build();
        final TestMicrosoftStsOAuth2Strategy strategy =
                new TestMicrosoftStsOAuth2Strategy(config, parameters);
        strategy.useTokenEndpoint(ORIGINAL_TOKEN_ENDPOINT);
        return strategy;
    }

    private MicrosoftStsAuthorizationRequest createAuthorizationRequest(
            final boolean multipleCloudAware) throws Exception {
        return new MicrosoftStsAuthorizationRequest.Builder()
                .setAuthority(new URL("https://login.microsoftonline.com/common"))
                .setClientId("test-client")
                .setRedirectUri("msauth://com.example.app/callback")
                .setMultipleCloudAware(multipleCloudAware)
                .build();
    }

    private MicrosoftStsAuthorizationResponse createAuthorizationResponse(final String host) {
        return new MicrosoftStsAuthorizationResponse("test-code", "test-state",
                host == null ? Collections.emptyMap() : Collections.singletonMap(
                        MicrosoftAuthorizationResponse.CLOUD_INSTANCE_HOST_NAME, host));
    }

    private void setCloudEndpointValidationEnabled(final boolean enabled) {
        final MockFlightsProvider provider = new MockFlightsProvider();
        provider.addFlight(CommonFlight.VALIDATE_CLOUD_TOKEN_ENDPOINT.getKey(),
                Boolean.toString(enabled));
        final MockFlightsManager manager = new MockFlightsManager();
        manager.setMockBrokerFlightsProvider(provider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(manager);
    }

    private void registerValidatedCloud(final String host) {
        AzureActiveDirectory.putCloud(host, new AzureActiveDirectoryCloud(
                host, host, Collections.singletonList(host)));
    }

    private void resetCloudMetadata() {
        AzureActiveDirectory.setEnvironment(Environment.PreProduction);
        AzureActiveDirectory.setEnvironment(Environment.Production);
    }

    private HttpsURLConnection enqueueCloudDiscoveryResponse() throws Exception {
        final HttpsURLConnection connection = MockConnection.getCommonHttpUrlConnection();
        final String metadata = "{\"metadata\":[{\"preferred_network\":\""
                + VALIDATED_CLOUD_HOST + "\",\"preferred_cache\":\"" + VALIDATED_CLOUD_HOST
                + "\",\"aliases\":[\"" + VALIDATED_CLOUD_HOST + "\"]}]}";
        Mockito.when(connection.getResponseCode()).thenReturn(200);
        Mockito.when(connection.getInputStream()).thenReturn(
                new ByteArrayInputStream(metadata.getBytes(StandardCharsets.UTF_8)));
        HttpUrlConnectionFactory.addMockedConnection(connection);
        return connection;
    }

    private static final class TestMicrosoftStsOAuth2Strategy extends MicrosoftStsOAuth2Strategy {
        private TestMicrosoftStsOAuth2Strategy(final MicrosoftStsOAuth2Configuration config,
                                              final OAuth2StrategyParameters parameters)
                throws ClientException {
            super(config, parameters);
        }

        @Override
        public String getTokenEndpoint() {
            return super.getTokenEndpoint();
        }

        private void useTokenEndpoint(final String endpoint) throws ClientException {
            setTokenEndpoint(endpoint);
        }
    }
}
