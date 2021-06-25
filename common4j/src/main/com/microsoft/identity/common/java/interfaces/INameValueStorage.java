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

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * An interface for a NameValue storage.
 * */
public interface INameValueStorage<T> {
    /**
     * Gets a value from the storage.
     *
     * @param name A name associated to the value.
     */
    T get(@NonNull String name);

    /**
     * Puts a value into the storage.
     *
     * @param name A name associated to the value.
     * @param value value to be persisted.
     */
    void put(@NonNull String name, @Nullable T value);

    /**
     * Removes a value from the storage.
     *
     * @param name A name associated to the value.
     */
    void remove(@NonNull String name);

    /**
     * Clear all data from the storage.
     */
    void clear();
}
