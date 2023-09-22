package com.microsoft.identity.common.java.util;

import static com.microsoft.identity.common.java.authorities.Authority.B2C;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.authscheme.ITokenAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.foci.FociQueryUtilities;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.request.SdkType;

import java.io.IOException;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

public class CacheUtil {
    private static final String TAG = CacheUtil.class.getSimpleName();

    public static ICacheRecord finalizeCacheRecordForResult(@NonNull final ICacheRecord cacheRecord,
                                                     @NonNull final AbstractAuthenticationScheme scheme) throws ClientException {
        if (scheme instanceof ITokenAuthenticationSchemeInternal &&
                !StringUtil.isNullOrEmpty(cacheRecord.getAccessToken().getSecret())) {
            final ITokenAuthenticationSchemeInternal tokenAuthScheme = (ITokenAuthenticationSchemeInternal) scheme;
            cacheRecord
                    .getAccessToken()
                    .setSecret(
                            tokenAuthScheme
                                    .getAccessTokenForScheme(
                                            cacheRecord.getAccessToken().getSecret()
                                    )
                    );
        }

        return cacheRecord;
    }

    public static AccountRecord getCachedAccountRecord(
            @NonNull final SilentTokenCommandParameters parameters) throws ClientException {
        final String methodTag = TAG + ":getCachedAccountRecord";
        if (parameters.getAccount() == null) {
            throw new ClientException(
                    ErrorStrings.NO_ACCOUNT_FOUND,
                    "No cached accounts found for the supplied homeAccountId and clientId"
            );
        }

        final boolean isB2CAuthority = B2C.equalsIgnoreCase(
                parameters
                        .getAuthority()
                        .getAuthorityTypeString()
        );

        AccountRecord targetAccount = getCachedAccountRecordFromCallingAppCache(parameters);
        if (targetAccount != null) {
            return targetAccount;
        } else {
            Logger.info(methodTag, "Account not found in app cache..");
            targetAccount = getCachedAccountRecordFromAllCaches(parameters);
        }

        if (null == targetAccount) {
            final String clientId = parameters.getClientId();
            final String homeAccountId = parameters.getAccount().getHomeAccountId();
            if (Logger.isAllowPii()) {
                Logger.errorPII(
                        methodTag,
                        "No accounts found for clientId [" + clientId + "], homeAccountId [" + homeAccountId + "]",
                        null
                );
            } else {
                Logger.error(
                        methodTag,
                        "No accounts found for clientId [" + clientId + "]",
                        null
                );
            }

            throw new ClientException(
                    ErrorStrings.NO_ACCOUNT_FOUND,
                    "No cached accounts found for the supplied "
                            + (isB2CAuthority ? "homeAccountId" : "localAccountId")
            );
        }

        return targetAccount;
    }

    /**
     * Lookup in app-specific cache.
     */
    @Nullable
    private static AccountRecord getCachedAccountRecordFromCallingAppCache(
            @NonNull final SilentTokenCommandParameters parameters) {
        final boolean isB2CAuthority = B2C.equalsIgnoreCase(
                parameters
                        .getAuthority()
                        .getAuthorityTypeString()
        );

        final String clientId = parameters.getClientId();
        final String homeAccountId = parameters.getAccount().getHomeAccountId();
        final String localAccountId = parameters.getAccount().getLocalAccountId();
        final String environment = parameters.getAccount().getEnvironment();

        AccountRecord targetAccount;

        if (isB2CAuthority) {
            // Due to differences in the B2C service API relative to AAD, all IAccounts returned by
            // the B2C-STS have the same local_account_id irrespective of the policy used to load it.
            //
            // Because the home_account_id is unique to policy and there is no concept of
            // multi-realm accounts relative to B2C, we'll conditionally use the home_account_id
            // in these cases
            targetAccount = parameters
                    .getOAuth2TokenCache()
                    .getAccountByHomeAccountId(
                            null,
                            clientId,
                            homeAccountId
                    );
        } else {
            targetAccount = parameters
                    .getOAuth2TokenCache()
                    .getAccountByLocalAccountId(
                            environment,
                            clientId,
                            localAccountId
                    );
        }
        return targetAccount;
    }

