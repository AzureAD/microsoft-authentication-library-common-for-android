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
package com.microsoft.identity.common.internal.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.microsoft.identity.common.adal.internal.cache.ADALTokenCacheItem;
import com.microsoft.identity.common.java.controllers.BaseController;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic unit tests for the pure-logic static helpers of
 * {@link TokenCacheItemMigrationAdapter}. Only the synchronous token-selection /
 * filtering helpers are exercised here — the threaded {@code migrateTokens} /
 * network-backed {@code renewToken} paths are intentionally not covered.
 */
public class TokenCacheItemMigrationAdapterTest {

    private static ADALTokenCacheItem item(final String refreshToken,
                                           final String resource,
                                           final String authority,
                                           final String clientId,
                                           final boolean isMrrt,
                                           final String familyClientId) {
        final ADALTokenCacheItem cacheItem = new ADALTokenCacheItem();
        cacheItem.setRefreshToken(refreshToken);
        cacheItem.setResource(resource);
        cacheItem.setAuthority(authority);
        cacheItem.setClientId(clientId);
        cacheItem.setIsMultiResourceRefreshToken(isMrrt);
        cacheItem.setFamilyClientId(familyClientId);
        return cacheItem;
    }

    @Test
    public void findRt_returnsFirstItemWithRefreshToken() {
        final ADALTokenCacheItem noRt = item(null, "res", "https://login/common", "client", false, null);
        final ADALTokenCacheItem withRt = item("rt-secret", "res", "https://login/common", "client", false, null);

        assertSame(withRt, TokenCacheItemMigrationAdapter.findRt(Arrays.asList(noRt, withRt)));
    }

    @Test
    public void findRt_noRefreshToken_returnsNull() {
        final ADALTokenCacheItem noRt = item("", "res", "https://login/common", "client", false, null);
        assertNull(TokenCacheItemMigrationAdapter.findRt(Arrays.asList(noRt)));
    }

    @Test
    public void findMrrt_returnsMultiResourceRefreshToken() {
        final ADALTokenCacheItem rtOnly = item("rt1", "res", "https://login/common", "client", false, null);
        final ADALTokenCacheItem mrrt = item("rt2", "res", "https://login/common", "client", true, null);

        assertSame(mrrt, TokenCacheItemMigrationAdapter.findMrrt(Arrays.asList(rtOnly, mrrt)));
    }

    @Test
    public void findMrrt_noMultiResourceToken_returnsNull() {
        final ADALTokenCacheItem rtOnly = item("rt1", "res", "https://login/common", "client", false, null);
        assertNull(TokenCacheItemMigrationAdapter.findMrrt(Arrays.asList(rtOnly)));
    }

    @Test
    public void findFrt_returnsFamilyRefreshToken() {
        final ADALTokenCacheItem mrrt = item("rt1", "res", "https://login/common", "client", true, null);
        final ADALTokenCacheItem frt = item("rt2", "res", "https://login/common", "client", true, "1");

        assertSame(frt, TokenCacheItemMigrationAdapter.findFrt(Arrays.asList(mrrt, frt)));
    }

    @Test
    public void findFrt_noFamilyToken_returnsNull() {
        final ADALTokenCacheItem mrrt = item("rt1", "res", "https://login/common", "client", true, null);
        assertNull(TokenCacheItemMigrationAdapter.findFrt(Arrays.asList(mrrt)));
    }

    @Test
    public void splitTokensByClientId_groupsByClientId() {
        final ADALTokenCacheItem a1 = item("rt1", "res", "https://login/common", "clientA", false, null);
        final ADALTokenCacheItem a2 = item("rt2", "res", "https://login/common", "clientA", false, null);
        final ADALTokenCacheItem b1 = item("rt3", "res", "https://login/common", "clientB", false, null);

        final Map<String, List<ADALTokenCacheItem>> result =
                TokenCacheItemMigrationAdapter.splitTokensByClientId(Arrays.asList(a1, a2, b1));

        assertEquals(2, result.size());
        assertEquals(2, result.get("clientA").size());
        assertEquals(1, result.get("clientB").size());
    }

    @Test
    public void preferentiallySelectTokens_prefersFrtThenMrrtThenRt() {
        final ADALTokenCacheItem rtOnlyForA = item("rtA1", "res", "https://login/common", "clientA", false, null);
        final ADALTokenCacheItem mrrtForA = item("rtA2", "res", "https://login/common", "clientA", true, null);
        final ADALTokenCacheItem frtForA = item("rtA3", "res", "https://login/common", "clientA", true, "1");

        final ADALTokenCacheItem rtOnlyForB = item("rtB1", "res", "https://login/common", "clientB", false, null);
        final ADALTokenCacheItem mrrtForB = item("rtB2", "res", "https://login/common", "clientB", true, null);

        final ADALTokenCacheItem rtOnlyForC = item("rtC1", "res", "https://login/common", "clientC", false, null);

        final Map<String, List<ADALTokenCacheItem>> input = new HashMap<>();
        input.put("clientA", Arrays.asList(rtOnlyForA, mrrtForA, frtForA));
        input.put("clientB", Arrays.asList(rtOnlyForB, mrrtForB));
        input.put("clientC", Arrays.asList(rtOnlyForC));

        final Map<String, List<ADALTokenCacheItem>> result =
                TokenCacheItemMigrationAdapter.preferentiallySelectTokens(input);

        assertEquals(3, result.size());
        assertSame(frtForA, result.get("clientA").get(0));
        assertSame(mrrtForB, result.get("clientB").get(0));
        assertSame(rtOnlyForC, result.get("clientC").get(0));
    }

    @Test
    public void filterDuplicateTokens_skipsResourcelessAndDedupesByRefreshToken() {
        final ADALTokenCacheItem resourceless = item("rt1", null, "https://login/tenant", "client", false, null);
        final ADALTokenCacheItem tenanted = item("rt-shared", "res", "https://login/tenant", "client", false, null);
        final ADALTokenCacheItem homeTenant = item("rt-shared", "res", "https://login/common", "client", false, null);

        final List<ADALTokenCacheItem> result = new ArrayList<>(
                TokenCacheItemMigrationAdapter.filterDuplicateTokens(
                        Arrays.asList(resourceless, tenanted, homeTenant)));

        // resourceless is skipped; the two "rt-shared" items collapse to one, preferring
        // the home-tenant (authority contains "/common") entry.
        assertEquals(1, result.size());
        assertSame(homeTenant, result.get(0));
    }

    @Test
    public void getScopesForTokenRequest_appendsDefaultScopes() {
        final String scopes = TokenCacheItemMigrationAdapter.getScopesForTokenRequest("https://graph.microsoft.com");
        assertTrue(scopes.endsWith(" " + BaseController.getDelimitedDefaultScopeString()));
    }
}
