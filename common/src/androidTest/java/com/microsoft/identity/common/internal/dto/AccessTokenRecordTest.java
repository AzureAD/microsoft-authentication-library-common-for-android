package com.microsoft.identity.common.internal.dto;

import junit.framework.TestCase;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class AccessTokenRecordTest extends TestCase {

    public void testIsExpired() {
        final AccessTokenRecord accessTokenRecord = new AccessTokenRecord();
        //When expiresOn is null
        try {
            accessTokenRecord.shouldRefresh();
        } catch (Exception e) {
            assertEquals(e.getClass(), NumberFormatException.class);
        }

        //When expiresOn true
        accessTokenRecord.setExpiresOn("0");
        assertTrue(accessTokenRecord.shouldRefresh());

        //When expiresOn false
        final String tomorrow = String.valueOf(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()).getTime());
        accessTokenRecord.setExpiresOn(tomorrow);
        assertFalse(accessTokenRecord.isExpired());
    }

    public void testShouldRefresh() {
        final AccessTokenRecord accessTokenRecord = new AccessTokenRecord();

        //When refreshOn is null
        accessTokenRecord.setExpiresOn("0");
        assertTrue(accessTokenRecord.shouldRefresh());

        //When refreshOn is true
        accessTokenRecord.setRefreshOn("0");
        assertTrue(accessTokenRecord.shouldRefresh());

        //When refreshOn is false
        final String tomorrow = String.valueOf(Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()).getTime());
        accessTokenRecord.setRefreshOn(tomorrow);
        assertFalse(accessTokenRecord.shouldRefresh());
    }

}