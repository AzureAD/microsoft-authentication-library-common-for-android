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
package com.microsoft.identity.internal.testutils

import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAudience.MSA_MEGA_TENANT_ID
import com.microsoft.identity.common.java.dto.AccessTokenRecord
import com.microsoft.identity.common.java.dto.AccountRecord
import com.microsoft.identity.common.java.dto.IdTokenRecord
import com.microsoft.identity.common.java.dto.RefreshTokenRecord
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAccount.AUTHORITY_TYPE_MS_STS
import com.microsoft.identity.internal.testutils.mocks.MockTokenCreator.createMockRawClientInfo
import java.util.concurrent.TimeUnit

object MockRecords {

    private const val MOCK_NAME = "MOCK_NAME"
    private const val MOCK_MSA_USERNAME = "mock@outlook.com"
    private const val MOCK_UID = "00000000-0000-0000-0000-000123456789"
    private const val MOCK_ENVIRONMENT = "login.windows.net"
    private const val MOCK_CLIENT_ID = "12345678-0000-0000-0000-000123456789"
    private const val MOCK_MSA_AUTHORITY = "https://login.microsoftonline.com/$MSA_MEGA_TENANT_ID/oAuth2/v2.0/token"
    private const val MOCK_SECRET = "TOKEN_SECRET"
    private const val MOCK_APPLICATION_IDENTIFIER = "com.msft.identity.client.sample.local/xxAk8S05zu0Nkce+X2J6IKJ2e7YE4F9ZorZj0YnYUQ2vw8vLc8VGGOqJdTnVySbbcy9VY8UDbOfeOETSErYllw=="
    private const val MOCK_TARGET = "User.Read openid profile"
    private const val MOCK_LOCAL_GUEST_UID = "b985a3e2-457e-4d38-8f1d-354a949b802e"
    private const val MOCK_AAD_TENANT_ID = "f645ad92-e38d-4d1a-b510-d1b09a74a8ca"
    private const val MOCK_MSA_GUEST_AUTHORITY = "https://login.microsoftonline.com/$MOCK_AAD_TENANT_ID/oAuth2/v2.0/token"
    private const val MOCK_AAD_ORGANIZATION_AUTHORITY = "https://login.microsoftonline.com/organizations/oAuth2/v2.0/token"

    //region MSA
    fun getMockAccountRecord_MSA(): AccountRecord {
        val accountRecord = AccountRecord()
        accountRecord.authorityType = AUTHORITY_TYPE_MS_STS
        accountRecord.clientInfo = createMockRawClientInfo(MOCK_UID, MSA_MEGA_TENANT_ID)
        accountRecord.environment = MOCK_ENVIRONMENT
        accountRecord.homeAccountId = getHomeAccountId(MOCK_UID, MSA_MEGA_TENANT_ID)
        accountRecord.localAccountId = MOCK_UID
        accountRecord.name = MOCK_NAME
        accountRecord.realm = MSA_MEGA_TENANT_ID
        accountRecord.username = MOCK_MSA_USERNAME
        return accountRecord
    }

    fun getMockIdTokenRecord_MSA(): IdTokenRecord {
        val idToken = IdTokenRecord()
        idToken.authority = MOCK_MSA_AUTHORITY
        idToken.realm = MSA_MEGA_TENANT_ID
        idToken.clientId = MOCK_CLIENT_ID
        idToken.credentialType = "IdToken"
        idToken.environment = MOCK_ENVIRONMENT
        idToken.homeAccountId = getHomeAccountId(MOCK_UID, MSA_MEGA_TENANT_ID)
        idToken.secret = MOCK_SECRET
        return idToken
    }

