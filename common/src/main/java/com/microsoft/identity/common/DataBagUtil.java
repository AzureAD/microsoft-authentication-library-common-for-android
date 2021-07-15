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
package com.microsoft.identity.common;

import android.os.Bundle;

import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.util.ported.DataBag;

import java.io.Serializable;
import java.util.Map;

import lombok.NonNull;

public class DataBagUtil {

    /**
     * Converts a {@link DataBag} to a {@link Bundle}
     */
    public static Bundle toBundle(@NonNull final DataBag dataBag) {
        final Bundle bundle = new Bundle();

        final INameValueStorage<String> stringMap = dataBag.getStringMap();
        for (final String key : stringMap.keySet()) {
            bundle.putString(key, stringMap.get(key));
        }

        final INameValueStorage<byte[]> byteArrayMap = dataBag.getByteArrayMap();
        for (final String key : byteArrayMap.keySet()) {
            bundle.putByteArray(key, byteArrayMap.get(key));
        }

        final INameValueStorage<Boolean> booleanMap = dataBag.getBooleanMap();
        for (final String key : booleanMap.keySet()) {
            final Boolean valueToPut = booleanMap.get(key);
            if (valueToPut != null) {
                bundle.putBoolean(key, valueToPut);
            }
        }

        final INameValueStorage<Integer> intMap = dataBag.getIntMap();
        for (final String key : intMap.keySet()) {
            final Integer valueToPut = intMap.get(key);
            if (valueToPut != null) {
                bundle.putInt(key, valueToPut);
            }
        }

        final INameValueStorage<Serializable> serializableMap = dataBag.getSerializableMap();
        for (final String key : serializableMap.keySet()) {
            bundle.putSerializable(key, serializableMap.get(key));
        }

        return bundle;
    }

    /**
     * Converts a {@link Bundle} to a {@link DataBag}
     * */
    public static DataBag fromBundle(@NonNull final Bundle bundle) {
        final DataBag result = new DataBag();

        for (final String key : bundle.keySet()) {
            final Object value = bundle.get(key);
            if (value instanceof String) {
                result.getStringMap().put(key, (String) value);
            } else if (value instanceof byte[]) {
                result.getByteArrayMap().put(key, (byte[]) value);
            } else if (value instanceof Boolean) {
                result.getBooleanMap().put(key, (Boolean) value);
            } else if (value instanceof Integer) {
                result.getIntMap().put(key, (Integer) value);
            } else if (value instanceof Serializable) {
                result.getSerializableMap().put(key, (Serializable) value);
            }
        }

        return result;
    }
}
