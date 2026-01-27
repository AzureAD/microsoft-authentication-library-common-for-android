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
import androidx.fragment.app.FragmentActivity;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.internal.util.CommonMoshiJsonAdapter;
import com.microsoft.identity.common.java.exception.TerminalException;
import com.microsoft.identity.common.java.opentelemetry.SerializableSpanContext;
import com.microsoft.identity.common.java.opentelemetry.TextMapPropagatorExtension;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.logging.Logger;

import java.util.HashMap;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import lombok.Getter;
import lombok.experimental.Accessors;

public class AuthorizationActivity extends FragmentActivity {

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
        final Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            try {
                String spanContextJson = bundle.getString(SerializableSpanContext.SERIALIZABLE_SPAN_CONTEXT);
                mSpanContext = StringUtil.isNullOrEmpty(spanContextJson) ? null : new CommonMoshiJsonAdapter().fromJson(
                        spanContextJson,
                        SerializableSpanContext.class
                );

                final HashMap<String, String> carrier;
                // For Android Tiramisu and above, we use the new Serializable interface, which is a bit safer because it performs type checking at the framework level.
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    carrier = bundle.getSerializable(OTEL_CONTEXT_CARRIER, HashMap.class);
                } else {
                    carrier = (HashMap<String, String>) bundle.getSerializable(OTEL_CONTEXT_CARRIER);
                }
                mOtelContext = TextMapPropagatorExtension.extract(carrier);
            } catch (final TerminalException | ClassCastException | NullPointerException e) {
                // Don't want to block any features if an error occurs during deserialization.
                Logger.error(methodTag, "Exception thrown during extraction: " + e.getMessage(), e);
            }
        }
        setContentView(R.layout.common_activity_content);
        final Fragment fragment = AuthorizationActivityFactory.getAuthorizationFragmentFromStartIntent(getIntent());
        if (fragment instanceof AuthorizationFragment) {
            mFragment = (AuthorizationFragment) fragment;
            mFragment.setInstanceState(bundle);
        } else {
            final IllegalStateException ex = new IllegalStateException("Unexpected fragment type.");
            Logger.error(methodTag, "Did not receive AuthorizationFragment from factory", ex);
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, mFragment)
                .commit();
    }
}
