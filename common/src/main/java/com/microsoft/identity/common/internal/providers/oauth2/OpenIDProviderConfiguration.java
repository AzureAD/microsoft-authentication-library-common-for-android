// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.providers.oauth2;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import static com.microsoft.identity.common.internal.providers.oauth2.OpenIDProviderConfiguration.SerializedNames.*;

/*
 * Represents the information returned from the OpenID Provider Configuration Endpoint
 * https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata
 */
@SuppressWarnings("PMD")
public class OpenIDProviderConfiguration {

    /**
     * Keys for JSON parsing.
     */
    public static final class SerializedNames {
        public static final String
                AUTHORIZATION_ENDPOINT = "authorization_endpoint",
                TOKEN_ENDPOINT = "token_endpoint",
                TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED = "token_endpoint_auth_methods_supported",
                JWKS_URI = "jwks_uri",
                RESPONSE_MODES_SUPPORTED = "response_modes_supported",
                SUBJECT_TYPES_SUPPORTED = "subject_types_supported",
                ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED = "id_token_signing_alg_values_supported",
                HTTP_LOGOUT_SUPPORTED = "http_logout_supported",
                FRONTCHANNEL_LOGOUT_SUPPORTED = "frontchannel_logout_supported",
                END_SESSION_ENDPOINT = "end_session_endpoint",
                RESPONSE_TYPES_SUPPORTED = "response_types_supported",
                SCOPES_SUPPORTED = "scopes_supported",
                ISSUER = "issuer",
                CLAIMS_SUPPORTED = "claims_supported",
                REQUEST_URI_PARAMETER_SUPPORTED = "request_uri_parameter_supported",
                USERINFO_ENDPOINT = "userinfo_endpoint",
                TENANT_REGION_SCOPE = "tenant_region_scope",
                CLOUD_INSTANCE_NAME = "cloud_instance_name",
                CLOUD_GRAPH_HOST_NAME = "cloud_graph_host_name",
                MSGRAPH_HOST = "msgraph_host",
                RBAC_URL = "rbac_url";
    }

    @SerializedName(AUTHORIZATION_ENDPOINT)
    private String mAuthorizationEndpoint;

    @SerializedName(TOKEN_ENDPOINT)
    private String mTokenEndpoint;

    @SerializedName(TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED)
    private List<String> mTokenEndpointAuthMethodsSupported;

    @SerializedName(JWKS_URI)
    private String mJwksUri;

    @SerializedName(RESPONSE_MODES_SUPPORTED)
    private List<String> mResponseModesSupported;

    @SerializedName(SUBJECT_TYPES_SUPPORTED)
    private List<String> mSubjectTypesSupported;

    @SerializedName(ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED)
    private List<String> mIdTokenSigningAlgValuesSupported;

    @SerializedName(HTTP_LOGOUT_SUPPORTED)
    private Boolean mHttpLogoutSupported;

    @SerializedName(FRONTCHANNEL_LOGOUT_SUPPORTED)
    private Boolean mFrontChannelLogoutSupported;

    @SerializedName(END_SESSION_ENDPOINT)
    private String mEndSessionEndpoint;

    @SerializedName(RESPONSE_TYPES_SUPPORTED)
    private List<String> mResponseTypesSupported;

    @SerializedName(SCOPES_SUPPORTED)
    private List<String> mScopesSupported;

    @SerializedName(ISSUER)
    private String mIssuer;

    @SerializedName(CLAIMS_SUPPORTED)
    private List<String> mClaimsSupported;

    @SerializedName(REQUEST_URI_PARAMETER_SUPPORTED)
    private Boolean mRequestUriParameterSupported;

    @SerializedName(USERINFO_ENDPOINT)
    private String mUserInfoEndpoint;

    @SerializedName(TENANT_REGION_SCOPE)
    private String mTenantRegionScope;

    @SerializedName(CLOUD_INSTANCE_NAME)
    private String mCloudInstanceName;

    @SerializedName(CLOUD_GRAPH_HOST_NAME)
    private String mCloudGraphHostName;

    @SerializedName(MSGRAPH_HOST)
    private String mMsGraphHost;

    @SerializedName(RBAC_URL)
    private String mRbacUrl;

    public String getAuthorizationEndpoint() {
        return mAuthorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String mAuthorizationEndpoint) {
        this.mAuthorizationEndpoint = mAuthorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return mTokenEndpoint;
    }

    public void setTokenEndpoint(String mTokenEndpoint) {
        this.mTokenEndpoint = mTokenEndpoint;
    }

