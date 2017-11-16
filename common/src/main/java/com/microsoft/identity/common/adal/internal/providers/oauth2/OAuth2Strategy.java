package com.microsoft.identity.common.adal.internal.providers.oauth2;

import com.microsoft.identity.common.Account;
import android.net.Uri;

/**
 * Created by shoatman on 11/15/2017.
 */

public abstract class OAuth2Strategy {

    protected String mTokenEndpoint;
    protected String mAuthorizationEndpoint;
    protected Uri mIssuer;

    protected AuthorizationResponse requestAuthorization(AuthorizationRequest request, AuthorizationStrategy authorizationStrategy){
        validateAuthoriztionRequest(request);
        Uri authorizationUri = createAuthorizationUri();
        AuthorizationResult result = authorizationStrategy.requestAuthorization(request);
        //TODO: Reconcile authorization result and response
        AuthorizationResponse response = new AuthorizationResponse();
        return response;
    }

    protected Uri createAuthorizationUri(){
        //final Uri.Builder builder = new Uri.Builder().scheme(originalAuthority.getProtocol()).authority(host).appendPath(path);
        Uri authorizationUri = Uri.withAppendedPath(mIssuer, mAuthorizationEndpoint);
        return authorizationUri;
    }

    protected abstract Account createAccount();

    protected abstract void validateAuthoriztionRequest(AuthorizationRequest request);
    protected abstract void validateTokenRequest(AuthorizationRequest request);
}
