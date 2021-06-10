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
package com.microsoft.identity.common.java.util;

import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class UrlUtilTest {

    @Test
    public void testAppendEmptyPathUrl() throws MalformedURLException, URISyntaxException {
        Assert.assertEquals("https://www.test.com",
                UrlUtil.appendPathToURL(new URL("https://www.test.com"), null));

        Assert.assertEquals("https://www.test.com",
                UrlUtil.appendPathToURL(new URL("https://www.test.com"), ""));
    }

    @Test
    public void testAppendPathStringWithExtraSlashes() throws MalformedURLException, URISyntaxException {
        Assert.assertEquals("https://www.test.com/this/is/a/path",
                UrlUtil.appendPathToURL(new URL("https://www.test.com"), "//this/is/a//path"));
    }

    @Test
    public void testAppendPathStringWithoutStartingSlash() throws MalformedURLException, URISyntaxException {
        Assert.assertEquals( "https://www.test.com/path",
                UrlUtil.appendPathToURL(new URL("https://www.test.com"), "path"));
    }

    @Test
    public void testAppendPathStringToUrlWithPath() throws MalformedURLException, URISyntaxException {
        Assert.assertEquals("https://www.test.com/path/another/path",
                UrlUtil.appendPathToURL(new URL("https://www.test.com/path"), "/another/path"));
    }
}
