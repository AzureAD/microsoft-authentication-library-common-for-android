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
package com.microsoft.identity.common.java.storage;

import lombok.NonNull;

/**
 * Describes an adapter that can take the value of an Object as specified by the type parameter, and
 * convert it into its String representation as well as vice versa.
 *
 * @param <T> the type parameter that denotes the {@link java.lang.reflect.Type} of the Object that
 *            needs to be adapted
 */
public interface IGenericTypeStringAdapter<T> {

    /**
     * Adapt the given value into a String representation.
     *
     * @param value the value that needs to be adapted
     * @return a String representation of the value
     */
    String adapt(T value);

    /**
     * Adapt the given String into a the Java Object specified by the type parameter.
     *
     * @param value the String that needs to be adapted
     * @return the adapted value
     */
    T adapt(String value);

    /**
     * An {@link IGenericTypeStringAdapter} for {@link Long}.
     */
    IGenericTypeStringAdapter<Long> LongStringAdapter = new IGenericTypeStringAdapter<Long>() {
        @NonNull
        @Override
        public String adapt(@NonNull final Long value) {
            return String.valueOf(value);
        }

        @NonNull
        @Override
        public Long adapt(@NonNull final String value) throws NumberFormatException {
            return Long.parseLong(value);
        }
    };
}
