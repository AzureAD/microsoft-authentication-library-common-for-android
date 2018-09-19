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

import com.microsoft.identity.common.internal.dto.AccessToken;

import junit.framework.Assert;

import org.junit.Test;

import java.util.Calendar;

public class AccessTokenTest {

    private static final int ONE_MINUTE = 60;

    @Test
    public void testExpiry() {
        final AccessToken accessToken = new AccessToken();
        accessToken.setExpiresOn(getCurrentTimeStr());
        Assert.assertTrue(accessToken.isExpired());
    }

    @Test
    public void testExpiryWithExtExpiresOn() {
        final AccessToken accessToken = new AccessToken();
        final String currentTime = getCurrentTimeStr();
        final String currentTimePlus5Min = String.valueOf(Long.valueOf(currentTime) + (5 * ONE_MINUTE));
        accessToken.setExpiresOn(currentTime);
        accessToken.setExtendedExpiresOn(currentTimePlus5Min);
        Assert.assertFalse(accessToken.isExpired());
    }

    private String getCurrentTimeStr() {
        return String.valueOf(
                Calendar
                        .getInstance()
                        .getTime()
                        .getTime() / 1000
        );
    }
}