    /**
     * Lookup in ALL the caches including the foci cache.
     */
    @Nullable
    protected static AccountRecord getCachedAccountRecordFromAllCaches(
            @NonNull final SilentTokenCommandParameters parameters) throws ClientException {
        // TO-DO https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1999531/
        if (parameters.getOAuth2TokenCache() instanceof MsalOAuth2TokenCache) {
            return getAccountWithFRTIfAvailable(
                    parameters,
                    (MsalOAuth2TokenCache) parameters.getOAuth2TokenCache()
            );
        }
        return null;
    }

    @Nullable
    private static AccountRecord getAccountWithFRTIfAvailable(@NonNull final SilentTokenCommandParameters parameters,
                                                       @SuppressWarnings(WarningType.rawtype_warning) @NonNull final MsalOAuth2TokenCache msalOAuth2TokenCache) {

        final String methodTag = TAG + ":getAccountWithFRTIfAvailable";
        final String homeAccountId = parameters.getAccount().getHomeAccountId();
        final String clientId = parameters.getClientId();

        // check for FOCI tokens for the homeAccountId
        final RefreshTokenRecord refreshTokenRecord = msalOAuth2TokenCache
                .getFamilyRefreshTokenForHomeAccountId(homeAccountId);

        if (refreshTokenRecord != null) {
            try {
                // foci token is available, make a request to service to see if the client id is FOCI and save the tokens
                FociQueryUtilities.tryFociTokenWithGivenClientId(
                        parameters.getOAuth2TokenCache(),
                        clientId,
                        parameters.getRedirectUri(),
                        refreshTokenRecord,
                        parameters.getAccount()
                );

                // Try to look for account again in the cache
                return parameters
                        .getOAuth2TokenCache()
                        .getAccountByLocalAccountId(
                                null,
                                clientId,
                                parameters.getAccount().getLocalAccountId()
                        );
            } catch (IOException | ClientException e) {
                Logger.warn(methodTag,
                        "Error while attempting to validate client: "
                                + clientId + " is part of family " + e.getMessage()
                );
            }
        } else {
            Logger.info(methodTag, "No Foci tokens found for homeAccountId " + homeAccountId);
        }
        return null;
    }

    public static boolean refreshTokenIsNull(@NonNull final ICacheRecord cacheRecord) {
        return null == cacheRecord.getRefreshToken();
    }

    public static boolean accessTokenIsNull(@NonNull final ICacheRecord cacheRecord) {
        return null == cacheRecord.getAccessToken();
    }

    public static boolean idTokenIsNull(@NonNull final ICacheRecord cacheRecord,
                                        @NonNull final SdkType sdkType) {
        final IdTokenRecord idTokenRecord = (sdkType == SdkType.ADAL) ?
                cacheRecord.getV1IdToken() : cacheRecord.getIdToken();

        return null == idTokenRecord;
    }

    /**
     * Helper method which returns false if the tenant id of the authority
     * doesn't match with the tenant of the Access token for AADAuthority.
     * <p>
     * Returns true otherwise.
     */
    public static boolean isRequestAuthorityRealmSameAsATRealm(@NonNull final Authority requestAuthority,
                                                        @NonNull final AccessTokenRecord accessTokenRecord)
            throws ServiceException, ClientException {
        if (requestAuthority instanceof AzureActiveDirectoryAuthority) {

            String tenantId = ((AzureActiveDirectoryAuthority) requestAuthority).getAudience().getTenantId();

            if (AzureActiveDirectoryAudience.isHomeTenantAlias(tenantId)) {
                // if realm on AT and home account's tenant id do not match, we have a token for guest and
                // requested authority here is for home, so return false we need to refresh the token
                final String utidFromHomeAccountId = accessTokenRecord
                        .getHomeAccountId()
                        .split(Pattern.quote("."))[1];

                return utidFromHomeAccountId.equalsIgnoreCase(accessTokenRecord.getRealm());

            } else {
                tenantId = ((AzureActiveDirectoryAuthority) requestAuthority)
                        .getAudience()
                        .getTenantUuidForAlias(requestAuthority.getAuthorityURL().toString());
                return tenantId.equalsIgnoreCase(accessTokenRecord.getRealm());
            }
        }
        return true;
    }

}
