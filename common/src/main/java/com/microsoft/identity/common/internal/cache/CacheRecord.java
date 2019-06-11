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

import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;

public class CacheRecord implements ICacheRecord {

    private AccountRecord mAccount;
    private AccessTokenRecord mAccessToken;
    private RefreshTokenRecord mRefreshToken;
    private IdTokenRecord mIdToken;
    private IdTokenRecord mV1IdToken;

    public void setAccount(final AccountRecord account) {
        mAccount = account;
    }

    @Override
    public AccountRecord getAccount() {
        return mAccount;
    }

    public void setAccessToken(final AccessTokenRecord accesToken) {
        mAccessToken = accesToken;
    }

    @Override
    public AccessTokenRecord getAccessToken() {
        return mAccessToken;
    }

    public void setRefreshToken(final RefreshTokenRecord refreshToken) {
        mRefreshToken = refreshToken;
    }

    @Override
    public RefreshTokenRecord getRefreshToken() {
        return mRefreshToken;
    }

    public void setIdToken(final IdTokenRecord idToken) {
        mIdToken = idToken;
    }

    @Override
    public IdTokenRecord getIdToken() {
        return mIdToken;
    }

    public void setV1IdToken(final IdTokenRecord idToken) {
        mV1IdToken = idToken;
    }

    @Override
    public IdTokenRecord getV1IdToken() {
        return mV1IdToken;
    }

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CacheRecord that = (CacheRecord) o;

        if (mAccount != null ? !mAccount.equals(that.mAccount) : that.mAccount != null)
            return false;
        if (mAccessToken != null ? !mAccessToken.equals(that.mAccessToken) : that.mAccessToken != null)
            return false;
        if (mRefreshToken != null ? !mRefreshToken.equals(that.mRefreshToken) : that.mRefreshToken != null)
            return false;
        return mIdToken != null ? mIdToken.equals(that.mIdToken) : that.mIdToken == null;
    }
    //CHECKSTYLE:ON

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public int hashCode() {
        int result = mAccount != null ? mAccount.hashCode() : 0;
        result = 31 * result + (mAccessToken != null ? mAccessToken.hashCode() : 0);
        result = 31 * result + (mRefreshToken != null ? mRefreshToken.hashCode() : 0);
        result = 31 * result + (mIdToken != null ? mIdToken.hashCode() : 0);
        return result;
    }
    //CHECKSTYLE:ON

}
