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

import android.content.Context;

import com.microsoft.identity.common.exception.ArgumentException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;

import java.util.ArrayList;
import java.util.Arrays;

public class OperationParameters {

    private static final String TAG = OperationParameters.class.getSimpleName();


    private transient Context mAppContext;
    private transient OAuth2TokenCache mTokenCache;
    // Instantiate with default scopes by default
    private ArrayList<String> mScopes = getDefaultScopes();
    protected IAccountRecord mAccount;
    private String clientId;
    private String redirectUri;
    private Authority mAuthority;
    private String mClaimsRequestJson;

    public Context getAppContext() {
        return mAppContext;
    }

    public void setAppContext(Context mAppContext) {
        this.mAppContext = mAppContext;
    }

    public ArrayList<String> getScopes() {
        // Remove empty and null scopes
        mScopes.removeAll(Arrays.asList("", null));
        return mScopes;
    }

    public void setScopes(final ArrayList<String> scopes) {
        // avoid adding duplicate scopes
        for(String scope : scopes){
            if(!mScopes.contains(scope)){
                mScopes.add(scope);
            }
        }
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public void setTokenCache(OAuth2TokenCache cache) {
        this.mTokenCache = cache;
    }

    public OAuth2TokenCache getTokenCache() {
        return mTokenCache;
    }

    public Authority getAuthority() {
        return mAuthority;
    }

    public void setAuthority(Authority authority) {
        this.mAuthority = authority;
    }

    public void setAccount(final IAccountRecord account) {
        mAccount = account;
    }

    public IAccountRecord getAccount() {
        return mAccount;
    }

    public String getClaimsRequestJson() {
        return mClaimsRequestJson;
    }

    public void setClaimsRequest(String claimsRequestJson) {
        mClaimsRequestJson = claimsRequestJson;
    }

    private static ArrayList<String> getDefaultScopes() {
        final ArrayList<String> defaultScopes = new ArrayList<>();
        defaultScopes.add("openid");
        defaultScopes.add("profile");
        defaultScopes.add("offline_access");
        return defaultScopes;
    }


    /**
     * Since this is about validating MSAL Parameters and not an authorization request or token request.  I've placed this here.
     */
    public void validate() throws ArgumentException {
        final String methodName = ":validate";
        Logger.verbose(
                TAG + methodName,
                "Validating operation params..."
        );
        Boolean validScopeArgument = false;

        if (mScopes != null) {
            mScopes.removeAll(Arrays.asList("", null));
            if (mScopes.size() > 3) {
                validScopeArgument = true;
            }
        }

        if (!validScopeArgument) {
            if (this instanceof AcquireTokenSilentOperationParameters) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                        ArgumentException.SCOPE_ARGUMENT_NAME,
                        "scope is empty or null"
                );
            }
            if (this instanceof AcquireTokenOperationParameters) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME,
                        ArgumentException.SCOPE_ARGUMENT_NAME,
                        "scope is empty or null");
            }
        }

    }

}
