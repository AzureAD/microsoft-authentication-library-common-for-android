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

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * CharArrayJsonAdapter is used with GSON to serialize character array as string and vice-versa.
 * The default implementation in GSON will serialize a character array to array of characters.
 * GSON API supports strings and not character array, so type conversion is required.
 */
public class CharArrayJsonAdapter extends TypeAdapter<char[]> {

    /**
     * Writes one JSON value (an array, object, string, number, boolean or null) for value.
     * @param out
     * @param val the Java object to write. May be null.
     * @throws IOException
     */
    @Override
    public void write(JsonWriter out, char[] val) throws IOException {
        out.value(new String(val));
    }

    /**
     * Converts value to a JSON document and writes it to out. Unlike Gson's similar toJson method,
     * this write is strict. Create a lenient JsonWriter and call write(JsonWriter, Object) for lenient writing.
     * @param in the Java object to convert. May be null.
     * @return
     * @throws IOException
     */
    @Override
    public char[] read(JsonReader in) throws IOException {
        return in.nextString().toCharArray();
    }
}
