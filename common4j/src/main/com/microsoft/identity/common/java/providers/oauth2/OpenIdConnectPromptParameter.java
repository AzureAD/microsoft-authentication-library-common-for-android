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

package com.microsoft.identity.common.java.providers.oauth2;

import java.util.Locale;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * The UI options that developer can pass during interactive token acquisition requests.
 */
public enum OpenIdConnectPromptParameter {

    /**
     * No prompt parameter will be injected into the request.
     */
    UNSET,

    /**
     * this value is not used currently, kept as a placeholder if required for usage in the future.
     */
    NONE,

    /**
     * acquireToken will send prompt=select_account to the authorize endpoint. Shows a list of users from which can be
     * selected for authentication.
     */
    SELECT_ACCOUNT,

    /**
     * acquireToken will send prompt=login to the authorize endpoint.  The user will always be prompted for credentials by the service.
     * <p>
     * toString override is to enable the correct protocol value of login to be returned instead of "force_login".
     */
    LOGIN,

    /**
     * acquireToken will send prompt=consent to the authorize endpoint.  The user will be prompted to consent even if consent was granted before.
     */
    CONSENT,

    /**
     * acquireToken will send prompt=create to the /authorize endpoint.  The user will be prompted to create a new account.
     * Requires configuring authority as type "AzureADMyOrg" with a tenant_id.
     * <p>
     * Prerequisite: https://docs.microsoft.com/en-us/azure/active-directory/external-identities/self-service-sign-up-user-flow
     */
    CREATE;

    @Override
    public String toString() {
        if (this == UNSET) {
            return "";
        }

        return this.name().toLowerCase(Locale.ROOT);
    }


    /**
     * Utility method to map Adal PromptBehavior with OpenIdConnectPromptParameter
     *
     * @param promptBehavior
     * @return
     */
    public static OpenIdConnectPromptParameter _fromPromptBehavior(@Nullable final String promptBehavior) {

        return promptBehavior != null && promptBehavior.equals("FORCE_PROMPT") ?
                OpenIdConnectPromptParameter.LOGIN :
                OpenIdConnectPromptParameter.UNSET;
    }
}