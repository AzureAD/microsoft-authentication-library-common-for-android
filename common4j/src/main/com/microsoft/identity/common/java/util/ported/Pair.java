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
import java.util.Objects;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class Pair<T, U> {
    public final T first;
    public final U second;

    @Override
    @SuppressFBWarnings(value = "NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION",
            justification = "Equals function should be able to take in a null object.")
    public boolean equals(@Nullable final Object obj) {
        if (obj == null){
            return false;
        }

        if (!(obj instanceof Pair)) {
            return false;
        }

        final Pair<?, ?> castedObj = (Pair<?, ?>) obj;
        return Objects.equals(castedObj.first, first) && Objects.equals(castedObj.second, second);
    }

    /**
     * Compute a hash code using the hash codes of the underlying objects
     *
     * @return a hashcode of the Pair
     */
    @Override
    public int hashCode() {
        int result = (first != null ? first.hashCode() : 0);
        result = 31 * result + (second != null ? second.hashCode() : 0);
        return result;
    }

    /**
     * Add this item to a list if a copy of the same key-value pair doesn't exist.
     * */
    public void addToListIfNotExist(@NonNull final List<Pair<T, U>> listToBeAdded) {
        if (!listToBeAdded.contains(this)) {
            listToBeAdded.add(this);
        }
    }

}
