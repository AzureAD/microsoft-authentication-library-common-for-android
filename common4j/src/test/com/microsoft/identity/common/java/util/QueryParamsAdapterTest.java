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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.identity.common.java.controllers.ExceptionAdapter;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.TerminalException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(JUnit4.class)
public class QueryParamsAdapterTest {

    @Test
    public void testConvertToJson() throws Exception {
        final List<Map.Entry<String, String>> input = new ArrayList<>();
        input.add(new AbstractMap.SimpleEntry<String, String>("eqp1", "1"));
        input.add(new AbstractMap.SimpleEntry<String, String>("eqp2", "2"));

        final String expected = "[{\"first\":\"eqp1\",\"second\":\"1\"},{\"first\":\"eqp2\",\"second\":\"2\"}]";

        Assert.assertEquals(expected, QueryParamsAdapter._toJson(input));
    }

    @Test
    public void testConvertToProperJsonFormat() throws Exception{
        final String expected = "{\"eqp1\":\"1\",\"eqp2\":\"2\"}";

        final List<Map.Entry<String, String>> input = new ArrayList<>();
        input.add(new AbstractMap.SimpleEntry<String, String>("eqp1", "1"));
        input.add(new AbstractMap.SimpleEntry<String, String>("eqp2", "2"));

        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(
                QueryParamsAdapter.getListType(),
                new QueryParamsAdapter(true)
        );
        final Gson gson = gsonBuilder.create();

        Assert.assertEquals(expected, gson.toJson(input, QueryParamsAdapter.getListType()));
    }

    @Test
    public void test123() throws Exception{
        TerminalException t = new TerminalException("errorMsg", ClientException.KEY_RING_WRITE_FAILURE);
        BaseException e = ExceptionAdapter.baseExceptionFromException(t);
        Assert.assertEquals(e.getErrorCode(), t.getErrorCode());
        Assert.assertEquals(e.getCause(), t);
    }

    @Test
    public void testConvertFromNullJsonString() throws Exception {
        final List<Map.Entry<String, String>> result = QueryParamsAdapter._fromJson(null);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testConvertFromEmptyJsonString() throws Exception {
        final List<Map.Entry<String, String>> result = QueryParamsAdapter._fromJson("");
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testConvertFromJson() throws Exception {
        final String input = "[{\"first\":\"eqp1\",\"second\":\"1\"},{\"first\":\"eqp2\",\"second\":\"2\"}]";

        final List<Map.Entry<String, String>> expected = new ArrayList<>();
        expected.add(new AbstractMap.SimpleEntry<String, String>("eqp1", "1"));
        expected.add(new AbstractMap.SimpleEntry<String, String>("eqp2", "2"));

        Assert.assertEquals(expected, QueryParamsAdapter._fromJson(input));
    }

    @Test
    public void testConvertFromProperJsonFormat() throws Exception{
        final String input = "{\"eqp1\": \"1\", \"eqp2\": \"2\"}";

        final List<Map.Entry<String, String>> expected = new ArrayList<>();
        expected.add(new AbstractMap.SimpleEntry<String, String>("eqp1", "1"));
        expected.add(new AbstractMap.SimpleEntry<String, String>("eqp2", "2"));

        Assert.assertEquals(expected, QueryParamsAdapter._fromJson(input));
    }

    @Test
    public void testConvertFromUnsupportedJson() throws Exception {
        // This is what we get if we serialized List<Map.Entry> directly.
        // the difference from List<Pair> is first->key and second->value.
        final String input = "[{\"key\":\"eqp1\",\"value\":\"1\"},{\"key\":\"eqp2\",\"value\":\"2\"}]";
        try {
            QueryParamsAdapter._fromJson(input);
            Assert.fail();
        } catch (final ClientException e){
            Assert.assertEquals(ClientException.JSON_PARSE_FAILURE, e.getErrorCode());
        }
    }

    @Test
    public void testConvertFromMalformedJson(){
        final String input = "[{\"eqp1\"}, {\"eqp2\", \"2\"}]";
        try {
            QueryParamsAdapter._fromJson(input);
            Assert.fail();
        } catch (final ClientException e){
            Assert.assertEquals(ClientException.JSON_PARSE_FAILURE, e.getErrorCode());
        }
    }

    @Test
    public void testConvertFromTruncatedJson(){
        final String input = "[{\"key1\"";
        try {
            QueryParamsAdapter._fromJson(input);
            Assert.fail();
        } catch (final ClientException e){
            Assert.assertEquals(ClientException.JSON_PARSE_FAILURE, e.getErrorCode());
        }
    }
}
