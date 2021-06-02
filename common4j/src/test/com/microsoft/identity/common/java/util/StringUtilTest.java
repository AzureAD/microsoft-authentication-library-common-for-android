//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class StringUtilTest {
    @Test
    public void testCountMatchWithEmptyStrings(){
        Assert.assertEquals(0, StringUtil.countMatches(null, "sub"));
        Assert.assertEquals(0, StringUtil.countMatches("", "sub"));
        Assert.assertEquals(0, StringUtil.countMatches("source", null));
        Assert.assertEquals(0, StringUtil.countMatches("source", ""));
    }

    @Test
    public void testCountMatch(){
        Assert.assertEquals(0, StringUtil.countMatches("string", "sub"));
        Assert.assertEquals(0, StringUtil.countMatches("sustring", "sub"));
        Assert.assertEquals(1, StringUtil.countMatches("substring", "sub"));
        Assert.assertEquals(2, StringUtil.countMatches("substring substring", "sub"));
        Assert.assertEquals(3, StringUtil.countMatches("substring substringsubstring", "sub"));
    }

    @Test
    public void testCountMatchCaseInsensitive(){
        Assert.assertEquals(0, StringUtil.countMatches("string", "sUb"));
        Assert.assertEquals(0, StringUtil.countMatches("sustring", "sUb"));
        Assert.assertEquals(1, StringUtil.countMatches("substring", "sUb"));
        Assert.assertEquals(2, StringUtil.countMatches("substring substring", "sUb"));
        Assert.assertEquals(3, StringUtil.countMatches("substring substringsubstring", "sUb"));
    }
}
