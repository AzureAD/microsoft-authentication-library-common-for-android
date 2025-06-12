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
package com.microsoft.identity.common.internal.providers.oauth2;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.OTEL_CONTEXT_CARRIER;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.microsoft.identity.common.internal.ui.DualScreenActivity;
import com.microsoft.identity.common.internal.util.CommonMoshiJsonAdapter;
import com.microsoft.identity.common.java.exception.TerminalException;
import com.microsoft.identity.common.java.opentelemetry.SerializableSpanContext;
import com.microsoft.identity.common.java.opentelemetry.TextMapPropagatorExtension;
import com.microsoft.identity.common.logging.Logger;

import java.util.HashMap;
import java.util.Map;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import lombok.Getter;
import lombok.experimental.Accessors;

public class AuthorizationActivity extends DualScreenActivity {

    public static final String TAG = AuthorizationActivity.class.getSimpleName();
    @Getter
    @Accessors(prefix = "m")
    private SpanContext mSpanContext;

    @Getter
    @Accessors(prefix = "m")
    private Context mOtelContext;

    private AuthorizationFragment mFragment;

    public AuthorizationFragment getFragment() {
        return mFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String methodTag = TAG + ":onCreate";
        if (getIntent().getExtras() != null) {
            try {
                mSpanContext = new CommonMoshiJsonAdapter().fromJson(
                        getIntent().getExtras().getString(SerializableSpanContext.SERIALIZABLE_SPAN_CONTEXT),
                        SerializableSpanContext.class
                );
                // Extract OtelContext from the carrier if it exists
                final Map<String, String> carrier;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    carrier = getIntent().getExtras() != null ?
                            getIntent().getExtras().getSerializable(OTEL_CONTEXT_CARRIER, HashMap.class) :
                            new HashMap<>();
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, String> temp = getIntent().getExtras() != null ?
                            (Map<String, String>) getIntent().getExtras().getSerializable(OTEL_CONTEXT_CARRIER) :
                            new HashMap<>();
                    carrier = temp;
                }
                mOtelContext = TextMapPropagatorExtension.extract(carrier);

            } catch (final TerminalException e) {
                // Don't want to block any features if an error occurs during deserialization of the span context.
                mSpanContext = null;
            }
        }
        final Fragment fragment = AuthorizationActivityFactory.getAuthorizationFragmentFromStartIntent(getIntent());
        if (fragment instanceof AuthorizationFragment) {
            mFragment = (AuthorizationFragment) fragment;
            mFragment.setInstanceState(getIntent().getExtras());
        } else {
            final IllegalStateException ex = new IllegalStateException("Unexpected fragment type.");
            Logger.error(methodTag, "Did not receive AuthorizationFragment from factory", ex);
        }
        setFragment(mFragment);
    }
}
