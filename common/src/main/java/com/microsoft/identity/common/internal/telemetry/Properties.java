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

package com.microsoft.identity.common.internal.telemetry;

import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The base class for the event properties.
 */
public class Properties {
    private ConcurrentHashMap<String, String> mProperties;

    Properties(final ConcurrentHashMap<String, String> properties) {
        mProperties = properties;
    }

    public Properties() {
        mProperties = new ConcurrentHashMap<>();
    }

    public Properties put(final String key, final String value) {
        if (mProperties == null) {
            mProperties = new ConcurrentHashMap<>();
        }

        if (!StringUtil.isEmpty(key) && !StringUtil.isEmpty(value)) {
            mProperties.put(key, value);
        }
        return this;
    }

    public Properties remove(final String key) {
        mProperties.remove(key);
        return this;
    }

    public Properties remove(final String key, final String value) {
        mProperties.remove(key, value);
        return this;
    }

    public Properties put(final Properties appendProperties) {
        if (mProperties == null) {
            mProperties = appendProperties.getProperties();
        } else {
            mProperties.putAll(appendProperties.getProperties());
        }

        return this;
    }

    public ConcurrentHashMap<String, String> getProperties() {
        return mProperties;
    }
}
