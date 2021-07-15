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
package com.microsoft.identity.common;

import com.microsoft.identity.common.java.dto.AccessTokenRecord;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class AccessTokenTest {

    @Test
    public void testExpiry() {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setExpiresOn(getCurrentTimeStr());
        assertTrue(accessToken.isExpired());
    }

    @Test
    public void testShouldRefreshAfterExpiration() {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setRefreshOn(getCurrentTimeStr());
        assertTrue(accessToken.shouldRefresh());
    }

    @Test
    public void testShouldRefreshWhileStillValid() {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setRefreshOn("2524608000"); // 1/1/2050
        assertFalse(accessToken.shouldRefresh());
    }

    @Test
    public void testShouldRefreshWhenNoPropertiesAreSet() {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        assertTrue(accessToken.shouldRefresh());
    }

    private String getCurrentTimeStr() {
        return String.valueOf(
                Calendar
                        .getInstance()
                        .getTime()
                        .getTime() / 1000 - 10
        );
    }
}
