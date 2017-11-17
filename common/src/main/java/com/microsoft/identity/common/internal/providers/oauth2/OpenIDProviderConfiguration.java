package com.microsoft.identity.common.internal.providers.oauth2;

import java.net.URL;
import java.util.ArrayList;

/*
 * Represents the information returned from the OpenID Provider Configuration Endpoint
 * https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata
 */

public class OpenIDProviderConfiguration {

    private String mIssuer;
    private URL mAuthorizationEndpoint;
    private URL mTokenEndpoint;
    private URL mUserInfoEndpoint;
    private URL mJWKSUri;
    private ArrayList<String> mSupportedScopes;
    private ArrayList<String> mResponseTypesSupported;
    private ArrayList<String> mACRValuesSupported;
    private ArrayList<String> mSubjectTypesSupported;
    private ArrayList<String> mIDTokenSigningAlgValuesSupported;
    private ArrayList<String> mClaimsSupported;


}