    public List<String> getTokenEndpointAuthMethodsSupported() {
        return mTokenEndpointAuthMethodsSupported;
    }

    public void setTokenEndpointAuthMethodsSupported(List<String> mTokenEndpointAuthMethodsSupported) {
        this.mTokenEndpointAuthMethodsSupported = mTokenEndpointAuthMethodsSupported;
    }

    public String getJwksUri() {
        return mJwksUri;
    }

    public void setJwksUri(String mJwksUri) {
        this.mJwksUri = mJwksUri;
    }

    public List<String> getResponseModesSupported() {
        return mResponseModesSupported;
    }

    public void setResponseModesSupported(List<String> mResponseModesSupported) {
        this.mResponseModesSupported = mResponseModesSupported;
    }

    public List<String> getSubjectTypesSupported() {
        return mSubjectTypesSupported;
    }

    public void setSubjectTypesSupported(List<String> mSubjectTypesSupported) {
        this.mSubjectTypesSupported = mSubjectTypesSupported;
    }

    public List<String> getIdTokenSigningAlgValuesSupported() {
        return mIdTokenSigningAlgValuesSupported;
    }

    public void setIdTokenSigningAlgValuesSupported(List<String> mIdTokenSigningAlgValuesSupported) {
        this.mIdTokenSigningAlgValuesSupported = mIdTokenSigningAlgValuesSupported;
    }

    public Boolean getHttpLogoutSupported() {
        return mHttpLogoutSupported;
    }

    public void setHttpLogoutSupported(Boolean mHttpLogoutSupported) {
        this.mHttpLogoutSupported = mHttpLogoutSupported;
    }

    public Boolean getFrontChannelLogoutSupported() {
        return mFrontChannelLogoutSupported;
    }

    public void setFrontChannelLogoutSupported(Boolean mFrontChannelLogoutSupported) {
        this.mFrontChannelLogoutSupported = mFrontChannelLogoutSupported;
    }

    public String getEndSessionEndpoint() {
        return mEndSessionEndpoint;
    }

    public void setEndSessionEndpoint(String mEndSessionEndpoint) {
        this.mEndSessionEndpoint = mEndSessionEndpoint;
    }

    public List<String> getResponseTypesSupported() {
        return mResponseTypesSupported;
    }

    public void setResponseTypesSupported(List<String> mResponseTypesSupported) {
        this.mResponseTypesSupported = mResponseTypesSupported;
    }

    public List<String> getScopesSupported() {
        return mScopesSupported;
    }

    public void setScopesSupported(List<String> mScopesSupported) {
        this.mScopesSupported = mScopesSupported;
    }

    public String getIssuer() {
        return mIssuer;
    }

    public void setIssuer(String mIssuer) {
        this.mIssuer = mIssuer;
    }

    public List<String> getClaimsSupported() {
        return mClaimsSupported;
    }

    public void setClaimsSupported(List<String> mClaimsSupported) {
        this.mClaimsSupported = mClaimsSupported;
    }

    public Boolean getRequestUriParameterSupported() {
        return mRequestUriParameterSupported;
    }

    public void setRequestUriParameterSupported(Boolean mRequestUriParameterSupported) {
        this.mRequestUriParameterSupported = mRequestUriParameterSupported;
    }

    public String getUserInfoEndpoint() {
        return mUserInfoEndpoint;
    }

    public void setUserInfoEndpoint(String mUserInfoEndpoint) {
        this.mUserInfoEndpoint = mUserInfoEndpoint;
    }

    public String getTenantRegionScope() {
        return mTenantRegionScope;
    }

    public void setTenantRegionScope(String mTenantRegionScope) {
        this.mTenantRegionScope = mTenantRegionScope;
    }

    public String getCloudInstanceName() {
        return mCloudInstanceName;
    }

    public void setCloudInstanceName(String mCloudInstanceName) {
        this.mCloudInstanceName = mCloudInstanceName;
    }

    public String getCloudGraphHostName() {
        return mCloudGraphHostName;
    }

    public void setCloudGraphHostName(String mCloudGraphHostName) {
        this.mCloudGraphHostName = mCloudGraphHostName;
    }

    public String getMsGraphHost() {
        return mMsGraphHost;
    }

    public void setMsGraphHost(String mMsGraphHost) {
        this.mMsGraphHost = mMsGraphHost;
    }

    public String getRbacUrl() {
        return mRbacUrl;
    }

    public void setRbacUrl(String mRbacUrl) {
        this.mRbacUrl = mRbacUrl;
    }
}
