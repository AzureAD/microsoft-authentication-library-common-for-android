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
package com.microsoft.identity.client.ui.automation.interaction.b2c;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.labapi.utilities.constants.B2CProvider;


public enum B2CProviderWrapper {

    Google(B2CProvider.GOOGLE.toString(), "GoogleExchange", "google.com"),
    Facebook(B2CProvider.FACEBOOK.toString(), "FacebookExchange", "facebook.com"),
    MSA(B2CProvider.MICROSOFT.toString(), "MicrosoftAccountExchange", "live.com"),
    Local(B2CProvider.LOCAL.toString(), null, null);

    private final static String TAG = "B2CProviderWrapper";
    private final String providerName;

    @Nullable // should be null for LOCAL B2C provider
    private final String idpSelectionBtnResourceId;

    @Nullable // should be null for LOCAL B2C provider
    private final String domainHint; // this can be used as query param to /authorize endpoint

    B2CProviderWrapper(@NonNull final String providerName,
                       @Nullable final String idpSelectionBtnResourceId,
                       @Nullable final String domainHint) {
        Logger.i(TAG, "Initializing B2CProviderWrapper for " + idpSelectionBtnResourceId + " ..");
        this.providerName = providerName;
        this.idpSelectionBtnResourceId = idpSelectionBtnResourceId;
        this.domainHint = domainHint;
    }

    public String getProviderName() {
        return providerName;
    }

    @Nullable
    public String getIdpSelectionBtnResourceId() {
        return idpSelectionBtnResourceId;
    }

    @Nullable
    public String getDomainHint() {
        return domainHint;
    }
}
