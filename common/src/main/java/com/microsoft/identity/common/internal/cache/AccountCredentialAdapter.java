package com.microsoft.identity.common.internal.cache;

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

public class AccountCredentialAdapter implements IAccountCredentialAdapter {

    // TODO move me!
    public static final String AUTHORITY_TYPE = "MSSTS";

    @Override
    public Account createAccount(
            final OAuth2Strategy strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        final MicrosoftIdToken msIdToken = new MicrosoftIdToken(response.getIdToken());
        final Map<String, String> tokenClaims = msIdToken.getTokenClaims();
        final MicrosoftStsOAuth2Strategy msStrategy;
        final MicrosoftStsAuthorizationRequest msRequest;
        final MicrosoftStsTokenResponse msTokenResponse;
        final MicrosoftStsAccount msAccount;
        final ClientInfo clientInfo;

        // Check types...
        if (strategy instanceof MicrosoftStsOAuth2Strategy) {
            msStrategy = (MicrosoftStsOAuth2Strategy) strategy;
        } else {
            throw new IllegalArgumentException("Invalid strategy type.");
        }

        if (request instanceof MicrosoftStsAuthorizationRequest) {
            msRequest = (MicrosoftStsAuthorizationRequest) request;
        } else {
            throw new IllegalArgumentException("Invalid AuthorizationRequest type.");
        }

        if (response instanceof MicrosoftStsTokenResponse) {
            msTokenResponse = (MicrosoftStsTokenResponse) response;
        } else {
            throw new IllegalArgumentException("Invalid TokenResponse type.");
        }

        msAccount = (MicrosoftStsAccount) msStrategy.createAccount(msTokenResponse);
        clientInfo = new ClientInfo(msTokenResponse.getClientInfo());

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

    @Override
    public AccessToken createAccessToken(OAuth2Strategy strategy, AuthorizationRequest request, TokenResponse response) {
        final AccessToken accessToken = new AccessToken();
        // TODO initialize
        return accessToken;
    }

    @Override
    public RefreshToken createRefreshToken(OAuth2Strategy strategy, AuthorizationRequest request, TokenResponse response) {
        final RefreshToken refreshToken = new RefreshToken();
        // TODO intialize
        return refreshToken;
    }

    private String formatUniqueId(final ClientInfo clientInfo) {
        final String uid = clientInfo.getUid();
        final String utid = clientInfo.getUtid();
        return uid + "." + utid;
    }
}
