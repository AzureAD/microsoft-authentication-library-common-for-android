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
package com.microsoft.identity.common.java.opentelemetry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.api.baggage.BaggageEntry;
import io.opentelemetry.api.baggage.BaggageEntryMetadata;

/**
 * A custom noop implementation of {@link Baggage}.
 */
public class NoopBaggage implements Baggage {

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super BaggageEntry> consumer) {}

    @Override
    public Map<String, BaggageEntry> asMap() {
        return new HashMap<>();
    }

    @Nullable
    @Override
    public String getEntryValue(final String entryKey) {
        return null;
    }

    @Override
    public BaggageBuilder toBuilder() {
        return new NoopBaggageBuilder();
    }
    /**
     * A no-op implementation of {@link BaggageBuilder}.
     */
    private static class NoopBaggageBuilder implements BaggageBuilder {

        @Override
        public BaggageBuilder put(final String key, final String value, final BaggageEntryMetadata entryMetadata) {
            return this;
        }

        @Override
        public BaggageBuilder remove(final String key) {
            return this;
        }

        @Override
        public Baggage build() {
            return new NoopBaggage();
        }
    }
}
