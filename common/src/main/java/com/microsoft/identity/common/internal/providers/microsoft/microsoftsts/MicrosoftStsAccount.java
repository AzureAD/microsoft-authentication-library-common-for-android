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
package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import android.support.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;

import java.util.Map;

public class MicrosoftStsAccount extends MicrosoftAccount {

    private static final String TAG = MicrosoftStsAccount.class.getSimpleName();

    /**
     * Constructor of MicrosoftStsAccount.
     *
     * @param idToken IDToken
     * @param uid     String
     * @param uTid    String
     */
    public MicrosoftStsAccount(IDToken idToken, String uid, final String uTid) {
        super(idToken, uid, uTid);
        Logger.verbose(TAG, "Init: " + TAG);
    }

    /**
     * Creates an MicrosoftStsAccount based on the contents of the IDToken and based on the contents of the ClientInfo JSON returned as part of the TokenResponse.
     *
     * @param idToken    The IDToken for this Account.
     * @param clientInfo The ClientInfo for this Account.
     * @return The newly created MicrosoftStsAccount.
     */
    public static MicrosoftStsAccount create(@NonNull final IDToken idToken,
                                             @NonNull final ClientInfo clientInfo) {
        final String uid = clientInfo.getUid();
        final String uTid = clientInfo.getUtid();

        MicrosoftStsAccount acct = new MicrosoftStsAccount(idToken, uid, uTid);

        return acct;
    }

    @Override
    public String getAuthorityType() {
        return AUTHORITY_TYPE_V1_V2;
    }

    @Override
    protected String getDisplayableId(final Map<String, String> claims) {
        if (!StringExtensions.isNullOrBlank(claims.get(MicrosoftStsIdToken.PREFERRED_USERNAME))) {
            return claims.get(MicrosoftStsIdToken.PREFERRED_USERNAME);
        } else if (!StringExtensions.isNullOrBlank(claims.get(MicrosoftStsIdToken.EMAIL))) {
            return claims.get(MicrosoftStsIdToken.EMAIL);
        }

        return null;
    }

}
