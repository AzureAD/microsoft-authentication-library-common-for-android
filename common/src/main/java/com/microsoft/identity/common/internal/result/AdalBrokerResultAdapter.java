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

import android.accounts.AccountManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.adal.internal.ADALError;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.java.commands.AcquirePrtSsoTokenResult;
import com.microsoft.identity.common.java.constants.OAuth2ErrorCode;
import com.microsoft.identity.common.java.exception.ArgumentException;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.IntuneAppProtectionPolicyRequiredException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.exception.UserCancelException;
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.java.util.SchemaUtil;
import com.microsoft.identity.common.java.dto.IAccountRecord;
import com.microsoft.identity.common.logging.Logger;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CliTelemInfo.RT_AGE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CliTelemInfo.SERVER_ERROR;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CliTelemInfo.SERVER_SUBERROR;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CliTelemInfo.SPE_RING;

public class AdalBrokerResultAdapter implements IBrokerResultAdapter {

    private static final String TAG = AdalBrokerResultAdapter.class.getName();

    @Override
    public @NonNull Bundle bundleFromAuthenticationResult(@NonNull final ILocalAuthenticationResult authenticationResult,
                                                          @Nullable final String negotiatedBrokerProtocolVersion) {
        final String methodTag = TAG + ":bundleFromAuthenticationResult";

        Logger.verbose(methodTag, "Constructing success bundle from Authentication Result.");
        final Bundle resultBundle = new Bundle();

        IAccountRecord accountRecord = authenticationResult.getAccountRecord();
        resultBundle.putString(
                AuthenticationConstants.Broker.ACCOUNT_LOGIN_HINT,
                accountRecord.getUsername()
        );
        resultBundle.putString(
                AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID,
                accountRecord.getLocalAccountId()
        );
        resultBundle.putString(
                AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE,
                accountRecord.getUsername()
        );
        resultBundle.putString(
                AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME,
                accountRecord.getFirstName()
        );
        resultBundle.putString(
                AuthenticationConstants.Broker.ACCOUNT_USERINFO_FAMILY_NAME,
                accountRecord.getFamilyName()
        );
        resultBundle.putString(
                AuthenticationConstants.Broker.ACCOUNT_USERINFO_IDENTITY_PROVIDER,
                SchemaUtil.getIdentityProvider(authenticationResult.getIdToken())
        );
        resultBundle.putString(
                AuthenticationConstants.Broker.ACCOUNT_USERINFO_TENANTID,
                authenticationResult.getTenantId()
        );

        resultBundle.putLong(
                AuthenticationConstants.Broker.ACCOUNT_EXPIREDATE,
                authenticationResult.getExpiresOn().getTime()
        );

        resultBundle.putString(
                AuthenticationConstants.Broker.ACCOUNT_AUTHORITY,
                getAuthority(authenticationResult)
        );

        resultBundle.putString(
                AuthenticationConstants.Broker.ACCOUNT_ACCESS_TOKEN,
                authenticationResult.getAccessToken()
        );

        resultBundle.putString(
                AuthenticationConstants.Broker.ACCOUNT_IDTOKEN,
                authenticationResult.getIdToken()
        );

        resultBundle.putString(
                SPE_RING,
                authenticationResult.getSpeRing()
        );

        resultBundle.putString(
                RT_AGE,
                authenticationResult.getRefreshTokenAge()
        );

        return resultBundle;
    }

    @Override
    public @NonNull Bundle bundleFromBaseException(@NonNull BaseException baseException,
                                                   @Nullable final String negotiatedBrokerProtocolVersion) {
        final String methodTag = TAG + ":bundleFromBaseException";

        Logger.verbose(methodTag, "Constructing error bundle from exception.");
        final Bundle resultBundle = new Bundle();

        resultBundle.putString(
                AuthenticationConstants.Browser.RESPONSE_ERROR_CODE,
                baseException.getErrorCode()
        );

        resultBundle.putString(
                AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE,
                baseException.getMessage()
        );

        // Set the SPE Ring & Client Telemetry info, if avail...
        resultBundle.putString(SPE_RING, baseException.getSpeRing());
        resultBundle.putString(RT_AGE, baseException.getRefreshTokenAge());
        resultBundle.putString(SERVER_ERROR, baseException.getCliTelemErrorCode());
        resultBundle.putString(SERVER_SUBERROR, baseException.getCliTelemSubErrorCode());

        mapExceptionToBundle(resultBundle, baseException);

        return resultBundle;
    }

