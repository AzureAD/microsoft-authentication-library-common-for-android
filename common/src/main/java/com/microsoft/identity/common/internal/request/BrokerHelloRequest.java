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
package com.microsoft.identity.common.internal.request;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CLIENT_ADVERTISED_MAXIMUM_BP_VERSION_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CLIENT_CONFIGURED_MINIMUM_BP_VERSION_KEY;


public class BrokerHelloRequest implements Serializable {

    @NonNull
    @SerializedName(CLIENT_ADVERTISED_MAXIMUM_BP_VERSION_KEY)
    private String mClientAdvertisedMaximumKey;

    @Nullable
    @SerializedName(CLIENT_CONFIGURED_MINIMUM_BP_VERSION_KEY)
    private String mClientConfiguredMinimumKey;

    @NonNull
    public String getClientAdvertisedMaximumKey() {
        return mClientAdvertisedMaximumKey;
    }

    public void setClientAdvertisedMaximumKey(@NonNull String clientAdvertisedMaximumKey) {
        mClientAdvertisedMaximumKey = clientAdvertisedMaximumKey;
    }

    @Nullable
    public String getClientConfiguredMinimumKey() {
        return mClientConfiguredMinimumKey;
    }

    public void setClientConfiguredMinimumKey(@Nullable String clientConfiguredMinimumKey) {
        this.mClientConfiguredMinimumKey = clientConfiguredMinimumKey;
    }


}
