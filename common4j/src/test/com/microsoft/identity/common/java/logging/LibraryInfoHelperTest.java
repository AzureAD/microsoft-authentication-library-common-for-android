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
package com.microsoft.identity.common.java.logging;

import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.PRODUCT;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.VERSION;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class LibraryInfoHelperTest {

    @After
    public void cleanUp() {
        DiagnosticContext.INSTANCE.clear();
    }

    @Test
    public void testLibraryNameEmpty() {
        Assert.assertEquals(LibraryInfoHelper.NOT_SET, LibraryInfoHelper.getLibraryName());
    }

    @Test
    public void testLibraryNameSet() {
        final String PRODUCT_NAME = "MSAL";
        final RequestContext rc = new RequestContext();
        rc.put(PRODUCT, PRODUCT_NAME);
        DiagnosticContext.INSTANCE.setRequestContext(rc);
        Assert.assertEquals(PRODUCT_NAME, LibraryInfoHelper.getLibraryName());
    }

    @Test
    public void testLibraryVersionEmpty() {
        Assert.assertEquals("1.5.9-default", LibraryInfoHelper.getLibraryVersion());
    }

    @Test
    public void testLibraryVersionSet() {
        final String PRODUCT_VERSION = "5.0.0";
        final RequestContext rc = new RequestContext();
        rc.put(VERSION, PRODUCT_VERSION);
        DiagnosticContext.INSTANCE.setRequestContext(rc);
        Assert.assertEquals(PRODUCT_VERSION, LibraryInfoHelper.getLibraryVersion());
    }
}