    @Override
    public @NonNull
    ILocalAuthenticationResult authenticationResultFromBundle(Bundle resultBundle) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NonNull BaseException getBaseExceptionFromBundle(Bundle resultBundle) {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public AcquirePrtSsoTokenResult getAcquirePrtSsoTokenResultFromBundle(Bundle resultBundle) {
        throw new UnsupportedOperationException();
    }

    /**
     * Helper method to map and add errors to Adal specific constants.
     */
    private void mapExceptionToBundle(@NonNull final Bundle resultBundle,
                                      @NonNull BaseException exception) {
        final String methodTag = TAG + ":mapExceptionToBundle";

        if (exception instanceof UserCancelException) {

            Logger.info(methodTag, "Setting Bundle result from UserCancelException.");
            setErrorToResultBundle(
                    resultBundle,
                    AccountManager.ERROR_CODE_CANCELED,
                    exception.getMessage());

        } else if (exception instanceof ArgumentException) {

            Logger.info(methodTag, "Setting Bundle result from ArgumentException.");
            setErrorToResultBundle(
                    resultBundle,
                    AccountManager.ERROR_CODE_BAD_ARGUMENTS,
                    exception.getMessage()
            );

        } else if (exception instanceof ClientException) {

            setClientExceptionPropertiesToBundle(
                    resultBundle,
                    (ClientException) exception
            );

        } else if (exception instanceof ServiceException) {

            setServiceExceptionPropertiesToBundle(
                    resultBundle,
                    (ServiceException) exception);

        } else {

            Logger.info(methodTag, "Setting Bundle result for Unknown Exception/Bad result.");

            setErrorToResultBundle(
                    resultBundle,
                    AccountManager.ERROR_CODE_BAD_REQUEST,
                    exception.getMessage()
            );
        }

    }

    /**
     * Helper method to get result authority.
     */
    private String getAuthority(@NonNull final ILocalAuthenticationResult authenticationResult) {
        final String protocol = "https";
        Uri.Builder builder = new Uri.Builder().scheme(protocol);
        builder.authority(authenticationResult.getAccessTokenRecord().getEnvironment());

        if (!TextUtils.isEmpty(authenticationResult.getTenantId())) {
            builder.appendPath(authenticationResult.getTenantId());
        } else {
            builder.appendPath("common");
        }
        return builder.build().toString();
    }

    private void setClientExceptionPropertiesToBundle(@NonNull final Bundle resultBundle,
                                                      @NonNull final ClientException clientException) {
        final String methodTag = TAG + ":setClientExceptionPropertiesToBundle";
        Logger.info(methodTag, "Setting properties from ClientException.");

        if (ErrorStrings.DEVICE_NETWORK_NOT_AVAILABLE.equalsIgnoreCase(clientException.getErrorCode())) {

            setErrorToResultBundle(
                    resultBundle,
                    AccountManager.ERROR_CODE_NETWORK_ERROR,
                    ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE.getDescription()
            );

        } else if (ErrorStrings.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION.equalsIgnoreCase(clientException.getErrorCode())) {

            setErrorToResultBundle(
                    resultBundle,
                    AccountManager.ERROR_CODE_NETWORK_ERROR,
                    ADALError.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION.getDescription()
            );

        } else if (ErrorStrings.IO_ERROR.equalsIgnoreCase(clientException.getErrorCode())) {
            setErrorToResultBundle(
                    resultBundle,
                    AccountManager.ERROR_CODE_NETWORK_ERROR,
                    ADALError.IO_EXCEPTION.getDescription()
            );
        }
    }


    /**
     * Helper method to set Service Exception to Bundle
     */
    @SuppressWarnings("deprecation")
    private void setServiceExceptionPropertiesToBundle(@NonNull final Bundle resultBundle,
                                                       @NonNull final ServiceException serviceException) {
        final String methodTag = TAG + ":setServiceExceptionPropertiesToBundle";

        Logger.info(methodTag, "Setting properties from ServiceException.");

        // Silent call in ADAL expects these calls which differs from intercative adal call,
        // so adding values to these constants as well
        resultBundle.putString(AuthenticationConstants.OAuth2.ERROR, serviceException.getErrorCode());
        resultBundle.putString(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION, serviceException.getMessage());
        resultBundle.putString(AuthenticationConstants.OAuth2.SUBERROR, serviceException.getOAuthSubErrorCode());

        if (null != serviceException.getHttpResponseBody()) {
            resultBundle.putSerializable(
                    AuthenticationConstants.OAuth2.HTTP_RESPONSE_BODY,
                    serviceException.getHttpResponseBody()
            );
        }

        if (null != serviceException.getHttpResponseHeaders()) {
            resultBundle.putSerializable(
                    AuthenticationConstants.OAuth2.HTTP_RESPONSE_HEADER,
                    serviceException.getHttpResponseHeaders()
            );
        }
        resultBundle.putInt(
                AuthenticationConstants.OAuth2.HTTP_STATUS_CODE,
                serviceException.getHttpStatusCode()
        );

        if (serviceException instanceof IntuneAppProtectionPolicyRequiredException) {
            Logger.verbose(methodTag, "Exception was IntuneAppProtectionPolicyRequired");
            setIntuneAppProtectionPropertiesToBundle(
                    resultBundle,
                    (IntuneAppProtectionPolicyRequiredException) serviceException
            );
        }

        //INTERACTION_REQUIRED is marked as deprecated
        if (OAuth2ErrorCode.INVALID_GRANT.equalsIgnoreCase(serviceException.getErrorCode())
                || OAuth2ErrorCode.INTERACTION_REQUIRED.equalsIgnoreCase(serviceException.getErrorCode())) {

            resultBundle.putString(
                    AuthenticationConstants.OAuth2.ERROR,
                    ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED.getDescription()
            );

            resultBundle.putString(
                    AuthenticationConstants.OAuth2.ERROR_DESCRIPTION,
                    serviceException.getMessage()
            );
        }
    }

    /**
     * Helper method to IntuneAppProtectionPolicyRequiredException properties to bundle
     *
     * @param resultBundle
     * @param exception
     */
    private void setIntuneAppProtectionPropertiesToBundle(@NonNull final Bundle resultBundle,
                                                          @NonNull final IntuneAppProtectionPolicyRequiredException exception) {
        final String methodTag = TAG + ":setIntuneAppProtectionPropertiesToBundle";

        Logger.info(methodTag, "Setting properties from IntuneAppProtectionPolicyRequiredException.");

        resultBundle.putString(
                AuthenticationConstants.Browser.RESPONSE_ERROR_CODE,
                ADALError.AUTH_FAILED_INTUNE_POLICY_REQUIRED.name()
        );
        resultBundle.putString(
                AuthenticationConstants.Broker.ACCOUNT_USERINFO_TENANTID,
                exception.getTenantId()
        );
        resultBundle.putString(
                AuthenticationConstants.Broker.ACCOUNT_AUTHORITY,
                exception.getAuthorityUrl()
        );
        resultBundle.putString(
                AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID,
                exception.getAccountUserId()
        );
        resultBundle.putString(
                AuthenticationConstants.Broker.ACCOUNT_NAME,
                exception.getAccountUpn()
        );
    }

    /**
     * Helper method to set Bundle with Account manager error keys as expected by Adal
     */
    private void setErrorToResultBundle(@NonNull final Bundle resultBundle,
                                        @NonNull int error,
                                        @NonNull final String errorDescription) {
        resultBundle.putInt(
                AccountManager.KEY_ERROR_CODE,
                error
        );
        resultBundle.putString(
                AccountManager.KEY_ERROR_MESSAGE,
                errorDescription
        );
    }
}
