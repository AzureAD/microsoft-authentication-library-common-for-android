package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;

/**
 * Default implementation to validate {@link Account} and {@link RefreshToken} token
 * instances prior to cache-writes. This class is thread-safe.
 */
public class DefaultSsoValidator implements ISsoValidator {

    /**
     * TAG used for logging.
     */
    private static final String TAG = DefaultSsoValidator.class.getSimpleName();

    /**
     * Valid-state flag for verifying {@link Account} instances.
     */
    private volatile boolean mAccountIsValid = true;

    /**
     * Valid-state flag for verifying {@link RefreshToken} instances.
     */
    private volatile boolean mRefreshTokenIsValid = true;

    @Override
    public synchronized boolean isAccountValid(final Account account) {
        final String methodName = "isAccountValid";
        Logger.entering(TAG, methodName, account);

        // Reset the validation flag state.
        mAccountIsValid = true;

        if (null == account) {
            mAccountIsValid = false;
            Logger.warn(TAG + ":" + methodName, "Account is null.");
        } else {
            // Validate required & optional fields
            validateAccountRequiredFields(account);
            validateAccountOptionalFields(account);
        }

        Logger.exiting(TAG, methodName, mAccountIsValid);

        return mAccountIsValid;
    }

    @Override
    public synchronized boolean isRefreshTokenValid(final RefreshToken refreshToken) {
        final String methodName = "isRefreshTokenValid";
        Logger.entering(TAG, methodName, refreshToken);

        // Reset the validation flag state.
        mRefreshTokenIsValid = true;

        if (null == refreshToken) {
            mRefreshTokenIsValid = false;
            Logger.warn(TAG + ":" + methodName, "RefreshToken is null.");
        } else {
            // Validate required & optional fields
            validateRefreshTokenRequiredFields(refreshToken);
            validateRefreshTokenOptionalFields(refreshToken);
        }

        Logger.exiting(TAG, methodName, mRefreshTokenIsValid);

        return mRefreshTokenIsValid;
    }

    /**
     * Any required fields you would like to validate on the {@link Account} object should be
     * checked here.
     *
     * @param account The Account to validate.
     */
    private void validateAccountRequiredFields(Account account) {
        final String methodName = "validateAccountRequiredFields";
        Logger.entering(TAG, methodName, account);

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

        Logger.exiting(TAG, methodName);
    }

    /**
     * Any optional fields you would like to validate on the {@link Account} object should be
     * checked here.
     *
     * @param account The Account to validate.
     */
    private void validateAccountOptionalFields(Account account) {
        final String methodName = "validateAccountOptionalFields";
        Logger.entering(TAG, methodName, account);

        validateAccountData("guestId", account.getGuestId(), false);
        validateAccountData("firstName", account.getFirstName(), false);
        validateAccountData("lastName", account.getLastName(), false);
        validateAccountData("avatarUrl", account.getAvatarUrl(), false);

        Logger.exiting(TAG, methodName);
    }

    /**
     * Any required fields you would like to validate on the {@link RefreshToken} object should be
     * checked here.
     *
     * @param refreshToken The RefreshToken to validate.
     */
    private void validateRefreshTokenRequiredFields(RefreshToken refreshToken) {
        final String methodName = "validateRefreshTokenRequiredFields";
        Logger.entering(TAG, methodName, refreshToken);

        validateRefreshTokenData("uniqueUserId", refreshToken.getUniqueUserId(), true);
        validateRefreshTokenData("environment", refreshToken.getEnvironment(), true);
        validateRefreshTokenData("clientId", refreshToken.getClientId(), true);
        validateRefreshTokenData("secret", refreshToken.getSecret(), true);

        Logger.exiting(TAG, methodName);
    }

    /**
     * Any optional fields you would like to validate on the {@link RefreshToken} object should be
     * checked here.
     *
     * @param refreshToken The RefreshToken to validate.
     */
    private void validateRefreshTokenOptionalFields(RefreshToken refreshToken) {
        final String methodName = "validateRefreshTokenOptionalFields";
        Logger.entering(TAG, methodName, refreshToken);

        validateRefreshTokenData("target", refreshToken.getTarget(), false);
        validateRefreshTokenData("expiresOn", refreshToken.getExpiresOn(), false);
        validateRefreshTokenData("familyId", refreshToken.getFamilyId(), false);

        Logger.exiting(TAG, methodName);
    }

    /**
     * Checks that supplied value is not null or blank. If supplied value is flagged 'required',
     * then the valid status of the current Account will be set to false.
     *
     * @param key           The name of the value being evaluated (used for logging).
     * @param value         The value to inspect.
     * @param requiredField True, if the supplied value must be populated. False if the supplied
     *                      value is optional.
     */
    private void validateAccountData(final String key, final String value, boolean requiredField) {
        final String methodName = "validateAccountData";
        Logger.entering(TAG, methodName, key, value);
        validateFieldInternal(key, value, requiredField, true);
        Logger.exiting(TAG, methodName);
    }

    /**
     * Checks that supplied value is not null or blank. If supplied value is flagged 'required',
     * then the valid status of the current RefreshToken will be set to false.
     *
     * @param key           The name of the value being evaluated (used for logging).
     * @param value         The value to inspect.
     * @param requiredField True, if the supplied value must be populated. False if the supplied
     *                      value is optional.
     */
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
