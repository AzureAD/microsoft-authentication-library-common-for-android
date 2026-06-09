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

import com.microsoft.identity.common.R;

/**
 * Declares as a separate class so that we can specify attributes exclusively to :auth process
 * in AndroidManifest without overriding MSAL's (in case where MSAL and broker are shipped together).
 *
 * <p>Transition animations are suppressed via {@code BrokerAuthorizationActivityTheme} pinned in
 * the broker's manifest. {@link #getThemeResId()} returns the DualScreen variant so the theme
 * swap inside {@link com.microsoft.identity.common.internal.ui.DualScreenActivity#onCreate}
 * preserves that suppression.
 */
public class BrokerAuthorizationActivity extends AuthorizationActivity {

    @Override
    protected int getThemeResId() {
        return R.style.DualScreenBrokerAuthorizationActivityTheme;
    }
}
