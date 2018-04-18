package com.microsoft.identity.common.internal.cache;

import android.support.annotation.NonNull;

import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.RefreshToken;
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

    // TODO move me!
    private static final String AUTHORITY_TYPE = "MSSTS";
    private static final String BEARER = "Bearer";

    @Override
    public Account createAccount(
            final OAuth2Strategy strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        final MicrosoftIdToken msIdToken = new MicrosoftIdToken(response.getIdToken());
        final Map<String, String> tokenClaims = msIdToken.getTokenClaims();
        final MicrosoftStsAuthorizationRequest msRequest = asMicrosoftStsAuthorizationRequest(request);
        final MicrosoftStsTokenResponse msTokenResponse = asMicrosoftStsTokenResponse(response);
        final ClientInfo clientInfo = new ClientInfo(msTokenResponse.getClientInfo());

        final Account account = new Account();
        account.setUniqueUserId(formatUniqueId(clientInfo));
        account.setEnvironment(msRequest.getAuthority().toString()); // host of authority with optional port
        account.setRealm(getRealm(strategy, response)); //tid
        account.setAuthorityAccountId(tokenClaims.get(OJBECT_ID)); // oid claim from id token
        account.setUsername(tokenClaims.get(PREFERRED_USERNAME));
        account.setAuthorityType(AUTHORITY_TYPE);
        account.setFirstName(tokenClaims.get(GIVEN_NAME));
        account.setLastName(tokenClaims.get(FAMILY_NAME));

        return account;
    }

    @NonNull
    private static MicrosoftStsTokenResponse asMicrosoftStsTokenResponse(final TokenResponse response) {
        MicrosoftStsTokenResponse msTokenResponse;

        if (response instanceof MicrosoftStsTokenResponse) {
            msTokenResponse = (MicrosoftStsTokenResponse) response;
        } else {
            throw new IllegalArgumentException("Invalid TokenResponse type.");
        }

        return msTokenResponse;
    }

    @NonNull
    private static MicrosoftStsAuthorizationRequest asMicrosoftStsAuthorizationRequest(final AuthorizationRequest request) {
        MicrosoftStsAuthorizationRequest msRequest;

        if (request instanceof MicrosoftStsAuthorizationRequest) {
            msRequest = (MicrosoftStsAuthorizationRequest) request;
        } else {
            throw new IllegalArgumentException("Invalid AuthorizationRequest type.");
        }

        return msRequest;
    }

    @NonNull
    private static MicrosoftStsOAuth2Strategy asMicrosoftStsOAuth2Strategy(final OAuth2Strategy strategy) {
        MicrosoftStsOAuth2Strategy msStrategy;

        if (strategy instanceof MicrosoftStsOAuth2Strategy) {
            msStrategy = (MicrosoftStsOAuth2Strategy) strategy;
        } else {
            throw new IllegalArgumentException("Invalid strategy type.");
        }

        return msStrategy;
    }

    @Override
    public AccessToken createAccessToken(
            final OAuth2Strategy strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
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

        return accessToken;
    }

    private String getExtendedExpiresOn(final OAuth2Strategy strategy, final TokenResponse response) {
        // TODO It doesn't look like the v2 endpoint supports extended_expires_on claims
        // Is this true?
        return null;
    }

    private String getAuthority(final AuthorizationRequest request) {
        final MicrosoftStsAuthorizationRequest msRequest = asMicrosoftStsAuthorizationRequest(request);
        final String authorityUrl = msRequest.getAuthority().toString();
        return authorityUrl;
    }

    private String getRealm(final OAuth2Strategy strategy, final TokenResponse response) {
        final MicrosoftStsOAuth2Strategy msStrategy = asMicrosoftStsOAuth2Strategy(strategy);
        final MicrosoftStsTokenResponse msTokenResponse = asMicrosoftStsTokenResponse(response);
        final MicrosoftStsAccount msAccount = (MicrosoftStsAccount) msStrategy.createAccount(msTokenResponse);
        return msAccount.getTenantId();
    }

    @Override
    public RefreshToken createRefreshToken(
            final OAuth2Strategy strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        final long cachedAt = getCachedAt();
        final long expiresOn = getExpiresOn(cachedAt, response);

        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTarget(getTarget(request));
        refreshToken.setCachedAt(String.valueOf(cachedAt)); // generated @ client side
        refreshToken.setExpiresOn(String.valueOf(expiresOn)); // derived from expires_in
        refreshToken.setClientInfo(getClientInfo(response));
        refreshToken.setFamilyId(getFamilyId(response));
        refreshToken.setUsername(getUsername(response));

        return refreshToken;
    }

    private String getUsername(final TokenResponse response) {
        final MicrosoftIdToken msIdToken = new MicrosoftIdToken(response.getIdToken());
        final Map<String, String> tokenClaims = msIdToken.getTokenClaims();
        return tokenClaims.get(PREFERRED_USERNAME);
    }

    private String getTarget(final AuthorizationRequest request) {
        final MicrosoftStsAuthorizationRequest msRequest = asMicrosoftStsAuthorizationRequest(request);
        return msRequest.getScope();
    }

    private long getCachedAt() {
        final long currentTimeMillis = System.currentTimeMillis();
        return TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis);
    }

    private long getExpiresOn(final long cachedAt, final TokenResponse response) {
        final MicrosoftStsTokenResponse msTokenResponse = asMicrosoftStsTokenResponse(response);
        final long expiresInSeconds = msTokenResponse.getExpiresIn();
        return cachedAt + expiresInSeconds;
    }

    private String getClientInfo(final TokenResponse response) {
        final MicrosoftStsTokenResponse msTokenResponse = asMicrosoftStsTokenResponse(response);
        return msTokenResponse.getClientInfo();
    }

    private String getFamilyId(final TokenResponse response) {
        final MicrosoftStsTokenResponse msTokenResponse = asMicrosoftStsTokenResponse(response);
        return msTokenResponse.getFamilyId();
    }

    private String formatUniqueId(final ClientInfo clientInfo) {
        final String uid = clientInfo.getUid();
        final String utid = clientInfo.getUtid();
        return uid + "." + utid;
    }
}
