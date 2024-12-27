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
package com.microsoft.identity.common.internal.msafederation.google

import android.app.Activity
import com.microsoft.identity.common.internal.msafederation.FederatedSignInParameters
import com.microsoft.identity.common.internal.msafederation.FederatedSignInProviderName

/**
 * SignInWithGoogleParameters holds the parameters required for signing in with Google.
 *
 * @property activity The Activity context used for the sign-in process.
 * UI for the sign-in process.
 */
data class SignInWithGoogleParameters(
    internal val activity: Activity
) : FederatedSignInParameters() {

    /**
     * Secondary constructor to initialize the parameters with an option to use a bottom sheet UI.
     * Current requirement do not use bottom sheet UI, so this constructor is kept internal
     * @param activity The Activity context used for the sign-in process.
     * @param useBottomSheet A flag indicating whether to use a bottom sheet UI for the sign-in process.
     */
    internal constructor(activity: Activity, useBottomSheet: Boolean) : this(activity) {
        this.useBottomSheet = useBottomSheet
    }

    /**
     * A flag indicating whether to use a bottom sheet UI for the sign-in process.
     */
    internal var useBottomSheet: Boolean = false

    /**
     * The provider type for the federated sign-in, which is Google in this case.
     */
    override val providerName: FederatedSignInProviderName
        get() = FederatedSignInProviderName.GOOGLE
}
