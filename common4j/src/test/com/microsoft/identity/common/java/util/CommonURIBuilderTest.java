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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(JUnit4.class)
public class CommonURIBuilderTest {

    @Test(expected = UnsupportedOperationException.class)
    public void testCallingAddParameter(){
        final CommonURIBuilder builder = new CommonURIBuilder()
                .addParameter("test", "test");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCallingAddParameters(){
        final CommonURIBuilder builder = new CommonURIBuilder()
                .addParameters(new ArrayList<>());
    }

    @Test
    public void testCallingAddParametersIfAbsent(){
        final CommonURIBuilder builder = new CommonURIBuilder()
                .addParameterIfAbsent("Test1", "Value1");

        Assert.assertEquals(1, builder.getQueryParams().size());
        Assert.assertEquals("Test1", builder.getQueryParams().get(0).getName());
        Assert.assertEquals("Value1", builder.getQueryParams().get(0).getValue());

        builder.addParameterIfAbsent("Test2", "Value2");

        Assert.assertEquals(2, builder.getQueryParams().size());
        Assert.assertEquals("Test1", builder.getQueryParams().get(0).getName());
        Assert.assertEquals("Value1", builder.getQueryParams().get(0).getValue());
        Assert.assertEquals("Test2", builder.getQueryParams().get(1).getName());
        Assert.assertEquals("Value2", builder.getQueryParams().get(1).getValue());
    }

    @Test
    public void testCallingAddParametersIfAbsent_ValueAlreadyExists(){
        final CommonURIBuilder builder = new CommonURIBuilder()
                .setParameter("Test1", "Value1")
                .addParameterIfAbsent("Test1", "Value2");

        Assert.assertEquals(1, builder.getQueryParams().size());
        Assert.assertEquals("Test1", builder.getQueryParams().get(0).getName());
        Assert.assertEquals("Value1", builder.getQueryParams().get(0).getValue());
    }

    @Test
    public void testCallingAddParametersIfAbsent_WithMap(){
        final Map<String, String> map = new HashMap<>();
        map.put("Test1", "Value1");
        map.put("Test2", "Value2");

        final CommonURIBuilder builder = new CommonURIBuilder().addParametersIfAbsent(map);

        Assert.assertEquals(2, builder.getQueryParams().size());
        Assert.assertEquals("Test1", builder.getQueryParams().get(0).getName());
        Assert.assertEquals("Value1", builder.getQueryParams().get(0).getValue());
        Assert.assertEquals("Test2", builder.getQueryParams().get(1).getName());
        Assert.assertEquals("Value2", builder.getQueryParams().get(1).getValue());
    }

    @Test
    public void testCallingAddParametersIfAbsent_WithMap_ValueAlreadyExists(){
        final CommonURIBuilder builder = new CommonURIBuilder()
                .setParameter("Test1", "Value1");

        final Map<String, String> map = new HashMap<>();
        map.put("Test1", "Value2");

        builder.addParametersIfAbsent(map);

        Assert.assertEquals(1, builder.getQueryParams().size());
        Assert.assertEquals("Test1", builder.getQueryParams().get(0).getName());
        Assert.assertEquals("Value1", builder.getQueryParams().get(0).getValue());
    }

    @Test
    public void testCallingAddParametersIfAbsent_WithMapEntryList(){
        final List<Map.Entry<String, String>> map = new ArrayList<>();
        map.add(new AbstractMap.SimpleEntry<String, String>("Test1", "Value1"));
        map.add(new AbstractMap.SimpleEntry<String, String>("Test2", "Value2"));

        final CommonURIBuilder builder = new
                CommonURIBuilder().addParametersIfAbsent(map);

        Assert.assertEquals(2, builder.getQueryParams().size());
        Assert.assertEquals("Test1", builder.getQueryParams().get(0).getName());
        Assert.assertEquals("Value1", builder.getQueryParams().get(0).getValue());
        Assert.assertEquals("Test2", builder.getQueryParams().get(1).getName());
        Assert.assertEquals("Value2", builder.getQueryParams().get(1).getValue());
    }

    @Test
    public void testCallingAddParametersIfAbsent_WithMapEntryList_ValueAlreadyExists(){
        final CommonURIBuilder builder = new CommonURIBuilder()
                .setParameter("Test1", "Value1");

        final List<Map.Entry<String, String>> map = new ArrayList<>();
        map.add(new AbstractMap.SimpleEntry<String, String>("Test1", "Value1"));

        builder.addParametersIfAbsent(map);

        Assert.assertEquals(1, builder.getQueryParams().size());
        Assert.assertEquals("Test1", builder.getQueryParams().get(0).getName());
        Assert.assertEquals("Value1", builder.getQueryParams().get(0).getValue());
    }
}
