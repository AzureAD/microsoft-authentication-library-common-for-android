package com.microsoft.identity.common.internal.providers.azureactivedirectory;

import android.support.annotation.Nullable;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;

import java.net.URL;
import java.util.UUID;



public class AzureActiveDirectoryOAuth2Strategy extends OAuth2Strategy {

    protected void validateAuthoriztionRequest(AuthorizationRequest request){

    }
    protected void validateTokenRequest(AuthorizationRequest request){

    }
    protected Account createAccount(){
        Account a = new AzureActiveDirectoryAccount();
        return a;
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
