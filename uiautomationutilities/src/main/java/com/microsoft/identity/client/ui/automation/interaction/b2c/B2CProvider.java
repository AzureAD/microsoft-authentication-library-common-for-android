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
import com.microsoft.identity.internal.testutils.labutils.LabConstants;

public enum B2CProvider {

    Google(LabConstants.B2CProvider.GOOGLE, "GoogleExchange", "google.com"),
    Facebook(LabConstants.B2CProvider.FACEBOOK, "FacebookExchange", "facebook.com"),
    MSA(LabConstants.B2CProvider.MICROSOFT, "MicrosoftAccountExchange", "live.com"),
    Local(LabConstants.B2CProvider.LOCAL, null, null);

    private final static String TAG = "B2CProvider";
    private final String providerName;

    @Nullable // should be null for LOCAL B2C provider
    private final String idpSelectionBtnResourceId;

    @Nullable // should be null for LOCAL B2C provider
    private final String domainHint; // this can be used as query param to /authorize endpoint

    B2CProvider(@NonNull final String providerName,
                @Nullable final String idpSelectionBtnResourceId,
                @Nullable final String domainHint) {
        Logger.i(TAG, "Initializing B2CProvider for " + idpSelectionBtnResourceId + " ..");
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
