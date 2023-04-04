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
package com.microsoft.identity.common.java.providers.oauth2;

import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.ACR_VALUES_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.AUTHORIZATION_ENDPOINT;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.CHECK_SESSION_IFRAME;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.CLAIMS_LOCALES_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.CLAIMS_PARAMETER_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.CLAIMS_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.CLAIM_TYPES_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.CLOUD_GRAPH_HOST_NAME;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.CLOUD_INSTANCE_NAME;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.DEVICE_AUTHORIZATION_ENDPOINT;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.DISPLAY_VALUES_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.END_SESSION_ENDPOINT;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.FRONTCHANNEL_LOGOUT_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.GRANT_TYPES_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.HTTP_LOGOUT_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.ISSUER;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.JWKS_URI;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.MSGRAPH_HOST;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.OP_POLICY_URI;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.OP_TOS_URI;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.RBAC_URL;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.REGISTRATION_ENDPOINT;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.REQUEST_PARAMETER_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.REQUEST_URI_PARAMETER_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.REQUIRE_REQUEST_URI_REGISTRATION;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.RESPONSE_MODES_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.RESPONSE_TYPES_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.SCOPES_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.SERVICE_DOCUMENTATION;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.SUBJECT_TYPES_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.TENANT_REGION_SCOPE;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.TENANT_REGION_SUB_SCOPE;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.TOKEN_ENDPOINT;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.UI_LOCALES_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.USERINFO_ENCRYPTION_ALG_VALUES_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.USERINFO_ENCRYPTION_ENC_VALUES_SUPPORTED;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.USERINFO_ENDPOINT;
import static com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration.SerializedNames.USERINFO_SIGNING_ALG_VALUES_SUPPORTED;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.Getter;
import lombok.experimental.Accessors;

/*
 * Represents the information returned from the OpenID Provider Configuration Endpoint
 * https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata
 */
@SuppressWarnings("PMD")
@Getter
@Accessors(prefix = "m")
public class OpenIdProviderConfiguration {

    /**
     * Keys for JSON parsing.
     */
    public static final class SerializedNames {
        public static final String
                AUTHORIZATION_ENDPOINT = "authorization_endpoint",
                DEVICE_AUTHORIZATION_ENDPOINT = "device_authorization_endpoint",
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
                TENANT_REGION_SUB_SCOPE = "tenant_region_sub_scope",
                CLOUD_INSTANCE_NAME = "cloud_instance_name",
                CLOUD_GRAPH_HOST_NAME = "cloud_graph_host_name",
                MSGRAPH_HOST = "msgraph_host",
                RBAC_URL = "rbac_url",
                REGISTRATION_ENDPOINT = "registration_endpoint",
                GRANT_TYPES_SUPPORTED = "grant_types_supported",
                ACR_VALUES_SUPPORTED = "acr_values_supported",
                ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED = "id_token_encryption_alg_values_supported",
                ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED = "id_token_encryption_enc_values_supported",
                USERINFO_SIGNING_ALG_VALUES_SUPPORTED = "userinfo_signing_alg_values_supported",
                USERINFO_ENCRYPTION_ALG_VALUES_SUPPORTED = "userinfo_encryption_alg_values_supported",
                USERINFO_ENCRYPTION_ENC_VALUES_SUPPORTED = "userinfo_encryption_enc_values_supported",
                REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED = "request_object_signing_alg_values_supported",
                REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED = "request_object_encryption_alg_values_supported",
                REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED = "request_object_encryption_enc_values_supported",
                TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED = "token_endpoint_auth_signing_alg_values_supported",
                DISPLAY_VALUES_SUPPORTED = "display_values_supported",
                CLAIM_TYPES_SUPPORTED = "claim_types_supported",
                SERVICE_DOCUMENTATION = "service_documentation",
                CLAIMS_LOCALES_SUPPORTED = "claims_locales_supported",
                UI_LOCALES_SUPPORTED = "ui_locales_supported",
                CLAIMS_PARAMETER_SUPPORTED = "claims_parameter_supported",
                REQUEST_PARAMETER_SUPPORTED = "request_parameter_supported",
                REQUIRE_REQUEST_URI_REGISTRATION = "require_request_uri_registration",
                OP_POLICY_URI = "op_policy_uri",
                OP_TOS_URI = "op_tos_uri",
                CHECK_SESSION_IFRAME = "check_session_iframe";
    }

