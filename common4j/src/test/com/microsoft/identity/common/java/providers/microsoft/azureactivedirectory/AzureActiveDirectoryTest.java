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
package com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory;

import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authorities.Environment;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;

/**
 * Tests for sovereign cloud discovery support in {@link AzureActiveDirectory}.
 * Focuses on pre-seeding, isSovCloudDiscoveryHost, cache-based discovery gating,
 * and environment changes.
 */
public class AzureActiveDirectoryTest {

    @Before
    public void setup() {
        // Reset to production environment before each test.
        AzureActiveDirectory.setEnvironment(Environment.Production);
    }

    @After
    public void tearDown() {
        // Ensure environment is restored.
        AzureActiveDirectory.setEnvironment(Environment.Production);
    }

    // --- isSovCloudDiscoveryHost tests ---

    @Test
    public void testIsSovCloudDiscoveryHost_bleu() {
        Assert.assertTrue(AzureActiveDirectory.isSovCloudDiscoveryHost("login.sovcloud-identity.fr"));
    }

    @Test
    public void testIsSovCloudDiscoveryHost_delos() {
        Assert.assertTrue(AzureActiveDirectory.isSovCloudDiscoveryHost("login.sovcloud-identity.de"));
    }

    @Test
    public void testIsSovCloudDiscoveryHost_sovsg() {
        Assert.assertTrue(AzureActiveDirectory.isSovCloudDiscoveryHost("login.sovcloud-identity.sg"));
    }

    @Test
    public void testIsSovCloudDiscoveryHost_caseInsensitive() {
        Assert.assertTrue(AzureActiveDirectory.isSovCloudDiscoveryHost("LOGIN.SOVCLOUD-IDENTITY.FR"));
        Assert.assertTrue(AzureActiveDirectory.isSovCloudDiscoveryHost("Login.SovCloud-Identity.De"));
    }

    @Test
    public void testIsSovCloudDiscoveryHost_nonSovCloud() {
        Assert.assertFalse(AzureActiveDirectory.isSovCloudDiscoveryHost("login.microsoftonline.com"));
        Assert.assertFalse(AzureActiveDirectory.isSovCloudDiscoveryHost("login.chinacloudapi.cn"));
        Assert.assertFalse(AzureActiveDirectory.isSovCloudDiscoveryHost("example.com"));
    }

    // --- Pre-seeding tests ---

    @Test
    public void testSovereignCloudsPreSeededInCache() throws Exception {
        // The static init block should have pre-seeded these hosts.
        Assert.assertTrue(AzureActiveDirectory.hasCloudHost(new URL("https://login.sovcloud-identity.fr/common")));
        Assert.assertTrue(AzureActiveDirectory.hasCloudHost(new URL("https://login.sovcloud-identity.de/common")));
        Assert.assertTrue(AzureActiveDirectory.hasCloudHost(new URL("https://login.sovcloud-identity.sg/common")));
    }

    @Test
    public void testPreSeededCloudMetadataIsCorrect() throws Exception {
        final AzureActiveDirectoryCloud bleuCloud = AzureActiveDirectory.getAzureActiveDirectoryCloud(
                new URL("https://login.sovcloud-identity.fr/common"));
        Assert.assertNotNull(bleuCloud);
        Assert.assertEquals("login.sovcloud-identity.fr", bleuCloud.getPreferredNetworkHostName());
        Assert.assertEquals("login.sovcloud-identity.fr", bleuCloud.getPreferredCacheHostName());
    }

    @Test
    public void testPreSeededCloudsAreValidated() throws Exception {
        Assert.assertTrue(AzureActiveDirectory.isValidCloudHost(new URL("https://login.sovcloud-identity.fr/common")));
        Assert.assertTrue(AzureActiveDirectory.isValidCloudHost(new URL("https://login.sovcloud-identity.de/common")));
        Assert.assertTrue(AzureActiveDirectory.isValidCloudHost(new URL("https://login.sovcloud-identity.sg/common")));
    }

    // --- getDefaultCloudUrl tests ---

    @Test
    public void testGetDefaultCloudUrl_production() {
        AzureActiveDirectory.setEnvironment(Environment.Production);
        Assert.assertEquals(AzureActiveDirectoryEnvironment.PRODUCTION_CLOUD_URL, AzureActiveDirectory.getDefaultCloudUrl());
    }

    @Test
    public void testGetDefaultCloudUrl_preProduction() {
        AzureActiveDirectory.setEnvironment(Environment.PreProduction);
        Assert.assertEquals(AzureActiveDirectoryEnvironment.PREPRODUCTION_CLOUD_URL, AzureActiveDirectory.getDefaultCloudUrl());
    }

    // --- hasCloudHost / isValidCloudHost tests ---

    @Test
    public void testHasCloudHost_unknownHost() throws Exception {
        Assert.assertFalse(AzureActiveDirectory.hasCloudHost(new URL("https://login.example.com/common")));
    }

    @Test
    public void testPutCloud_makesHostAvailable() throws Exception {
        final String host = "login.testcloud.com";
        final AzureActiveDirectoryCloud cloud = new AzureActiveDirectoryCloud(host, host);
        AzureActiveDirectory.putCloud(host, cloud);

        Assert.assertTrue(AzureActiveDirectory.hasCloudHost(new URL("https://" + host + "/common")));
        Assert.assertTrue(AzureActiveDirectory.isValidCloudHost(new URL("https://" + host + "/common")));
    }

