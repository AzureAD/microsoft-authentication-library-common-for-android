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
package com.microsoft.identity.common.java.util.ported;

import com.microsoft.identity.common.java.interfaces.INameValueStorage;

import java.io.Serializable;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
public class PropertyBag {
    private final INameValueStorage<Object> mMap = new InMemoryStorage<>();

    public <T extends Serializable> void put(@NonNull final String name, @Nullable final T value) {
        mMap.put(name, value);
    }

    @Nullable
    public <T extends Serializable> T get(@NonNull final String name) {
        return getOrDefaultInternal(name, null);
    }

    @NonNull
    public <T extends Serializable> T getOrDefault(@NonNull final String name, @NonNull final T defaultValue) {
       return getOrDefaultInternal(name, defaultValue);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private <T extends Serializable> T getOrDefaultInternal(@NonNull final String name, @Nullable final T defaultValue) {
        final Object object = mMap.get(name);
        if (object == null) {
            return defaultValue;
        }

        try {
            return (T) object;
        } catch (final ClassCastException e) {
            return defaultValue;
        }
    }

    public Set<String> keySet(){
        return mMap.keySet();
    }
}
