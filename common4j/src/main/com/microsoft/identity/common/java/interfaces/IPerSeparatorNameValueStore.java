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
package com.microsoft.identity.common.java.interfaces;

import com.microsoft.identity.common.java.util.ported.Predicate;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

public interface IPerSeparatorNameValueStore<S, T> {
    /**
     * Gets a value from the storage.
     *
     * @param name A name associated to the value.
     */
    @Nullable
    T get(@NonNull S separator, @NonNull String name);

    /**
     * @return a map containing all the values associated to the supplied separator in this structure.
     */
    @NonNull
    Map<String, T> getAll(@NonNull S separator);

    /**
     * Puts a value into the storage.
     *
     * @param name  A name associated to the value.
     * @param value value to be persisted.
     */
    void put(@NonNull S separator, @NonNull String name, @Nullable T value);

    /**
     * Removes a value from the storage.
     * [
     *
     * @param name A name associated to the value.
     */
    void remove(@NonNull S separator, @NonNull String name);

    /**
     * Clear all data from the storage.
     */
    void clear(@NonNull S separator);

    /**
     * Get all keys in this storage.
     */
    @NonNull
    Set<String> keySet(@NonNull S separator);

    /**
     *
     */
    Iterator<Map.Entry<String, T>> getAllFilteredByKey(@NonNull S separator, Predicate<String> keyFilter);
}
