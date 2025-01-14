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

package com.microsoft.identity.common.java.exception;

import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.java.commands.parameters.BrokerInteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.BrokerSilentTokenCommandParameters;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.Nullable;

public class IntuneAppProtectionPolicyRequiredException extends ServiceException {

    private static final String TAG = IntuneAppProtectionPolicyRequiredException.class.getSimpleName();

    // This is needed for backward compatibility with older versions of MSAL (pre 3.0.0)
    // When MSAL converts the result bundle it looks for this value to know about exception type
    // We moved the exception class to a new package with refactoring work,
    // but need to keep this value to older package name to avoid breaking older MSAL clients.
    public static final String sName = "com.microsoft.identity.common.exception.IntuneAppProtectionPolicyRequiredException";

    private static final long serialVersionUID = -620109887467926354L;

    private String mAccountUpn;
    private String mAccountUserId;
    private String mTenantId;
    private String mAuthorityUrl;


    public IntuneAppProtectionPolicyRequiredException(final String errorCode,
                                                      final String errorMessage) {
        super(errorCode, errorMessage, null);
    }

    public IntuneAppProtectionPolicyRequiredException(final String errorCode,
                                                      final String errorMessage,
                                                      final Throwable throwable) {
        super(errorCode, errorMessage, throwable);
    }

    public IntuneAppProtectionPolicyRequiredException(final String errorCode,
                                                      final String errorMessage,
                                                      final BrokerInteractiveTokenCommandParameters originalParameters) {
        super(errorCode, errorMessage, null);

        final String upn = (originalParameters.getBrokerAccount() != null) ?
                originalParameters.getBrokerAccount().getUsername() :
                originalParameters.getLoginHint();

        String uId = originalParameters.getLocalAccountId();

        if (StringUtil.isNullOrEmpty(uId)) {
            Logger.info(TAG, "Local account id is empty, attempting get user id from home account id");
            uId = getUIdFromHomeAccountId(
                    originalParameters.getHomeAccountId()
            );
        }

        final Authority authority = originalParameters.getAuthority();
        setAuthorityUrl(authority.getAuthorityURL().toString());

        final String homeAccountId = originalParameters.getHomeAccountId();
        String tenantId = null;

        if (homeAccountId != null) {
            tenantId = StringUtil.getTenantInfo(homeAccountId).getValue();
        }

        if (StringUtil.isNullOrEmpty(tenantId) && authority instanceof AzureActiveDirectoryAuthority) {
            tenantId = ((AzureActiveDirectoryAuthority) authority).mAudience.getTenantId();

        }

        if (StringUtil.isNullOrEmpty(uId)) {
            Logger.verbose(TAG, "IntuneAppProtectionPolicyException property user id was null or empty.");
        }
        if (StringUtil.isNullOrEmpty(upn)) {
            Logger.verbose(TAG, "IntuneAppProtectionPolicyException property upn was null or empty.");
        }
        if (StringUtil.isNullOrEmpty(tenantId)) {
            Logger.verbose(TAG, "IntuneAppProtectionPolicyException property tenant id was null or empty.");
        }

        Logger.verbose(TAG, "Setting IntuneAppProtectionPolicyException properties");
        Logger.verbosePII(TAG, String.format("Setting IntuneAppProtectionPolicyException properties.  AccountId: %s, UPN: %s, TenantId: %s", uId, upn, tenantId));
        setAccountUserId(uId);
        setAccountUpn(upn);
        setTenantId(tenantId);


    }

    public IntuneAppProtectionPolicyRequiredException(final String errorCode,
                                                      final String errorMessage,
                                                      final BrokerSilentTokenCommandParameters originalParameters) {
        super(errorCode, errorMessage, null);

        final String upn = (originalParameters.getBrokerAccount() != null) ?
                originalParameters.getBrokerAccount().getUsername() :
                originalParameters.getLoginHint();

        setAccountUpn(upn);

        String uId = originalParameters.getLocalAccountId();

        if (StringUtil.isNullOrEmpty(uId)) {
            Logger.info(TAG, "Local account id is empty, attempting get user id from home account id");
            uId = getUIdFromHomeAccountId(
                    originalParameters.getHomeAccountId()
            );
        }

        setAccountUserId(uId);

        final Authority authority = originalParameters.getAuthority();
        setAuthorityUrl(authority.getAuthorityURL().toString());

        final String homeAccountId = originalParameters.getHomeAccountId();
        String tenantId = null;

        if (homeAccountId != null) {
            tenantId = StringUtil.getTenantInfo(homeAccountId).getValue();
        }

        if (StringUtil.isNullOrEmpty(tenantId) && authority instanceof AzureActiveDirectoryAuthority) {
            tenantId = ((AzureActiveDirectoryAuthority) authority).mAudience.getTenantId();

        }
        setTenantId(tenantId);
    }

    /**
     * Helper method to get uid from home account id.
     * V2 home account format : <uid>.<utid>
     * V1 : it's stored as <uid>
     *
     * @param homeAccountId
     * @return valid uid or null if it's not in either of the format.
     */
    @Nullable
    private String getUIdFromHomeAccountId(@Nullable String homeAccountId) {

        final String methodName = ":getUIdFromHomeAccountId";
        final String DELIMITER_TENANTED_USER_ID = ".";
        final int EXPECTED_ARGS_LEN = 2;
        final int INDEX_USER_ID = 0;

        if (!StringUtil.isNullOrEmpty(homeAccountId)) {
            final String[] homeAccountIdSplit = homeAccountId.split(
                    Pattern.quote(DELIMITER_TENANTED_USER_ID)
            );

            if (homeAccountIdSplit.length == EXPECTED_ARGS_LEN) {
                Logger.info(TAG + methodName,
                        "Home account id is tenanted, returning uid "
                );
                return homeAccountIdSplit[INDEX_USER_ID];
            } else if (homeAccountIdSplit.length == 1) {
                Logger.info(TAG + methodName,
                        "Home account id not tenanted, it's the uid added by v1 broker "
                );
                return homeAccountIdSplit[INDEX_USER_ID];
            }
        }

        Logger.warn(TAG + methodName,
                "Home Account id doesn't have uid or tenant id information, returning null "
        );

        return null;
    }

    public String getAccountUpn() {
        return mAccountUpn;
    }

    public void setAccountUpn(String accountUpn) {
        mAccountUpn = accountUpn;
    }

    public String getAccountUserId() {
        return mAccountUserId;
    }

    public void setAccountUserId(String accountUserId) {
        mAccountUserId = accountUserId;
    }

    public String getTenantId() {
        return mTenantId;
    }

    public void setTenantId(String tenantId) {
        mTenantId = tenantId;
    }

    public String getAuthorityUrl() {
        return mAuthorityUrl;
    }

    public void setAuthorityUrl(String authorityUrl) {
        mAuthorityUrl = authorityUrl;
    }

    @Override
    public String getExceptionName() {
        return sName;
    }

    @Override
    public boolean isCacheable() {
        return false;
    }
}
