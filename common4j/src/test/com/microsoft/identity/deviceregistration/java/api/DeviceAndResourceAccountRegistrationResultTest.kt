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

package com.microsoft.identity.deviceregistration.java.api

import com.microsoft.identity.common.java.dto.AccountRecord
import com.microsoft.identity.common.java.dto.IAccountRecord
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DeviceAndResourceAccountRegistrationResultTest {
    @Test
    fun exposesBothRecordsWithoutIncludingThemInToString() {
        val deviceRecord = DeviceRegistrationRecord("tenant", "resource@example.com", "device", true, true)
        val accountRecord = AccountRecord().apply { username = "resource@example.com" }
        val result = DeviceAndResourceAccountRegistrationResult(deviceRecord, accountRecord)

        assertSame(deviceRecord, result.deviceRegistrationRecord)
        assertSame(accountRecord, result.resourceAccountRecord)
        assertFalse(result.toString().contains("resource@example.com"))
    }

    @Test
    fun rejectsNullResourceAccountFromJava() {
        val deviceRecord = DeviceRegistrationRecord("tenant", "", "device", false, true)
        val constructor = DeviceAndResourceAccountRegistrationResult::class.java.getConstructor(
            IDeviceRegistrationRecord::class.java, IAccountRecord::class.java
        )
        try {
            constructor.newInstance(deviceRecord, null)
            fail("Expected the required resource account record to reject null")
        } catch (exception: java.lang.reflect.InvocationTargetException) {
            assertTrue(exception.cause is NullPointerException)
        }
    }
}