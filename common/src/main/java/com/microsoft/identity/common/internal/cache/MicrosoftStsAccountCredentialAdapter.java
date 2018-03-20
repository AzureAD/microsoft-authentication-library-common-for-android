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

import static com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdToken.OJBECT_ID;
import static com.microsoft.identity.common.internal.providers.oauth2.IDToken.FAMILY_NAME;
import static com.microsoft.identity.common.internal.providers.oauth2.IDToken.GIVEN_NAME;
import static com.microsoft.identity.common.internal.providers.oauth2.IDToken.PREFERRED_USERNAME;

public class MicrosoftStsAccountCredentialAdapter implements IAccountCredentialAdapter {

    // TODO move me!
    public static final String AUTHORITY_TYPE = "MSSTS";

    @Override
    public Account createAccount(
            final OAuth2Strategy strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        final MicrosoftIdToken msIdToken = new MicrosoftIdToken(response.getIdToken());
        final Map<String, String> tokenClaims = msIdToken.getTokenClaims();
        final MicrosoftStsOAuth2Strategy msStrategy = asMicrosoftStsOAuth2Strategy(strategy);
        final MicrosoftStsAuthorizationRequest msRequest = asMicrosoftStsAuthorizationRequest(request);
        final MicrosoftStsTokenResponse msTokenResponse = asMicrosoftStsTokenResponse(response);
        final MicrosoftStsAccount msAccount = (MicrosoftStsAccount) msStrategy.createAccount(msTokenResponse);
        final ClientInfo clientInfo = new ClientInfo(msTokenResponse.getClientInfo());

        final Account account = new Account();
        account.setUniqueId(formatUniqueId(clientInfo));
        account.setEnvironment(msRequest.getAuthority().toString()); // host of authority with optional port
        account.setRealm(msAccount.getTenantId()); //tid
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
    public AccessToken createAccessToken(OAuth2Strategy strategy, AuthorizationRequest request, TokenResponse response) {
        final AccessToken accessToken = new AccessToken();
        // TODO initialize
        return accessToken;
    }

    @Override
    public RefreshToken createRefreshToken(OAuth2Strategy strategy, AuthorizationRequest request, TokenResponse response) {
        final MicrosoftStsAuthorizationRequest msRequest = asMicrosoftStsAuthorizationRequest(request);
        final MicrosoftStsTokenResponse msTokenResponse = asMicrosoftStsTokenResponse(response);
        final long currentTimeMillis = System.currentTimeMillis();
        final String currentTimeMillisStr = String.valueOf(currentTimeMillis);
        final long expiresInSeconds = msTokenResponse.getExpiresIn();
        final long expiresInMillis = expiresInSeconds * 1000;
        // The cached value uses millis, the service return value users seconds
        final long expiresOnCacheValue = currentTimeMillis + expiresInMillis;
        final String expiresOnCacheValueStr = String.valueOf(expiresOnCacheValue);

        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTarget(msRequest.getScope());
        refreshToken.setCachedAt(currentTimeMillisStr); // generated @ client side
        /*
        Per this document: https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-protocols-oauth-code
        expires_on is expressed as SECONDS since Jan 1 1970 - our schema caches this value as millis derived from expires_in
         */
        refreshToken.setExpiresOn(expiresOnCacheValueStr); // derived from expires_in
        refreshToken.setClientInfo(msTokenResponse.getClientInfo());
        refreshToken.setFamilyId(msTokenResponse.getFamilyId());
        return refreshToken;
    }

    private String formatUniqueId(final ClientInfo clientInfo) {
        final String uid = clientInfo.getUid();
        final String utid = clientInfo.getUtid();
        return uid + "." + utid;
    }
}
