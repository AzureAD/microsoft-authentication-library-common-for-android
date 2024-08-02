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
package com.microsoft.identity.common.java.providers;

import com.microsoft.identity.common.java.controllers.ExceptionAdapter;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.UrlUtil;
import com.microsoft.identity.common.java.util.ported.PropertyBag;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import static com.microsoft.identity.common.java.AuthenticationConstants.AAD.APP_LINK_KEY;
import static com.microsoft.identity.common.java.AuthenticationConstants.AAD.DEVICE_REGISTRATION_REDIRECT_URI_HOSTNAME;
import static com.microsoft.identity.common.java.AuthenticationConstants.AAD.REDIRECT_PREFIX;
import static com.microsoft.identity.common.java.AuthenticationConstants.Browser.RESPONSE_EXCEPTION;
import static com.microsoft.identity.common.java.AuthenticationConstants.Browser.RESPONSE_FINAL_URL;
import static com.microsoft.identity.common.java.AuthenticationConstants.Browser.SUB_ERROR_UI_CANCEL;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterFields.RESULT_CODE;
import static com.microsoft.identity.common.java.AuthenticationConstants.OAuth2.ERROR_SUBCODE;

/**
 * Raw results from an Authorization session.
 * This will contains one of the following
 * 1. a 'final' redirect URL (which could either contain a success or an error returned from the server).
 * 2. a client-side exception.
 * 3. only the result code, such as user cancellation, SDK cancellation, MDM flow indicator, etc.
 */
@Builder(access = AccessLevel.PRIVATE)
@Getter
@Accessors(prefix = "m")
public class RawAuthorizationResult {
    private static final String TAG = RawAuthorizationResult.class.getSimpleName();

    public enum ResultCode {
        UNKNOWN(-1),

        /**
         * Represents that user cancelled the flow,
         * or the auth session was cancelled in order to proceed with the flow
         * (i.e. cancelling the session to launch the broker app)
         *
         * This response code should not pop up any error.
         */
        CANCELLED(2001),

        /**
         * Represents an error/exceptions that is not returned from the server side as part of redirect uri.
         */
        NON_OAUTH_ERROR(2002),

        /**
         * Represents that the redirect url is returned successfully from the auth endpoint.
         * This redirect url could contain an error "returned from the server".
         */
        COMPLETED(2003),

        /**
         * CA flow, device doesn't have company portal or azure authenticator installed.
         * Waiting for broker package to be installed, and resume request in broker.
         */
        BROKER_INSTALLATION_TRIGGERED(2006),

        /**
         * The AuthZ session happens in broker, and WPJ is required.
         */
        DEVICE_REGISTRATION_REQUIRED(2007),

        /**
         * Represents that SDK signalled to cancelled the auth flow as app
         * launched a new interactive auth request.
         */
        SDK_CANCELLED(2008),

        /**
         * Indicates that MDM Flow is triggered.
         * Some apps uses this signal to optimize user experiences.
         */
        MDM_FLOW(2009);

        private final int mCode;

        ResultCode(final int code){
            mCode = code;
        }

        static ResultCode fromInteger(@Nullable final Integer value){
            if (value == null){
                return ResultCode.UNKNOWN;
            }

            for (final ResultCode resultCode : values()) {
                if (resultCode.mCode == value) {
                    return resultCode;
                }
            }

            return null;
        }
    }

    private final ResultCode mResultCode;

    @Nullable
    private final URI mAuthorizationFinalUri;

    @Nullable
    private final BaseException mException;

    @NonNull
    public static RawAuthorizationResult fromResultCode(final ResultCode resultCode) {
        if (resultCode == ResultCode.NON_OAUTH_ERROR ||
                resultCode == ResultCode.COMPLETED ||
                resultCode == ResultCode.DEVICE_REGISTRATION_REQUIRED ||
                resultCode == ResultCode.BROKER_INSTALLATION_TRIGGERED) {
            throw new IllegalArgumentException("Result code " + resultCode + " should be set via other factory methods");
        }

        return RawAuthorizationResult.builder()
                .resultCode(resultCode)
                .build();
    }

