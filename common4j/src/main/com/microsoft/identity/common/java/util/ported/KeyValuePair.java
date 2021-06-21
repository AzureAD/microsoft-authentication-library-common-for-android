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

package com.microsoft.identity.common.java.util.ported;

import java.util.List;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 * NOTE: There are classes that are using this that allows key to be null.
 * Forcing non-null on lombok will throw an exception at runtime (and immediately break those).
 *
 * TODO: Address those places and mark key with @NonNull.
 * */
@AllArgsConstructor
@EqualsAndHashCode
public class KeyValuePair<T, U> {

    @Nullable
    public final T key;

    @Nullable
    public final U value;

    /**
     * A convenience method to create a pair.
     * */
    public static <T, U> KeyValuePair<T, U> create(@Nullable final T key,
                                                   @Nullable final U value){
        return new KeyValuePair<>(key, value);
    }

    /**
     * Add this item to a list if a copy of the same key-value pair doesn't exist.
     * */
    public void addToListIfNotExist(@NonNull final List<KeyValuePair<T, U>> listToBeAdded) {
        if (!listToBeAdded.contains(this)) {
            listToBeAdded.add(this);
        }
    }
}
