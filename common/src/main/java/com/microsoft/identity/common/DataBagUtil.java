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

/**
 * {@link DataBag} utilities.
 */
public class DataBagUtil {

    /**
     * Converts a {@link DataBag} to a {@link Bundle}
     */
    public static Bundle toBundle(@NonNull final DataBag dataBag) {
        final Bundle bundle = new Bundle();

        for (final String key : dataBag.keySet()) {
            bundle.putSerializable(key, dataBag.get(key));
        }

        return bundle;
    }

    /**
     * Converts a {@link Bundle} to a {@link DataBag}
     * */
    public static DataBag fromBundle(@NonNull final Bundle bundle) {
        final DataBag result = new DataBag();

        for (final String key : bundle.keySet()) {
            result.put(key, bundle.getSerializable(key));
        }

        return result;
    }
}