    fun getMockAccessTokenRecord_MSA(): AccessTokenRecord {
        val accessTokenRecord = AccessTokenRecord()
        accessTokenRecord.accessTokenType = "bearer"
        accessTokenRecord.applicationIdentifier = MOCK_APPLICATION_IDENTIFIER
        accessTokenRecord.authority = MOCK_MSA_AUTHORITY
        accessTokenRecord.expiresOn = getMockExpiresOn()
        accessTokenRecord.realm = MSA_MEGA_TENANT_ID
        accessTokenRecord.target = MOCK_TARGET
        accessTokenRecord.cachedAt = getMockCachedAt()
        accessTokenRecord.clientId = MOCK_CLIENT_ID
        accessTokenRecord.credentialType = "AccessToken"
        accessTokenRecord.environment = MOCK_ENVIRONMENT
        accessTokenRecord.homeAccountId = getHomeAccountId(MOCK_UID, MSA_MEGA_TENANT_ID)
        accessTokenRecord.secret = MOCK_SECRET
        return accessTokenRecord
    }

    fun getMockRefreshTokenRecord_MSA(): RefreshTokenRecord {
        val refreshTokenRecord = RefreshTokenRecord()
        refreshTokenRecord.familyId = "1"
        refreshTokenRecord.target = MOCK_TARGET
        refreshTokenRecord.cachedAt = getMockCachedAt()
        refreshTokenRecord.clientId = MOCK_CLIENT_ID
        refreshTokenRecord.credentialType = "RefreshToken"
        refreshTokenRecord.environment = MOCK_ENVIRONMENT
        refreshTokenRecord.homeAccountId = getHomeAccountId(MOCK_UID, MSA_MEGA_TENANT_ID)
        refreshTokenRecord.secret = MOCK_SECRET
        return refreshTokenRecord
    }
    //endregion

    //region MSAPassthrough
    fun getMockAccountRecord_MSAPassthrough(): AccountRecord {
        val accountRecord = AccountRecord()
        accountRecord.authorityType = AUTHORITY_TYPE_MS_STS
        accountRecord.clientInfo = createMockRawClientInfo(MOCK_UID, MSA_MEGA_TENANT_ID)
        accountRecord.environment = MOCK_ENVIRONMENT
        accountRecord.homeAccountId = getHomeAccountId(MOCK_UID, MSA_MEGA_TENANT_ID)
        accountRecord.localAccountId = MOCK_LOCAL_GUEST_UID
        accountRecord.name = MOCK_NAME
        accountRecord.realm = MOCK_AAD_TENANT_ID
        accountRecord.username = MOCK_MSA_USERNAME
        return accountRecord
    }

    fun getMockIdTokenRecord_MSAPassthrough(): IdTokenRecord {
        val idToken = IdTokenRecord()
        idToken.authority = MOCK_MSA_GUEST_AUTHORITY
        idToken.realm = MOCK_AAD_TENANT_ID
        idToken.clientId = MOCK_CLIENT_ID
        idToken.credentialType = "IdToken"
        idToken.environment = MOCK_ENVIRONMENT
        idToken.homeAccountId = getHomeAccountId(MOCK_UID, MSA_MEGA_TENANT_ID)
        idToken.secret = MOCK_SECRET
        return idToken
    }

    fun getMockAccessTokenRecord_MSAPassthrough(): AccessTokenRecord {
        val accessTokenRecord = AccessTokenRecord()
        accessTokenRecord.accessTokenType = "bearer"
        accessTokenRecord.applicationIdentifier = MOCK_APPLICATION_IDENTIFIER
        accessTokenRecord.authority = MOCK_MSA_GUEST_AUTHORITY
        accessTokenRecord.expiresOn = getMockExpiresOn()
        accessTokenRecord.realm = MOCK_AAD_TENANT_ID
        accessTokenRecord.target = MOCK_TARGET
        accessTokenRecord.cachedAt = getMockCachedAt()
        accessTokenRecord.clientId = MOCK_CLIENT_ID
        accessTokenRecord.credentialType = "AccessToken"
        accessTokenRecord.environment = MOCK_ENVIRONMENT
        accessTokenRecord.homeAccountId = getHomeAccountId(MOCK_UID, MSA_MEGA_TENANT_ID)
        accessTokenRecord.secret = MOCK_SECRET
        return accessTokenRecord
    }

