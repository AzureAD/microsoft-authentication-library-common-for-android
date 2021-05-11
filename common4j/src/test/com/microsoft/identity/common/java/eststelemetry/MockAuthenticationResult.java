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

package com.microsoft.identity.common.java.eststelemetry;

import com.microsoft.identity.common.java.result.ILocalAuthenticationResult;

import java.util.Date;

import lombok.Builder;
import lombok.NonNull;

@Builder
public class MockAuthenticationResult implements ILocalAuthenticationResult {

    @Builder.Default
    String accessToken = "MOCK_ACCESS_TOKEN";

    @Builder.Default
    Date date = new Date();

    @Builder.Default
    String tenantId = "MOCK_TENANT_ID";

    @Builder.Default
    String refreshToken = "MOCK_REFRESH_TOKEN";

    @Builder.Default
    String idToken = "MOCK_ID_TOKEN";

    @Builder.Default
    String uniqueId = "MOCK_UNIQUE_ID";

    @Builder.Default
    String[] scope = new String[0];

    @Builder.Default
    String speRing = "MOCK_SPE_RING";

    @Builder.Default
    String refreshTokenAge = "MOCK_REFRESH_TOKEN_AGE";

    @Builder.Default
    String familyId = "MOCK_FAMILY_ID";

    @Builder.Default
    String correlationId = "MOCK_CORRELATION_ID";

    @Builder.Default
    boolean isServicedFromCache = false;

    @Override
    public @NonNull String getAccessToken() {
        return accessToken;
    }

    @Override
    public @NonNull Date getExpiresOn() {
        return date;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public @NonNull String getUniqueId() {
        return uniqueId;
    }

    @Override
    public @NonNull String getRefreshToken() {
        return refreshToken;
    }

    @Override
    public String getIdToken() {
        return idToken;
    }

    @Override
    public @NonNull String[] getScope() {
        return scope;
    }

    @Override
    public String getSpeRing() {
        return speRing;
    }

    @Override
    public String getRefreshTokenAge() {
        return refreshTokenAge;
    }

    @Override
    public String getFamilyId() {
        return familyId;
    }

    @Override
    public boolean isServicedFromCache() {
        return isServicedFromCache;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }
}
