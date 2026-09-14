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

package com.microsoft.identity.common.adal.internal;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.HashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class ADALErrorTest {

    @Test
    public void getDescription_returnsConstructorMessage() {
        Assert.assertEquals(
                "Device needs to be managed to access the resource",
                ADALError.MDM_REQUIRED.getDescription());
        Assert.assertEquals(
                "Authority validation returned an error",
                ADALError.DEVELOPER_AUTHORITY_CAN_NOT_BE_VALIDED.getDescription());
    }

    @Test
    public void getLocalizedDescription_nullContextFallsBackToDescription() {
        for (final ADALError error : ADALError.values()) {
            Assert.assertEquals(
                    error.getDescription(),
                    error.getLocalizedDescription(null));
        }
    }

    @Test
    public void everyValue_hasNonEmptyDescription() {
        for (final ADALError error : ADALError.values()) {
            Assert.assertNotNull(error.getDescription());
            Assert.assertFalse(error.getDescription().isEmpty());
        }
    }

    @Test
    public void names_areUniqueAndValuesResolvable() {
        final Set<String> names = new HashSet<>();
        for (final ADALError error : ADALError.values()) {
            names.add(error.name());
            // valueOf round-trips every declared constant.
            Assert.assertSame(error, ADALError.valueOf(error.name()));
        }
        Assert.assertEquals(ADALError.values().length, names.size());
    }
}
