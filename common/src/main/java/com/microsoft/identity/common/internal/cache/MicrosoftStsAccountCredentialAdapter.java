package com.microsoft.identity.common.internal.cache;

import android.support.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.RefreshToken;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdToken;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAccount;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdToken.OJBECT_ID;
import static com.microsoft.identity.common.internal.providers.oauth2.IDToken.FAMILY_NAME;
import static com.microsoft.identity.common.internal.providers.oauth2.IDToken.GIVEN_NAME;
import static com.microsoft.identity.common.internal.providers.oauth2.IDToken.PREFERRED_USERNAME;

public class MicrosoftStsAccountCredentialAdapter implements IAccountCredentialAdapter {

    private static final String TAG = MicrosoftStsAccountCredentialAdapter.class.getSimpleName();

    // TODO move me!
    private static final String AUTHORITY_TYPE = "MSSTS";
    private static final String BEARER = "Bearer";
    private static final String FOCI_PREFIX = "foci-";

    @Override
    public Account createAccount(
            final OAuth2Strategy strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        final String methodName = "createAccount";
        Logger.entering(TAG, methodName, strategy, request, response);

        final MicrosoftIdToken msIdToken;
        try {
            msIdToken = new MicrosoftIdToken(response.getIdToken());
            final Map<String, String> tokenClaims = msIdToken.getTokenClaims();
            final MicrosoftStsAuthorizationRequest msRequest = asMicrosoftStsAuthorizationRequest(request);
            final MicrosoftStsTokenResponse msTokenResponse = asMicrosoftStsTokenResponse(response);
            final ClientInfo clientInfo = new ClientInfo(msTokenResponse.getClientInfo());

            final Account account = new Account();
            account.setUniqueUserId(SchemaUtil.getUniqueId(clientInfo));
            account.setEnvironment(msRequest.getAuthority().toString()); // host of authority with optional port
            account.setRealm(getRealm(strategy, response)); //tid
            account.setAuthorityAccountId(tokenClaims.get(OJBECT_ID)); // oid claim from id token
            account.setUsername(tokenClaims.get(PREFERRED_USERNAME));
            account.setAuthorityType(AUTHORITY_TYPE);
            account.setFirstName(tokenClaims.get(GIVEN_NAME));
            account.setLastName(tokenClaims.get(FAMILY_NAME));

            Logger.exiting(TAG, methodName, account);

            return account;
        } catch (ServiceException e) {
            // TODO handle this properly
            throw new RuntimeException(e);
        }
    }

    @NonNull
    private static MicrosoftStsTokenResponse asMicrosoftStsTokenResponse(final TokenResponse response) {
        final String methodName = "asMicrosoftStsTokenResponse";
        Logger.entering(TAG, methodName, response);

        MicrosoftStsTokenResponse msTokenResponse;

        if (response instanceof MicrosoftStsTokenResponse) {
            msTokenResponse = (MicrosoftStsTokenResponse) response;
        } else {
            throw new IllegalArgumentException("Invalid TokenResponse type.");
        }

        Logger.exiting(TAG, methodName, msTokenResponse);

        return msTokenResponse;
    }

    @NonNull
    private static MicrosoftStsAuthorizationRequest asMicrosoftStsAuthorizationRequest(final AuthorizationRequest request) {
        final String methodName = "asMicrosoftStsAuthorizationRequest";
        Logger.entering(TAG, methodName, request);

        MicrosoftStsAuthorizationRequest msRequest;

        if (request instanceof MicrosoftStsAuthorizationRequest) {
            msRequest = (MicrosoftStsAuthorizationRequest) request;
        } else {
            throw new IllegalArgumentException("Invalid AuthorizationRequest type.");
        }

        Logger.exiting(TAG, methodName, msRequest);

        return msRequest;
    }

    @NonNull
    private static MicrosoftStsOAuth2Strategy asMicrosoftStsOAuth2Strategy(final OAuth2Strategy strategy) {
        final String methodName = "asMicrosoftStsOAuth2Strategy";
        Logger.entering(TAG, methodName, strategy);

        MicrosoftStsOAuth2Strategy msStrategy;

        if (strategy instanceof MicrosoftStsOAuth2Strategy) {
            msStrategy = (MicrosoftStsOAuth2Strategy) strategy;
        } else {
            throw new IllegalArgumentException("Invalid strategy type.");
        }

        Logger.exiting(TAG, methodName, msStrategy);

        return msStrategy;
    }

