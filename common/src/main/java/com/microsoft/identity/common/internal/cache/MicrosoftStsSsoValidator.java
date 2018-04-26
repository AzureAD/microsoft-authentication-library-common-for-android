package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;

class MicrosoftStsSsoValidator implements ISsoValidator {

    private static final String TAG = MicrosoftStsSsoValidator.class.getSimpleName();
    private volatile boolean mAccountIsValid;
    private volatile boolean mRefreshTokenIsValid;

    @Override
    public synchronized boolean isAccountValid(final Account account) {
        final String methodName = "isAccountValid";
        Logger.entering(TAG, methodName, account);

        if (null == account) {
            mAccountIsValid = false;
            Logger.warn(TAG + ":" + methodName, "Account is null.");
        } else {
            // Validate required fields
            final String uniqueUserId = account.getUniqueUserId();
            if (StringExtensions.isNullOrBlank(uniqueUserId)
                    || uniqueUserId.equalsIgnoreCase(".")) {
                mAccountIsValid = false;
                Logger.warn(TAG + ":" + methodName, "UniqueUserId was null or invalid");
            }

            validateAccountData("environment", account.getEnvironment(), true);
            validateAccountData("realm", account.getRealm(), true);
            validateAccountData("authorityAccountId", account.getAuthorityAccountId(), true);
            validateAccountData("username", account.getUsername(), true);
            validateAccountData("authorityType", account.getAuthorityType(), true);

            // Inspect optional fields, warn if missing but don't invalidate
            validateAccountData("guestId", account.getGuestId(), false);
            validateAccountData("firstName", account.getFirstName(), false);
            validateAccountData("lastName", account.getLastName(), false);
            validateAccountData("avatarUrl", account.getAvatarUrl(), false);
        }

        Logger.exiting(TAG, methodName, mAccountIsValid);

        return mAccountIsValid;
    }

    @Override
    public synchronized boolean isRefreshTokenValid(final RefreshToken refreshToken) {
        final String methodName = "isRefreshTokenValid";
        Logger.entering(TAG, methodName, refreshToken);

        // Validate required fields
        validateRefreshTokenData("uniqueUserId", refreshToken.getUniqueUserId(), true);
        validateRefreshTokenData("environment", refreshToken.getEnvironment(), true);
        validateRefreshTokenData("clientId", refreshToken.getClientId(), true);
        validateRefreshTokenData("secret", refreshToken.getSecret(), true);

        // Inspect optional fields, warn if missing but don't invalidate
        validateRefreshTokenData("target", refreshToken.getTarget(), false);
        validateRefreshTokenData("expiresOn", refreshToken.getExpiresOn(), false);
        validateRefreshTokenData("familyId", refreshToken.getFamilyId(), false);

        Logger.exiting(TAG, methodName, mRefreshTokenIsValid);

        return mRefreshTokenIsValid;
    }

    private void validateAccountData(final String key, final String value, boolean requiredField) {
        final String methodName = "validateAccountData";
        Logger.entering(TAG, methodName, key, value);
        validateFieldInternal(key, value, requiredField, true);
        Logger.exiting(TAG, methodName);
    }

    private void validateRefreshTokenData(final String key, final String value, boolean requiredField) {
        final String methodName = "validateRefreshTokenData";
        Logger.entering(TAG, methodName, key, value);
        validateFieldInternal(key, value, requiredField, false);
        Logger.exiting(TAG, methodName);
    }

    private void validateFieldInternal(final String key, final String value, boolean requiredField, boolean isAccount) {
        final String methodName = "validateFieldInternal";
        Logger.entering(TAG, methodName, key, value);

        if (StringExtensions.isNullOrBlank(value)) {
            if (requiredField && isAccount) {
                mAccountIsValid = false;
            } else if (requiredField) { // is implicitly evaluating an RT
                mRefreshTokenIsValid = false;
            }
            Logger.warn(TAG + ":" + methodName, key + " was null or blank");
        }

        Logger.exiting(TAG, methodName);
    }
}