    @NonNull
    public static RawAuthorizationResult fromThrowable(@NonNull final Throwable e) {
        if (e instanceof BaseException) {
            return fromException((BaseException) e);
        }
        return RawAuthorizationResult.builder()
                .resultCode(ResultCode.NON_OAUTH_ERROR)
                .exception(ExceptionAdapter.baseExceptionFromException(e))
                .build();
    }

    @NonNull
    public static RawAuthorizationResult fromException(@NonNull final BaseException e) {
        return RawAuthorizationResult.builder()
                .resultCode(ResultCode.NON_OAUTH_ERROR)
                .exception(e)
                .build();
    }

    @NonNull
    public static RawAuthorizationResult fromRedirectUri(@NonNull final String redirectUri) {
        try {
            final URI uri = new URI(redirectUri);
            return RawAuthorizationResult.builder()
                    .resultCode(getResultCodeFromFinalRedirectUri(uri))
                    .authorizationFinalUri(uri)
                    .build();
        } catch (final URISyntaxException e) {
            return fromException(new ClientException(ClientException.MALFORMED_URL,
                    "Failed to parse redirect URL", e));
        }
    }

    @NonNull
    public static PropertyBag toPropertyBag(@NonNull final RawAuthorizationResult data) {
        final PropertyBag propertyBag = new PropertyBag();
        propertyBag.put(RESULT_CODE, data.mResultCode.mCode);
        propertyBag.put(RESPONSE_FINAL_URL, data.mAuthorizationFinalUri);
        propertyBag.put(RESPONSE_EXCEPTION, data.mException);
        return propertyBag;
    }

    @NonNull
    public static RawAuthorizationResult fromPropertyBag(@NonNull final PropertyBag propertyBag) {
        return RawAuthorizationResult.builder()
                .resultCode(ResultCode.fromInteger(propertyBag.<Integer>get(RESULT_CODE)))
                .authorizationFinalUri(propertyBag.<URI>get(RESPONSE_FINAL_URL))
                .exception((BaseException) propertyBag.<Serializable>get(RESPONSE_EXCEPTION))
                .build();
    }

    private static ResultCode getResultCodeFromFinalRedirectUri(@NonNull final URI uri) throws URISyntaxException {
        final String methodTag = TAG + "getResultCodeFromFinalRedirectUri";
        final Map<String, String> parameters = UrlUtil.getParameters(uri);

        if (REDIRECT_PREFIX.equalsIgnoreCase(uri.getScheme())) {
            // i.e. (Browser) msauth://com.msft.identity.client.sample.local/1wIqXSqBj7w%2Bh11ZifsnqwgyKrY%3D?wpj=1&username=idlab1%40msidlab4.onmicrosoft.com&app_link=https%3a%2f%2fplay.google.com%2fstore%2fapps%2fdetails%3fid%3dcom.azure.authenticator
            //      (WebView) msauth://wpj/?username=idlab1%40msidlab4.onmicrosoft.com&app_link=https%3a%2f%2fplay.google.com%2fstore%2fapps%2fdetails%3fid%3dcom.azure.authenticator%26referrer%3dcom.msft.identity.client.sample.local
            if (parameters.containsKey(APP_LINK_KEY)) {
                Logger.info(methodTag, "Return to caller with BROWSER_CODE_WAIT_FOR_BROKER_INSTALL, and waiting for result.");
                return ResultCode.BROKER_INSTALLATION_TRIGGERED;
            }

            // i.e. (both Browser and WebView) msauth://wpj/?username=idlab1%40msidlab4.onmicrosoft.com&client_info=[SOME_GUID]
            if (DEVICE_REGISTRATION_REDIRECT_URI_HOSTNAME.equalsIgnoreCase(uri.getHost())) {
                Logger.info(methodTag, " Device needs to be registered, sending BROWSER_CODE_DEVICE_REGISTER");
                return ResultCode.DEVICE_REGISTRATION_REQUIRED;
            }
        }

        if (StringUtil.equalsIgnoreCase(parameters.get(ERROR_SUBCODE), SUB_ERROR_UI_CANCEL)) {
            // when the user click the "cancel" button in the UI, server will send the the
            // redirect uri with "cancel" error sub-code and redirects back to the calling app
            Logger.info(methodTag, "User cancelled the session");
            return ResultCode.CANCELLED;
        }

        return ResultCode.COMPLETED;
    }
}