    @Override
    public AccessToken createAccessToken(
            final OAuth2Strategy strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        final String methodName = "createAccessToken";
        Logger.entering(TAG, methodName, strategy, request, response);

        final long cachedAt = getCachedAt();
        final long expiresOn = getExpiresOn(cachedAt, response);

        final AccessToken accessToken = new AccessToken();
        accessToken.setTarget(getTarget(request));
        accessToken.setCachedAt(String.valueOf(cachedAt)); // generated @ client side
        accessToken.setExpiresOn(String.valueOf(expiresOn)); // derived from expires_in
        accessToken.setClientInfo(getClientInfo(response));
        // TODO Do AccessTokens track a family id?
        //accessToken.setFamilyId(msTokenResponse.getFamilyId());
        accessToken.setAccessTokenType(BEARER); // TODO does this value come from somewhere in the auth response?
        accessToken.setExtendedExpiresOn(getExtendedExpiresOn(strategy, response));
        accessToken.setAuthority(getAuthority(request));
        accessToken.setRealm(getRealm(strategy, response));

        Logger.exiting(TAG, methodName, accessToken);

        return accessToken;
    }

    private String getExtendedExpiresOn(final OAuth2Strategy strategy, final TokenResponse response) {
        final String methodName = "getExtendedExpiresOn";
        Logger.entering(TAG, methodName, strategy, response);

        // TODO It doesn't look like the v2 endpoint supports extended_expires_on claims
        // Is this true?
        String result = null;

        Logger.exiting(TAG, methodName, result);

        return result;
    }

    private String getAuthority(final AuthorizationRequest request) {
        final String methodName = "getAuthority";
        Logger.entering(TAG, methodName, request);

        final MicrosoftStsAuthorizationRequest msRequest = asMicrosoftStsAuthorizationRequest(request);
        final String authorityUrl = msRequest.getAuthority().toString();

        Logger.exiting(TAG, methodName, authorityUrl);

        return authorityUrl;
    }

    private String getRealm(final OAuth2Strategy strategy, final TokenResponse response) {
        final String methodName = "getRealm";
        Logger.entering(TAG, methodName, strategy, response);

        final MicrosoftStsOAuth2Strategy msStrategy = asMicrosoftStsOAuth2Strategy(strategy);
        final MicrosoftStsTokenResponse msTokenResponse = asMicrosoftStsTokenResponse(response);
        final MicrosoftStsAccount msAccount = (MicrosoftStsAccount) msStrategy.createAccount(msTokenResponse);

        Logger.exiting(TAG, methodName, msAccount.getRealm());

        return msAccount.getRealm();
    }

    @Override
    public RefreshToken createRefreshToken(
            final OAuth2Strategy strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        final String methodName = "createRefreshToken";
        Logger.entering(TAG, methodName, strategy, request, response);

        final long cachedAt = getCachedAt();
        final long expiresOn = getExpiresOn(cachedAt, response);

        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTarget(getTarget(request));
        refreshToken.setCachedAt(String.valueOf(cachedAt)); // generated @ client side
        refreshToken.setExpiresOn(String.valueOf(expiresOn)); // derived from expires_in
        refreshToken.setClientInfo(getClientInfo(response));
        refreshToken.setFamilyId(getFamilyId(response));
        refreshToken.setUsername(getUsername(response));

        Logger.exiting(TAG, methodName, refreshToken);

        return refreshToken;
    }

