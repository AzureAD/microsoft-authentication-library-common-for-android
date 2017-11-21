package com.microsoft.identity.common.internal.providers.azureactivedirectory;

import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaSessionCompat;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

import java.net.URL;
import java.util.UUID;


/**
 * The Azure Active Directory oAuth2 Strategy
 */
public class AzureActiveDirectoryOAuth2Strategy extends OAuth2Strategy {

    protected void validateAuthoriztionRequest(AuthorizationRequest request){

    }

    /**
     * validate the contents of the token request... all the base class is currently abstract
     * some of the validation for requried parameters for the protocol could be there...
     *
     * @param request
     */
    protected void validateTokenRequest(TokenRequest request){

    }

    /**
     * Stubbed out for now, but should create a new AzureActiveDirectory account
     * Should accept a parameter (TokenResponse) for producing that user
     * @return
     */
    protected Account createAccount(TokenResponse response){
        IDToken idToken = new IDToken(response.getIdToken());
        return AzureActiveDirectoryAccount.create(idToken);
    }

    /*
    private void validateAuthority(final URL authorityUrl,
                                   @Nullable final String domain,
                                   boolean isSilent,
                                   final UUID correlationId) throws AuthenticationException {
        boolean isAdfsAuthority = UrlExtensions.isADFSAuthority(authorityUrl);
        final boolean isAuthorityValidated = AuthorityValidationMetadataCache.isAuthorityValidated(authorityUrl);
        if (isAuthorityValidated || isAdfsAuthority && mAuthContext.getIsAuthorityValidated()) {
            return;
        }

        Logger.v(TAG, "Start validating authority");
        mDiscovery.setCorrelationId(correlationId);

        Discovery.verifyAuthorityValidInstance(authorityUrl);

        if (!isSilent && isAdfsAuthority && domain != null) {
            mDiscovery.validateAuthorityADFS(authorityUrl, domain);
        } else {
            if (isSilent && UrlExtensions.isADFSAuthority(authorityUrl)) {
                Logger.v(TAG, "Silent request. Skipping AD FS authority validation");
            }

            mDiscovery.validateAuthority(authorityUrl);
        }

        Logger.v(TAG, "The passed in authority is valid.");
        mAuthContext.setIsAuthorityValidated(true);
    }
    */
}
