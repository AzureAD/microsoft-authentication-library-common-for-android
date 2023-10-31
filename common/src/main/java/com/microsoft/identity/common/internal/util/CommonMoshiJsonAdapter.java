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
package com.microsoft.identity.common.internal.util;

import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.TerminalException;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.lang.reflect.Type;

import lombok.NonNull;

/**
 * Converts Java classes to and from JSON.
 */
public class CommonMoshiJsonAdapter {

    private final Moshi mMoshi;

    /**
     * Constructor for CommonMoshiJsonAdapter.
     */
    public CommonMoshiJsonAdapter() {
        mMoshi = new Moshi.Builder()
                //Add any custom adapters here.
                .build();
    }

    /**
     * Convert a JAVA object to its JSON representation.
     *
     * @param obj the object that needs to be converted to JSON
     * @param <T> the type of the object that will be converted to JSON
     * @return a String representing the JSON of the supplied of the object
     */
    public <T> String toJson(@NonNull final T obj) {
        final Type type = obj.getClass();
        final JsonAdapter<T> jsonAdapter = mMoshi.adapter(type);
        return jsonAdapter.toJson(obj);
    }

    /**
     * Convert json into an object of a specified type.
     *
     * @param json     the JSON that needs to be converted to a Java object
     * @param classOfT the class of the object to which the json should be converted
     * @param <T>      the type of the JAVA object that will be returned after deserialization
     * @return a JAVA object
     */
    public <T> T fromJson(@NonNull final String json, @NonNull final Class<T> classOfT)
            throws TerminalException {
        final JsonAdapter<T> jsonAdapter = mMoshi.adapter(classOfT);
        try {
            return jsonAdapter.fromJson(json);
        } catch (final IOException e) {
            throw new TerminalException(e.getMessage(), e, ErrorStrings.IO_ERROR);
        }
    }
}
