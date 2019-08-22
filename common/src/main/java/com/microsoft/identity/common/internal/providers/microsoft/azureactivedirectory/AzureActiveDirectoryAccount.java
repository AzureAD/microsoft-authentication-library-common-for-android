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
package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;

import java.util.Map;

/**
 * Inherits from account and implements the getUniqueIdentifier method for returning a unique identifier for an AAD User UTID, UID combined as a single identifier per current MSAL implementation.
 */
public class AzureActiveDirectoryAccount extends MicrosoftAccount {

    private static final String TAG = AzureActiveDirectoryAccount.class.getSimpleName();
    /**
     * Identity provide is a claim for V1 specific.
     */
    private String mIdentityProvider;

    /**
     * Constructor of AzureActiveDirectoryAccount.
     */
    public AzureActiveDirectoryAccount() {
        super();
    }

    /**
     * Constructor for AzureActiveDirectoryAccount object.
     *
     * @param idToken    Returned as part of the TokenResponse
     * @param clientInfo Returned via TokenResponse
     */
    public AzureActiveDirectoryAccount(@NonNull final IDToken idToken,
                                       @NonNull final ClientInfo clientInfo) {
        super(idToken, clientInfo);
        final Map<String, ?> claims = idToken.getTokenClaims();
        mIdentityProvider = (String) claims.get(AzureActiveDirectoryIdToken.IDENTITY_PROVIDER);
        Logger.verbose(TAG, "Init: " + TAG);
    }

    /**
     * Sets the identity provider.
     *
     * @param idp The identity provider to set.
     */
    public void setIdentityProvider(final String idp) {
        mIdentityProvider = idp;
    }

    /**
     * @return The identity provider of the user authenticated. Can be null if not returned from the service.
     */
    public String getIdentityProvider() {
        return mIdentityProvider;
    }

    @Override
    public String getAuthorityType() {
        return AUTHORITY_TYPE_V1_V2;
    }

    @Override
    protected String getDisplayableId(Map<String, ?> claims) {
        final String methodName = "getDisplayableId";

        String displayableId = null;

        if (!StringExtensions.isNullOrBlank((String) claims.get(AzureActiveDirectoryIdToken.UPN))) {
            Logger.info(TAG + ":" + methodName, "Returning upn as displayableId");
            displayableId = (String) claims.get(AzureActiveDirectoryIdToken.UPN);
        } else if (!StringExtensions.isNullOrBlank((String) claims.get(AzureActiveDirectoryIdToken.EMAIL))) {
            Logger.info(TAG + ":" + methodName, "Returning email as displayableId");
            displayableId = (String) claims.get(AzureActiveDirectoryIdToken.EMAIL);
        }

        return displayableId;
    }

    @Override
    public String toString() {
        return "AzureActiveDirectoryAccount{} " + super.toString() +
                ", mIdentityProvider='" + mIdentityProvider + '\'';
    }
}