    @Override
    public RefreshToken asRefreshToken(final com.microsoft.identity.common.internal.providers.oauth2.RefreshToken refreshTokenIn) {
        final String methodName = "asRefreshToken";
        Logger.entering(TAG, methodName, refreshTokenIn);

        final RefreshToken refreshTokenOut = new RefreshToken();

        // Required fields
        refreshTokenOut.setUniqueUserId(refreshTokenIn.getUniqueUserId());
        refreshTokenOut.setEnvironment(refreshTokenIn.getEnvironment());
        refreshTokenOut.setCredentialType(CredentialType.RefreshToken.name());
        refreshTokenOut.setClientId(refreshTokenIn.getClientId());
        refreshTokenOut.setSecret(refreshTokenIn.getSecret());

        // Optional fields
        refreshTokenOut.setTarget(refreshTokenIn.getTarget());
        refreshTokenOut.setCachedAt(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
        refreshTokenOut.setExpiresOn(refreshTokenIn.getExpiresOn());
        //refreshTokenOut.setClientInfo(""); TODO OK to drop?
        refreshTokenOut.setFamilyId(refreshTokenIn.getFamilyId());
        //refreshTokenOut.setUsername(""); TODO OK to drop?

        if (!StringExtensions.isNullOrBlank(refreshTokenIn.getFamilyId())) {
            String familyId = refreshTokenIn.getFamilyId();
            // It is a foci token, replace the client and [possibly] prepend "foci-"
            if (!familyId.startsWith(FOCI_PREFIX)) {
                familyId = FOCI_PREFIX + familyId;
            }

            refreshTokenOut.setClientId(familyId);
        }

        Logger.exiting(TAG, methodName, refreshTokenOut);

        return refreshTokenOut;
    }

    @Override
    public Account asAccount(com.microsoft.identity.common.Account account) {
        final String methodName = "asAccount";
        Logger.entering(TAG, methodName, account);

        Account acct = new Account(account);

        Logger.exiting(TAG, methodName, acct);

        return acct;
    }

    private String getUsername(final TokenResponse response) {
        final String methodName = "getUsername";
        Logger.entering(TAG, methodName, response);

        try {
            final MicrosoftIdToken msIdToken = new MicrosoftIdToken(response.getIdToken());
            final Map<String, String> tokenClaims = msIdToken.getTokenClaims();
            final String username = tokenClaims.get(PREFERRED_USERNAME);

            Logger.exiting(TAG, methodName, username);

            return username;
        } catch (ServiceException e) {
            // TODO handle this properly
            throw new RuntimeException(e);
        }
    }

    private String getTarget(final AuthorizationRequest request) {
        final String methodName = "getTarget";
        Logger.entering(TAG, methodName, request);

        final MicrosoftStsAuthorizationRequest msRequest = asMicrosoftStsAuthorizationRequest(request);
        final String target = msRequest.getScope();

        Logger.exiting(TAG, methodName, target);

        return target;
    }

    private long getCachedAt() {
        final String methodName = "getCachedAt";
        Logger.entering(TAG, methodName);

        final long currentTimeMillis = System.currentTimeMillis();
        final long cachedAt = TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis);

        Logger.exiting(TAG, methodName, cachedAt);

        return cachedAt;
    }

    private long getExpiresOn(final long cachedAt, final TokenResponse response) {
        final String methodName = "";
        Logger.entering(TAG, methodName, cachedAt, response);

        final MicrosoftStsTokenResponse msTokenResponse = asMicrosoftStsTokenResponse(response);
        final long expiresInSeconds = msTokenResponse.getExpiresIn();
        final long expiresOn = cachedAt + expiresInSeconds;

        Logger.exiting(TAG, methodName, expiresOn);

        return expiresOn;
    }

    private String getClientInfo(final TokenResponse response) {
        final String methodName = "getClientInfo";
        Logger.entering(TAG, methodName, response);

        final MicrosoftStsTokenResponse msTokenResponse = asMicrosoftStsTokenResponse(response);
        final String clientInfo = msTokenResponse.getClientInfo();

        Logger.exiting(TAG, methodName, clientInfo);

        return clientInfo;
    }

    private String getFamilyId(final TokenResponse response) {
        final String methodName = "getFamilyId";
        Logger.entering(TAG, methodName, response);

        final MicrosoftStsTokenResponse msTokenResponse = asMicrosoftStsTokenResponse(response);
        final String familyId = msTokenResponse.getFamilyId();

        Logger.exiting(TAG, methodName, familyId);

        return familyId;
    }
}
