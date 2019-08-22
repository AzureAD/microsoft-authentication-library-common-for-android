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
package com.microsoft.identity.common.internal.providers.microsoft;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.cache.SchemaUtil;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;

public class MicrosoftRefreshToken extends RefreshToken {

    private ClientInfo mClientInfo;
    private String mFamilyId;
    private String mScope;
    private String mClientId;
    private String mEnvironment;

    public MicrosoftRefreshToken(final String rawRefreshToken,
                                 final ClientInfo clientInfo,
                                 final String scope,
                                 final String clientId,
                                 final String environment,
                                 @Nullable String familyId) {
        super(rawRefreshToken);
        mClientInfo = clientInfo;
        mScope = scope;
        mClientId = clientId;
        mEnvironment = environment;
        mFamilyId = familyId;
    }

    /**
     * Constructs a new MicrosoftRefreshToken instance.
     *
     * @param tokenResponse The {@link MicrosoftTokenResponse} from which to construct this rt.
     */
    public MicrosoftRefreshToken(@NonNull final MicrosoftTokenResponse tokenResponse) {
        super(tokenResponse);
        try {
            mClientInfo = new ClientInfo(tokenResponse.getClientInfo());
            mFamilyId = tokenResponse.getFamilyId();
            mScope = tokenResponse.getScope();
            mClientId = tokenResponse.getClientId();
        } catch (ServiceException e) {
            // TODO handle this properly
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getHomeAccountId() {
        return SchemaUtil.getHomeAccountId(mClientInfo);
    }

    public void setEnvironment(final String environment) {
        mEnvironment = environment;
    }

    @Override
    public String getEnvironment() {
        return mEnvironment;
    }

    @Override
    public String getClientId() {
        return mClientId;
    }

    @Override
    public String getSecret() {
        return getRefreshToken();
    }

    @Override
    public String getTarget() {
        return mScope;
    }

    @Override
    public String getFamilyId() {
        return mFamilyId;
    }

    /**
     * @return true if this token is family refresh token, false otherwise.
     */
    public boolean getIsFamilyRefreshToken() {
        return !TextUtils.isEmpty(mFamilyId);
    }

    /**
     * Gets the {@link ClientInfo}.
     *
     * @return The ClientInfo to get.
     */
    public ClientInfo getClientInfo() {
        return mClientInfo;
    }
}