    @SerializedName(AUTHORIZATION_ENDPOINT)
    private String mAuthorizationEndpoint;

    @SerializedName(DEVICE_AUTHORIZATION_ENDPOINT)
    private String mDeviceAuthorizationEndpoint;

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

    @SerializedName(TENANT_REGION_SUB_SCOPE)
    private String mTenantRegionSubScope;

    @SerializedName(CLOUD_INSTANCE_NAME)
    private String mCloudInstanceName;

    @SerializedName(CLOUD_GRAPH_HOST_NAME)
    private String mCloudGraphHostName;

    @SerializedName(MSGRAPH_HOST)
    private String mMsGraphHost;

    @SerializedName(RBAC_URL)
    private String mRbacUrl;

    @SerializedName(REGISTRATION_ENDPOINT)
    private String mRegistrationEndpoint;

    @SerializedName(GRANT_TYPES_SUPPORTED)
    private List<String> mGrantTypesSupported;

    @SerializedName(ACR_VALUES_SUPPORTED)
    private List<String> mAcrValuesSupported;

    @SerializedName(ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED)
    private List<String> mIdTokenEncryptionAlgValuesSupported;

    @SerializedName(ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED)
    private List<String> mIdTokenEncryptionEncValuesSupported;

    @SerializedName(USERINFO_SIGNING_ALG_VALUES_SUPPORTED)
    private List<String> mUserInfoSigningAlgValuesSupported;

    @SerializedName(USERINFO_ENCRYPTION_ALG_VALUES_SUPPORTED)
    private List<String> mUserInfoEncryptionAlgValuesSupported;

    @SerializedName(USERINFO_ENCRYPTION_ENC_VALUES_SUPPORTED)
    private List<String> mUserInfoEncryptionEncValueSupported;

    @SerializedName(REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED)
    private List<String> mRequestObjectSigningAlgValuesSupported;

    @SerializedName(REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED)
    private List<String> mRequestObjectEncryptionAlgValuesSupported;

    @SerializedName(REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED)
    private List<String> mRequestObjectEncryptionEncValuesSupported;

    @SerializedName(TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED)
    private List<String> mTokenEndpointAuthSigningAlgValuesSupported;

    @SerializedName(DISPLAY_VALUES_SUPPORTED)
    private List<String> mDisplayValuesSupported;

    @SerializedName(CLAIM_TYPES_SUPPORTED)
    private List<String> mClaimTypesSupported;

    @SerializedName(SERVICE_DOCUMENTATION)
    private String mServiceDocumentation;

    @SerializedName(CLAIMS_LOCALES_SUPPORTED)
    private List<String> mClaimsLocalesSupported;

    @SerializedName(UI_LOCALES_SUPPORTED)
    private List<String> mUiLocalesSupported;

    @SerializedName(CLAIMS_PARAMETER_SUPPORTED)
    private Boolean mClaimsParameterSupported;

    @SerializedName(REQUEST_PARAMETER_SUPPORTED)
    private Boolean mRequestParameterSupported;

    @SerializedName(REQUIRE_REQUEST_URI_REGISTRATION)
    private Boolean mRequireRequestUriRegistration;

    @SerializedName(OP_POLICY_URI)
    private String mOpPolicyUri;

    @SerializedName(OP_TOS_URI)
    private String mOpTosUri;

    @SerializedName(CHECK_SESSION_IFRAME)
    private String mCheckSessionIFrame;
}
