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
package com.microsoft.identity.common.internal.cache;

import android.content.Context;

import com.microsoft.identity.common.BaseAccount;
import com.microsoft.identity.common.adal.internal.cache.IStorageHelper;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.dto.AccountCredentialBase;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.events.CacheEndEvent;
import com.microsoft.identity.common.internal.telemetry.events.CacheStartEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.microsoft.identity.common.exception.ErrorStrings.ACCOUNT_IS_SCHEMA_NONCOMPLIANT;
import static com.microsoft.identity.common.exception.ErrorStrings.CREDENTIAL_IS_SCHEMA_NONCOMPLIANT;
import static com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache.DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES;
import static com.microsoft.identity.common.internal.dto.CredentialType.ID_TOKEN_TYPES;

/**
 * Sub class of {@link MsalCppOAuth2TokenCache} to add specific public api's required for MSAL CPP library.
 */
public class MsalCppOAuth2TokenCache
        <GenericOAuth2Strategy extends OAuth2Strategy,
                GenericAuthorizationRequest extends AuthorizationRequest,
                GenericTokenResponse extends TokenResponse,
                GenericAccount extends BaseAccount,
                GenericRefreshToken extends com.microsoft.identity.common.internal.providers.oauth2.RefreshToken>
        extends MsalOAuth2TokenCache<GenericOAuth2Strategy, GenericAuthorizationRequest, GenericTokenResponse, GenericAccount, GenericRefreshToken> {


    /**
     * Constructor of MsalOAuth2TokenCache.
     *
     * @param context                  Context
     * @param accountCredentialCache   IAccountCredentialCache
     * @param accountCredentialAdapter IAccountCredentialAdapter
     */
    private MsalCppOAuth2TokenCache(final Context context,
                                   final IAccountCredentialCache accountCredentialCache,
                                   final IAccountCredentialAdapter accountCredentialAdapter) {
        super(context, accountCredentialCache, accountCredentialAdapter);
    }

    /**
     * Factory method for creating an instance of MsalCppOAuth2TokenCache
     * <p>
     * NOTE: Currently this is configured for AAD v2 as the only IDP
     *
     * @param context The Application Context
     * @return An instance of the MsalCppOAuth2TokenCache.
     */
    public static MsalCppOAuth2TokenCache create(@NonNull final Context context){
        final MsalOAuth2TokenCache msalOAuth2TokenCache = MsalOAuth2TokenCache.create(context);
        return new MsalCppOAuth2TokenCache(
                context,
                msalOAuth2TokenCache.getAccountCredentialCache(),
                msalOAuth2TokenCache.getAccountCredentialAdapter()
        );
    }

    /**
     * @param accountRecord : AccountRecord associated with the input credentials.
     * @param credentials   : list of Credential which can include AccessTokenRecord, IdTokenRecord and RefreshTokenRecord.
     *                      Note : Both IdTokenRecord and RefreshTokenRecord need to be non null. AccessTokenRecord can be optional.
     * @throws ClientException : If the supplied Account or Credential are null or schema invalid.
     */
    public void saveCredentials(@NonNull final AccountRecord accountRecord,
                                @NonNull final Credential... credentials) throws ClientException {
        if (credentials == null || credentials.length == 0) {
            throw new ClientException("Credential array passed in is null or empty");
        }

        AccessTokenRecord accessTokenRecord = null;
        IdTokenRecord idTokenRecord = null;
        RefreshTokenRecord refreshTokenRecord = null;

        for (final Credential credential : credentials) {
            if (credential instanceof AccessTokenRecord) {
                accessTokenRecord = (AccessTokenRecord) credential;
            } else if (credential instanceof IdTokenRecord) {
                idTokenRecord = (IdTokenRecord) credential;
            } else if (credential instanceof RefreshTokenRecord) {
                refreshTokenRecord = (RefreshTokenRecord) credential;
            }
        }

        validateNonNull(accountRecord, "AccountRecord");
        validateNonNull(refreshTokenRecord, "RefreshTokenRecord");
        validateNonNull(idTokenRecord, "IdTokenRecord");
        validateCacheArtifacts(accountRecord, accessTokenRecord, refreshTokenRecord, idTokenRecord);

        removeRefreshTokenIfNeeded(accountRecord, refreshTokenRecord);

        saveCredentialsInternal(credentials);
    }

}
