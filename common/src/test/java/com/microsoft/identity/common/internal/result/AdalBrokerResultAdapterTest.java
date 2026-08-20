//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.result;

import static android.accounts.AccountManager.KEY_AUTHTOKEN;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CliTelemInfo.RT_AGE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CliTelemInfo.SERVER_ERROR;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CliTelemInfo.SERVER_SUBERROR;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CliTelemInfo.SPE_RING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.accounts.AccountManager;
import android.os.Bundle;

import com.microsoft.identity.common.adal.internal.ADALError;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.java.constants.OAuth2ErrorCode;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.exception.ArgumentException;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.IntuneAppProtectionPolicyRequiredException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.exception.UserCancelException;
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AdalBrokerResultAdapterTest {

    private static final String ID_TOKEN_WITH_IDP_CLAIM = "eyJhbGciOiJub25lIn0.eyJpZHAiOiJsaXZlLmNvbSJ9.";

    private final AdalBrokerResultAdapter mAdapter = new AdalBrokerResultAdapter();

    @Test
    public void bundleFromAuthenticationResult_whenTenantPresent_setsAdalSuccessBundle() {
        final Date expiresOn = new Date(123456789L);
        final ILocalAuthenticationResult authenticationResult = mockAuthenticationResult(
                "login.microsoftonline.com",
                "tenant-id",
                expiresOn
        );

        final Bundle bundle = mAdapter.bundleFromAuthenticationResult(authenticationResult, "1.0");

        assertEquals("user@contoso.com", bundle.getString(AuthenticationConstants.Broker.ACCOUNT_LOGIN_HINT));
        assertEquals("local-account-id", bundle.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID));
        assertEquals("user@contoso.com", bundle.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE));
        assertEquals("Ada", bundle.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME));
        assertEquals("Lovelace", bundle.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_FAMILY_NAME));
        // ID token is not a parseable JWT, so SchemaUtil.getIdentityProvider(...) returns null.
        assertNull(bundle.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_IDENTITY_PROVIDER));
        assertEquals("tenant-id", bundle.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_TENANTID));
        assertEquals(expiresOn.getTime(), bundle.getLong(AuthenticationConstants.Broker.ACCOUNT_EXPIREDATE));
        assertEquals("https://login.microsoftonline.com/tenant-id", bundle.getString(AuthenticationConstants.Broker.ACCOUNT_AUTHORITY));
        assertEquals("access-token", bundle.getString(AuthenticationConstants.Broker.ACCOUNT_ACCESS_TOKEN));
        assertEquals(ID_TOKEN_WITH_IDP_CLAIM, bundle.getString(AuthenticationConstants.Broker.ACCOUNT_IDTOKEN));
        assertEquals("spe-ring", bundle.getString(SPE_RING));
        assertEquals("rt-age", bundle.getString(RT_AGE));
        assertEquals("access-token", bundle.getString(KEY_AUTHTOKEN));
    }

    @Test
    public void bundleFromAuthenticationResult_whenTenantEmpty_usesCommonAuthorityPath() {
        final ILocalAuthenticationResult authenticationResult = mockAuthenticationResult(
                "login.windows.net",
                "",
                new Date(1L)
        );

        final Bundle bundle = mAdapter.bundleFromAuthenticationResult(authenticationResult, null);

        assertEquals("https://login.windows.net/common", bundle.getString(AuthenticationConstants.Broker.ACCOUNT_AUTHORITY));
        assertEquals("", bundle.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_TENANTID));
    }

    @Test
    public void bundleFromBaseException_whenUserCancel_setsCanceledAccountManagerError() {
        final UserCancelException exception = new UserCancelException("user_cancel", "User cancelled");

        final Bundle bundle = mAdapter.bundleFromBaseException(exception, null);

        assertCommonExceptionFields(bundle, "user_cancel", "User cancelled");
        assertEquals(AccountManager.ERROR_CODE_CANCELED, bundle.getInt(AccountManager.KEY_ERROR_CODE));
        assertEquals("User cancelled", bundle.getString(AccountManager.KEY_ERROR_MESSAGE));
    }

    @Test
    public void bundleFromBaseException_whenArgumentException_setsBadArgumentsAccountManagerError() {
        final ArgumentException exception = new ArgumentException(
                ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME,
                ArgumentException.SCOPE_ARGUMENT_NAME,
                "Invalid scopes"
        );

        final Bundle bundle = mAdapter.bundleFromBaseException(exception, null);

        assertCommonExceptionFields(bundle, ArgumentException.ILLEGAL_ARGUMENT_ERROR_CODE, "Invalid scopes");
        assertEquals(AccountManager.ERROR_CODE_BAD_ARGUMENTS, bundle.getInt(AccountManager.KEY_ERROR_CODE));
        assertEquals("Invalid scopes", bundle.getString(AccountManager.KEY_ERROR_MESSAGE));
    }

    @Test
    public void bundleFromBaseException_whenUnknownBaseException_setsBadRequestAccountManagerError() {
        final BaseException exception = new BaseException("unknown_error", "Unknown failure");
        exception.setSpeRing("spe");
        exception.setRefreshTokenAge("age");
        exception.setCliTelemErrorCode("server-error");
        exception.setCliTelemSubErrorCode("server-suberror");

        final Bundle bundle = mAdapter.bundleFromBaseException(exception, null);

        assertCommonExceptionFields(bundle, "unknown_error", "Unknown failure");
        assertEquals("spe", bundle.getString(SPE_RING));
        assertEquals("age", bundle.getString(RT_AGE));
        assertEquals("server-error", bundle.getString(SERVER_ERROR));
        assertEquals("server-suberror", bundle.getString(SERVER_SUBERROR));
        assertEquals(AccountManager.ERROR_CODE_BAD_REQUEST, bundle.getInt(AccountManager.KEY_ERROR_CODE));
        assertEquals("Unknown failure", bundle.getString(AccountManager.KEY_ERROR_MESSAGE));
    }

    @Test
    public void bundleFromBaseException_whenClientExceptionIsDeviceNetworkUnavailable_setsNetworkError() {
        final ClientException exception = new ClientException(
                ErrorStrings.DEVICE_NETWORK_NOT_AVAILABLE,
                "No network"
        );

        final Bundle bundle = mAdapter.bundleFromBaseException(exception, null);

        assertCommonExceptionFields(bundle, ErrorStrings.DEVICE_NETWORK_NOT_AVAILABLE, "No network");
        assertEquals(AccountManager.ERROR_CODE_NETWORK_ERROR, bundle.getInt(AccountManager.KEY_ERROR_CODE));
        assertEquals(ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE.getDescription(), bundle.getString(AccountManager.KEY_ERROR_MESSAGE));
    }

    @Test
    public void bundleFromBaseException_whenClientExceptionIsPowerOptimization_setsNetworkError() {
        final ClientException exception = new ClientException(
                ErrorStrings.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION,
                "Network blocked by doze"
        );

        final Bundle bundle = mAdapter.bundleFromBaseException(exception, null);

        assertCommonExceptionFields(bundle, ErrorStrings.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION, "Network blocked by doze");
        assertEquals(AccountManager.ERROR_CODE_NETWORK_ERROR, bundle.getInt(AccountManager.KEY_ERROR_CODE));
        assertEquals(ADALError.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION.getDescription(), bundle.getString(AccountManager.KEY_ERROR_MESSAGE));
    }

    @Test
    public void bundleFromBaseException_whenClientExceptionIsIoError_setsNetworkError() {
        final ClientException exception = new ClientException(ErrorStrings.IO_ERROR, "IO failed");

        final Bundle bundle = mAdapter.bundleFromBaseException(exception, null);

        assertCommonExceptionFields(bundle, ErrorStrings.IO_ERROR, "IO failed");
        assertEquals(AccountManager.ERROR_CODE_NETWORK_ERROR, bundle.getInt(AccountManager.KEY_ERROR_CODE));
        assertEquals(ADALError.IO_EXCEPTION.getDescription(), bundle.getString(AccountManager.KEY_ERROR_MESSAGE));
    }

    @Test
    public void bundleFromBaseException_whenClientExceptionIsUnmapped_setsOnlyCommonExceptionFields() {
        final ClientException exception = new ClientException(ClientException.UNKNOWN_ERROR, "Client failed");

        final Bundle bundle = mAdapter.bundleFromBaseException(exception, null);

        assertCommonExceptionFields(bundle, ClientException.UNKNOWN_ERROR, "Client failed");
        assertFalse(bundle.containsKey(AccountManager.KEY_ERROR_CODE));
        assertFalse(bundle.containsKey(AccountManager.KEY_ERROR_MESSAGE));
    }

    @Test
    public void bundleFromBaseException_whenServiceException_setsOAuthAndHttpFields() {
        final ServiceException exception = new ServiceException("service_error", "Service failed", 503, null);
        exception.setSubErrorCode("sub-error");
        final HashMap<String, String> responseBody = new HashMap<>();
        responseBody.put("error", "body-error");
        final HashMap<String, List<String>> responseHeaders = new HashMap<>();
        responseHeaders.put("x-ms-request-id", Collections.singletonList("request-id"));
        exception.setHttpResponseBody(responseBody);
        exception.setHttpResponseHeaders(responseHeaders);

        final Bundle bundle = mAdapter.bundleFromBaseException(exception, null);

        assertCommonExceptionFields(bundle, "service_error", "Service failed");
        assertEquals("service_error", bundle.getString(AuthenticationConstants.OAuth2.ERROR));
        assertEquals("Service failed", bundle.getString(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION));
        assertEquals("sub-error", bundle.getString(AuthenticationConstants.OAuth2.SUBERROR));
        assertEquals(503, bundle.getInt(AuthenticationConstants.OAuth2.HTTP_STATUS_CODE));
        assertEquals(responseBody, bundle.getSerializable(AuthenticationConstants.OAuth2.HTTP_RESPONSE_BODY));
        assertEquals(responseHeaders, bundle.getSerializable(AuthenticationConstants.OAuth2.HTTP_RESPONSE_HEADER));
    }

    @Test
    public void bundleFromBaseException_whenServiceExceptionIsInvalidGrant_mapsOauthErrorForAdal() {
        final ServiceException exception = new ServiceException(OAuth2ErrorCode.INVALID_GRANT, "Refresh failed", 400, null);

        final Bundle bundle = mAdapter.bundleFromBaseException(exception, null);

        assertCommonExceptionFields(bundle, OAuth2ErrorCode.INVALID_GRANT, "Refresh failed");
        assertEquals(ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED.getDescription(), bundle.getString(AuthenticationConstants.OAuth2.ERROR));
        assertEquals("Refresh failed", bundle.getString(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION));
    }

    @Test
    public void bundleFromBaseException_whenServiceExceptionIsInteractionRequired_mapsOauthErrorForAdal() {
        final ServiceException exception = new ServiceException(OAuth2ErrorCode.INTERACTION_REQUIRED, "Interaction required", 400, null);

        final Bundle bundle = mAdapter.bundleFromBaseException(exception, null);

        assertCommonExceptionFields(bundle, OAuth2ErrorCode.INTERACTION_REQUIRED, "Interaction required");
        assertEquals(ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED.getDescription(), bundle.getString(AuthenticationConstants.OAuth2.ERROR));
        assertEquals("Interaction required", bundle.getString(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION));
    }

    @Test
    public void bundleFromBaseException_whenIntunePolicyRequired_setsIntuneAdalFields() {
        final IntuneAppProtectionPolicyRequiredException exception =
                new IntuneAppProtectionPolicyRequiredException("intune_error", "Policy required");
        exception.setTenantId("tenant-id");
        exception.setAuthorityUrl("https://login.microsoftonline.com/tenant-id");
        exception.setAccountUserId("local-account-id");
        exception.setAccountUpn("user@contoso.com");

        final Bundle bundle = mAdapter.bundleFromBaseException(exception, null);

        assertEquals(ADALError.AUTH_FAILED_INTUNE_POLICY_REQUIRED.name(), bundle.getString(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE));
        assertEquals("Policy required", bundle.getString(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE));
        assertEquals("tenant-id", bundle.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_TENANTID));
        assertEquals("https://login.microsoftonline.com/tenant-id", bundle.getString(AuthenticationConstants.Broker.ACCOUNT_AUTHORITY));
        assertEquals("local-account-id", bundle.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID));
        assertEquals("user@contoso.com", bundle.getString(AuthenticationConstants.Broker.ACCOUNT_NAME));
        assertEquals("intune_error", bundle.getString(AuthenticationConstants.OAuth2.ERROR));
        assertEquals("Policy required", bundle.getString(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void bundleFromBaseExceptionForWebApps_throwsUnsupportedOperationException() {
        mAdapter.bundleFromBaseExceptionForWebApps(new BaseException("error", "message"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void authenticationResultFromBundle_throwsUnsupportedOperationException() throws BaseException {
        mAdapter.authenticationResultFromBundle(new Bundle());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void bundleFromAuthenticationResultForWebApps_throwsUnsupportedOperationException() throws BaseException {
        mAdapter.bundleFromAuthenticationResultForWebApps(mock(ILocalAuthenticationResult.class), null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getBaseExceptionFromBundle_throwsUnsupportedOperationException() {
        mAdapter.getBaseExceptionFromBundle(new Bundle());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getAcquirePrtSsoTokenResultFromBundle_throwsUnsupportedOperationException() {
        mAdapter.getAcquirePrtSsoTokenResultFromBundle(new Bundle());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getAcquirePrtSsoTokenBatchResultFromBundle_throwsUnsupportedOperationException() {
        mAdapter.getAcquirePrtSsoTokenBatchResultFromBundle(new Bundle());
    }

    private ILocalAuthenticationResult mockAuthenticationResult(final String environment,
                                                                final String tenantId,
                                                                final Date expiresOn) {
        final AccountRecord accountRecord = new AccountRecord();
        accountRecord.setUsername("user@contoso.com");
        accountRecord.setLocalAccountId("local-account-id");
        accountRecord.setFirstName("Ada");
        accountRecord.setFamilyName("Lovelace");

        final AccessTokenRecord accessTokenRecord = new AccessTokenRecord();
        accessTokenRecord.setEnvironment(environment);

        final ILocalAuthenticationResult authenticationResult = mock(ILocalAuthenticationResult.class);
        when(authenticationResult.getAccountRecord()).thenReturn(accountRecord);
        when(authenticationResult.getAccessTokenRecord()).thenReturn(accessTokenRecord);
        when(authenticationResult.getTenantId()).thenReturn(tenantId);
        when(authenticationResult.getExpiresOn()).thenReturn(expiresOn);
        when(authenticationResult.getAccessToken()).thenReturn("access-token");
        when(authenticationResult.getIdToken()).thenReturn(ID_TOKEN_WITH_IDP_CLAIM);
        when(authenticationResult.getSpeRing()).thenReturn("spe-ring");
        when(authenticationResult.getRefreshTokenAge()).thenReturn("rt-age");
        return authenticationResult;
    }

    private void assertCommonExceptionFields(final Bundle bundle,
                                             final String errorCode,
                                             final String errorMessage) {
        assertEquals(errorCode, bundle.getString(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE));
        assertEquals(errorMessage, bundle.getString(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE));
    }
}
