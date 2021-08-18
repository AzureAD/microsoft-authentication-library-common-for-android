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
package com.microsoft.identity.common.internal.dto;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.java.dto.AccessTokenRecord;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@RunWith(AndroidJUnit4.class)
public class AccessTokenRecordTest extends TestCase {

    @Test
    public void testIsExpired() {
        final AccessTokenRecord accessTokenRecord = new AccessTokenRecord();
        //When expiresOn is null
        try {
            accessTokenRecord.isExpired();
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

    @Test
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