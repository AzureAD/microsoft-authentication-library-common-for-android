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
package com.microsoft.identity.common.java.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.java.util.SchemaUtil;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.java.providers.oauth2.IDToken;
import com.microsoft.identity.common.java.logging.Logger;

import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@EqualsAndHashCode(callSuper = true)
public class MicrosoftStsAccount extends MicrosoftAccount {

    private static final String TAG = MicrosoftStsAccount.class.getSimpleName();

    /**
     * Constructor of MicrosoftStsAccount.
     */
    public MicrosoftStsAccount() {
        super();
        Logger.verbose(TAG, "Init: " + TAG);
    }

    /**
     * Constructor of MicrosoftStsAccount.
     *
     * @param idToken    IDToken
     * @param clientInfo clientInfo
     */
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    public MicrosoftStsAccount(@NonNull final IDToken idToken,
                               @NonNull final ClientInfo clientInfo) {
        super(idToken, clientInfo);
        Logger.verbose(TAG, "Init: " + TAG);
    }

    @Override
    public String getAuthorityType() {
        return AUTHORITY_TYPE_MS_STS;
    }

    @Override
    protected String getDisplayableIdFromClaims(@NonNull final Map<String, ?> claims) {
        return SchemaUtil.getDisplayableId(claims);
    }

}