    @Test
    public void testIsValidCloudHost_unvalidatedCloud() throws Exception {
        final String host = "login.unvalidated.com";
        AzureActiveDirectory.putCloud(host, new AzureActiveDirectoryCloud(false));

        Assert.assertTrue(AzureActiveDirectory.hasCloudHost(new URL("https://" + host + "/common")));
        Assert.assertFalse(AzureActiveDirectory.isValidCloudHost(new URL("https://" + host + "/common")));
    }

    // --- ensureCloudDiscoveryForAuthority caching behavior ---

    @Test
    public void testEnsureCloudDiscoveryForAuthority_sovCloudAlreadyCached_noOp() throws Exception {
        // Sovereign clouds are pre-seeded, so calling ensure should be a no-op (no network).
        // This verifies it doesn't throw and the cloud remains accessible.
        final URL sovUrl = new URL("https://login.sovcloud-identity.fr/common");
        AzureActiveDirectory.ensureCloudDiscoveryForAuthority(sovUrl);

        Assert.assertTrue(AzureActiveDirectory.hasCloudHost(sovUrl));
        Assert.assertTrue(AzureActiveDirectory.isValidCloudHost(sovUrl));
    }

    @Test
    public void testEnsureCloudDiscoveryForAuthority_nullUrl_doesNotThrow() throws Exception {
        // Passing null URL should fall back to ensureCloudDiscoveryComplete (which calls global).
        // In test without network, this may throw — but we verify it doesn't NPE.
        try {
            AzureActiveDirectory.ensureCloudDiscoveryForAuthority((URL) null);
        } catch (final Exception e) {
            // Expected in test environment without network — just verify no NPE.
            Assert.assertFalse("Should not be NPE", e instanceof NullPointerException);
        }
    }

    @Test
    public void testEnsureCloudDiscoveryForAuthority_unknownHostAfterGlobalCached_noOp() throws Exception {
        // Simulate global discovery already cached by putting the default host.
        final String defaultHost = "login.microsoftonline.com";
        AzureActiveDirectory.putCloud(defaultHost, new AzureActiveDirectoryCloud(defaultHost, defaultHost));

        // Now calling with an unknown host should be a no-op (global already cached).
        final URL unknownUrl = new URL("https://login.example.com/common");
        AzureActiveDirectory.ensureCloudDiscoveryForAuthority(unknownUrl);

        // The unknown host shouldn't be added to cache.
        Assert.assertFalse(AzureActiveDirectory.hasCloudHost(unknownUrl));
        // But global is still there.
        Assert.assertTrue(AzureActiveDirectory.hasCloudHost(new URL("https://" + defaultHost + "/common")));
    }

    // --- getAzureActiveDirectoryCloudFromHostName tests ---

    @Test
    public void testGetAzureActiveDirectoryCloudFromHostName_sovCloud() {
        final AzureActiveDirectoryCloud cloud = AzureActiveDirectory.getAzureActiveDirectoryCloudFromHostName(
                "login.sovcloud-identity.fr");
        Assert.assertNotNull(cloud);
        Assert.assertEquals("login.sovcloud-identity.fr", cloud.getPreferredNetworkHostName());
    }

    @Test
    public void testGetAzureActiveDirectoryCloudFromHostName_caseInsensitive() {
        final AzureActiveDirectoryCloud cloud = AzureActiveDirectory.getAzureActiveDirectoryCloudFromHostName(
                "LOGIN.SOVCLOUD-IDENTITY.DE");
        Assert.assertNotNull(cloud);
        Assert.assertEquals("login.sovcloud-identity.de", cloud.getPreferredNetworkHostName());
    }

    @Test
    public void testGetAzureActiveDirectoryCloudFromHostName_unknown() {
        final AzureActiveDirectoryCloud cloud = AzureActiveDirectory.getAzureActiveDirectoryCloudFromHostName(
                "login.unknown.com");
        Assert.assertNull(cloud);
    }

    // --- ensureCloudDiscoveryForAuthority(Authority) overload tests ---

    @Test
    public void testEnsureCloudDiscoveryForAuthority_nullAuthority_doesNotThrow() throws Exception {
        // Passing null Authority should fall back to global discovery.
        // May throw due to no network, but must not NPE.
        try {
            AzureActiveDirectory.ensureCloudDiscoveryForAuthority((Authority) null);
        } catch (final Exception e) {
            Assert.assertFalse("Should not be NPE", e instanceof NullPointerException);
        }
    }

    @Test
    public void testEnsureCloudDiscoveryForAuthority_sovCloudAuthority_isNoOp() throws Exception {
        // Sovereign cloud authority should be already cached (pre-seeded), so no network call.
        final Authority authority = Authority.getAuthorityFromAuthorityUrl(
                "https://" + AzureActiveDirectoryCloud.BLEU_CLOUD_HOST + "/common");
        AzureActiveDirectory.ensureCloudDiscoveryForAuthority(authority);
        Assert.assertTrue(AzureActiveDirectory.hasCloudHost(
                new URL("https://" + AzureActiveDirectoryCloud.BLEU_CLOUD_HOST + "/common")));
    }

    // --- Environment switching with sovereign clouds ---

    @Test
    public void testSovCloudsRemainAfterEnvironmentSwitch() throws Exception {
        // Sovereign clouds are pre-seeded in the static init block.
        // Switching environment should NOT remove them from the cache.
        AzureActiveDirectory.setEnvironment(Environment.PreProduction);
        Assert.assertTrue(AzureActiveDirectory.hasCloudHost(
                new URL("https://login.sovcloud-identity.fr/common")));
        Assert.assertTrue(AzureActiveDirectory.hasCloudHost(
                new URL("https://login.sovcloud-identity.de/common")));
        Assert.assertTrue(AzureActiveDirectory.hasCloudHost(
                new URL("https://login.sovcloud-identity.sg/common")));
    }
}
