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
package com.microsoft.identity.common.internal.broker;

import com.microsoft.identity.common.java.cache.CacheRecord;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(JUnit4.class)
public class BrokerResultClearSensitiveDataTest {

    private static final String FAKE_ACCESS_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.fake_at";
    private static final String FAKE_ID_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.fake_id";
    private static final String FAKE_REFRESH_TOKEN = "0.ARgA.fake_rt";
    private static final String FAKE_HTTP_BODY = "{\"access_token\":\"secret\"}";
    private static final String FAKE_CLIENT_INFO = "eyJ1aWQiOiIxMjM0NSJ9";
    private static final String FAKE_SHARED_TOKEN = "shared_cache_record_token_value";

    @Test
    public void testClearSensitiveData_releasesAllSensitiveFields() {
        final AccessTokenRecord sharedAccessToken = new AccessTokenRecord();
        sharedAccessToken.setSecret(FAKE_SHARED_TOKEN);

        final AccountRecord accountRecord = new AccountRecord();
        accountRecord.setHomeAccountId("uid.utid");

        final ICacheRecord cacheRecord = CacheRecord.builder()
                .account(accountRecord)
                .accessToken(sharedAccessToken)
                .build();

        final List<ICacheRecord> tenantProfileList = Collections.singletonList(cacheRecord);

        final BrokerResult brokerResult = new BrokerResult.Builder()
                .accessToken(FAKE_ACCESS_TOKEN)
                .idToken(FAKE_ID_TOKEN)
                .refreshToken(FAKE_REFRESH_TOKEN)
                .httpResponseBody(FAKE_HTTP_BODY)
                .clientInfo(FAKE_CLIENT_INFO)
                .tenantProfileCacheRecords(tenantProfileList)
                .build();

        // Verify pre-conditions
        assertEquals(FAKE_ACCESS_TOKEN, brokerResult.getAccessToken());
        assertEquals(FAKE_ID_TOKEN, brokerResult.getIdToken());
        assertEquals(FAKE_REFRESH_TOKEN, brokerResult.getRefreshToken());
        assertEquals(FAKE_HTTP_BODY, brokerResult.getHttpResponseBody());
        assertEquals(FAKE_CLIENT_INFO, brokerResult.getClientInfo());
        assertNotNull(brokerResult.getTenantProfileData());

        // Act: clear sensitive data
        brokerResult.clearSensitiveData();

        // Assert: all sensitive fields are nulled
        assertNull("AccessToken should be null", brokerResult.getAccessToken());
        assertNull("IdToken should be null", brokerResult.getIdToken());
        assertNull("RefreshToken should be null", brokerResult.getRefreshToken());
        assertNull("HttpResponseBody should be null", brokerResult.getHttpResponseBody());
        assertNull("ClientInfo should be null", brokerResult.getClientInfo());
        assertNull("TenantProfileData should be null", brokerResult.getTenantProfileData());

        // Assert: idempotency — calling clearSensitiveData() a second time should not throw
        brokerResult.clearSensitiveData();

        // Assert: external shared record was not mutated
        assertEquals("Shared cache record token should remain untouched",
                FAKE_SHARED_TOKEN, sharedAccessToken.getSecret());
    }

    @Test
    public void testClearSensitiveData_handlesNullFields() {
        final BrokerResult brokerResult = new BrokerResult.Builder().build();

        // Act: should not throw NPE when all fields are already null
        brokerResult.clearSensitiveData();

        assertNull(brokerResult.getAccessToken());
        assertNull(brokerResult.getIdToken());
        assertNull(brokerResult.getRefreshToken());
        assertNull(brokerResult.getHttpResponseBody());
        assertNull(brokerResult.getClientInfo());
        assertNull(brokerResult.getTenantProfileData());
    }
}
