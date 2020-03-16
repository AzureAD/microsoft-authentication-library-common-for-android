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
package com.microsoft.identity.common.internal.eststelemetry;

import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.internal.util.StringUtil;

public class TelemetryUtils {

    static String errorFromAcquireTokenResult(final AcquireTokenResult acquireTokenResult) {
        if (acquireTokenResult == null) {
            return "unknown_error";
        }

        final String errorFromAuthorization = getErrorFromAuthorizationResult(acquireTokenResult.getAuthorizationResult());

        if (errorFromAuthorization != null) {
            return errorFromAuthorization;
        } else {
            return getErrorFromTokenResult(acquireTokenResult.getTokenResult());
        }
    }

    private static String getErrorFromAuthorizationResult(final AuthorizationResult authorizationResult) {
        if (authorizationResult != null && authorizationResult.getErrorResponse() != null) {
            return authorizationResult.getErrorResponse().getError();
        } else {
            return null;
        }
    }

    private static String getErrorFromTokenResult(final TokenResult tokenResult) {
        if (tokenResult != null && tokenResult.getErrorResponse() != null) {
            return tokenResult.getErrorResponse().getError();
        } else {
            return null;
        }
    }

    static boolean getBooleanFromSchemaString(final String val) {
        return val.equals(SchemaConstants.Value.TRUE);
    }

    static String getSchemaCompliantStringFromBoolean(final boolean val) {
        return val ? SchemaConstants.Value.TRUE : SchemaConstants.Value.FALSE;
    }

    static String getSchemaCompliantString(final String s) {
        if (StringUtil.isEmpty(s)) {
            return SchemaConstants.Value.EMPTY;
        } else if (s.equals(TelemetryEventStrings.Value.TRUE)) {
            return SchemaConstants.Value.TRUE;
        } else if (s.equals(TelemetryEventStrings.Value.FALSE)) {
            return SchemaConstants.Value.FALSE;
        } else {
            return s;
        }
    }

}