    fun getMockRefreshTokenRecord_MSAPassthrough(): RefreshTokenRecord {
        val refreshTokenRecord = RefreshTokenRecord()
        refreshTokenRecord.familyId = "1"
        refreshTokenRecord.target = MOCK_TARGET
        refreshTokenRecord.cachedAt = getMockCachedAt()
        refreshTokenRecord.clientId = MOCK_CLIENT_ID
        refreshTokenRecord.credentialType = "RefreshToken"
        refreshTokenRecord.environment = MOCK_ENVIRONMENT
        refreshTokenRecord.homeAccountId = getHomeAccountId(MOCK_UID, MSA_MEGA_TENANT_ID)
        refreshTokenRecord.secret = MOCK_SECRET
        return refreshTokenRecord
    }
    //endregion

    //region AAD
    fun getMockAccountRecord_AAD(): AccountRecord {
        val accountRecord = AccountRecord()
        accountRecord.authorityType = AUTHORITY_TYPE_MS_STS
        accountRecord.clientInfo = createMockRawClientInfo(MOCK_UID, MOCK_AAD_TENANT_ID)
        accountRecord.environment = MOCK_ENVIRONMENT
        accountRecord.homeAccountId = getHomeAccountId(MOCK_UID, MOCK_AAD_TENANT_ID)
        accountRecord.localAccountId = MOCK_UID
        accountRecord.name = MOCK_NAME
        accountRecord.realm = MOCK_AAD_TENANT_ID
        accountRecord.username = MOCK_MSA_USERNAME
        return accountRecord
    }

    fun getMockIdTokenRecord_AAD(): IdTokenRecord {
        val idToken = IdTokenRecord()
        idToken.authority = MOCK_AAD_ORGANIZATION_AUTHORITY
        idToken.realm = MOCK_AAD_TENANT_ID
        idToken.clientId = MOCK_CLIENT_ID
        idToken.credentialType = "IdToken"
        idToken.environment = MOCK_ENVIRONMENT
        idToken.homeAccountId = getHomeAccountId(MOCK_UID, MOCK_AAD_TENANT_ID)
        idToken.secret = MOCK_SECRET
        return idToken
    }

    fun getMockAccessTokenRecord_AAD(): AccessTokenRecord {
        val accessTokenRecord = AccessTokenRecord()
        accessTokenRecord.accessTokenType = "bearer"
        accessTokenRecord.applicationIdentifier = MOCK_APPLICATION_IDENTIFIER
        accessTokenRecord.authority = MOCK_AAD_ORGANIZATION_AUTHORITY
        accessTokenRecord.expiresOn = getMockExpiresOn()
        accessTokenRecord.realm = MOCK_AAD_TENANT_ID
        accessTokenRecord.target = MOCK_TARGET
        accessTokenRecord.cachedAt = getMockCachedAt()
        accessTokenRecord.clientId = MOCK_CLIENT_ID
        accessTokenRecord.credentialType = "AccessToken"
        accessTokenRecord.environment = MOCK_ENVIRONMENT
        accessTokenRecord.homeAccountId = getHomeAccountId(MOCK_UID, MOCK_AAD_TENANT_ID)
        accessTokenRecord.secret = MOCK_SECRET
        return accessTokenRecord
    }

    fun getMockRefreshTokenRecord_AAD(): RefreshTokenRecord {
        val refreshTokenRecord = RefreshTokenRecord()
        refreshTokenRecord.familyId = "1"
        refreshTokenRecord.target = MOCK_TARGET
        refreshTokenRecord.cachedAt = getMockCachedAt()
        refreshTokenRecord.clientId = MOCK_CLIENT_ID
        refreshTokenRecord.credentialType = "RefreshToken"
        refreshTokenRecord.environment = MOCK_ENVIRONMENT
        refreshTokenRecord.homeAccountId = getHomeAccountId(MOCK_UID, MOCK_AAD_TENANT_ID)
        refreshTokenRecord.secret = MOCK_SECRET
        return refreshTokenRecord
    }
    //endregion

    fun getHomeAccountId(uid: String, utid:String) : String {
        return "$uid:$utid"
    }

    fun getMockExpiresOn(): String {
        return (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + 100000).toString()
    }

    fun getMockCachedAt(): String {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()).toString()
    }
}