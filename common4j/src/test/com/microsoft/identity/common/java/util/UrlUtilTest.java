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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

public class UrlUtilTest {

    @Test
    public void testAppendEmptyPathUrl() throws MalformedURLException, URISyntaxException {
        Assert.assertEquals(new URL("https://www.test.com"),
                UrlUtil.appendPathToURL(new URL("https://www.test.com"), null));

        Assert.assertEquals(new URL("https://www.test.com"),
                UrlUtil.appendPathToURL(new URL("https://www.test.com"), ""));
    }

    @Test
    public void testAppendPathStringWithExtraSlashes() throws MalformedURLException, URISyntaxException {
        Assert.assertEquals(new URL("https://www.test.com/this/is/a/path"),
                UrlUtil.appendPathToURL(new URL("https://www.test.com"), "//this/is/a//path"));
    }

    @Test
    public void testAppendPathStringWithoutStartingSlash() throws MalformedURLException, URISyntaxException {
        Assert.assertEquals(new URL("https://www.test.com/path"),
                UrlUtil.appendPathToURL(new URL("https://www.test.com"), "path"));
    }

    @Test
    public void testAppendPathStringToUrlWithPath() throws MalformedURLException, URISyntaxException {
        Assert.assertEquals(new URL("https://www.test.com/path/another/path"),
                UrlUtil.appendPathToURL(new URL("https://www.test.com/path"), "/another/path"));
    }

    @Test
    public void testGettingParameterFromUrl() throws URISyntaxException {
        final Map<String, String> queryParams = UrlUtil.getParameters(
                new URI("https://www.test.com/path/another/path?param1=value1&param2=value2"));
        Assert.assertEquals(2, queryParams.size());
        Assert.assertEquals("value1", queryParams.get("param1"));
        Assert.assertEquals("value2", queryParams.get("param2"));
    }

    @Test
    public void testGettingParameterFromUrlContainingUrl() throws URISyntaxException {
        final Map<String, String> queryParams = UrlUtil.getParameters(
                new URI("msauth://com.msft.identity.client.sample.local/1wIqXSqBj7w%2Bh11ZifsnqwgyKrY%3D?wpj=1&username=test%40test.onmicrosoft.com&app_link=https%3a%2f%2fplay.google.com%2fstore%2fapps%2fdetails%3fid%3dcom.azure.authenticator"));
        Assert.assertEquals(3, queryParams.size());
        Assert.assertEquals("1", queryParams.get("wpj"));
        Assert.assertEquals("test@test.onmicrosoft.com", queryParams.get("username"));
        Assert.assertEquals("https://play.google.com/store/apps/details?id=com.azure.authenticator", queryParams.get("app_link"));
    }

    @Test
    public void testGettingParameterFromEmptyUrl() throws URISyntaxException {
        final Map<String, String> queryParams = UrlUtil.getParameters(new URI(""));
        Assert.assertEquals(0, queryParams.size());
    }
}
