package com.microsoft.identity.common.internal.providers.oauth2;

import java.net.URL;
import java.util.List;

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
    private List<String> mSupportedScopes;
    private List<String> mResponseTypesSupported;
    private List<String> mACRValuesSupported;
    private List<String> mSubjectTypesSupported;
    private List<String> mIDTokenSigningAlgValuesSupported;
    private List<String> mClaimsSupported;


}
