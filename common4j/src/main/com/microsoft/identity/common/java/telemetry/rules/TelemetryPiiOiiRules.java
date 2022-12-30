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
package com.microsoft.identity.common.java.telemetry.rules;

import lombok.NonNull;

import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Device;
import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Key;

final public class TelemetryPiiOiiRules {
    private static TelemetryPiiOiiRules sInstance;
    private Set<String> piiPropertiesSet;
    private Set<String> oiiPropertiesSet;

    final private String[] piiArray = {
            Key.USER_ID,
            Device.ID,
            Key.LOGIN_HINT,
            Key.ERROR_DESCRIPTION,
            Key.REQUEST_QUERY_PARAMS,
            Key.REDIRECT_URI,
            Key.SCOPE,
            Key.CLAIM_REQUEST
    };

    final private String[] oiiArray = {
            Key.TENANT_ID,
            Key.CLIENT_ID,
            Key.REDIRECT_URI,
            Key.HTTP_PATH,
            Key.AUTHORITY,
            Key.IDP_NAME,
            Key.CALLER_APP_PACKAGE_NAME,
            Key.CALLER_APP_UUID,
            Key.CALLER_APP_VERSION
    };

    private TelemetryPiiOiiRules() {
        piiPropertiesSet = new HashSet<>(Arrays.asList(piiArray));
        oiiPropertiesSet = new HashSet<>(Arrays.asList(oiiArray));
    }

    @NonNull
    public synchronized static TelemetryPiiOiiRules getInstance() {
        if (sInstance == null) {
            sInstance = new TelemetryPiiOiiRules();
        }

        return sInstance;
    }

    /**
     * @param propertyName String of propertyName {@link TelemetryEventStrings}
     * @return true if the property belongs to Personally identifiable information. False otherwise.
     */
    public boolean isPii(final String propertyName) {
        if (StringUtil.isNullOrEmpty(propertyName)) {
            return false;
        }

        return piiPropertiesSet.contains(propertyName);
    }

    /**
     * @param propertyName String of propertyName {@link TelemetryEventStrings}
     * @return true if the property belongs to Objective identifiable information. False otherwise.
     */
    public boolean isOii(final String propertyName) {
        if (StringUtil.isNullOrEmpty(propertyName)) {
            return false;
        }

        return oiiPropertiesSet.contains(propertyName);
    }

    /**
     * @param propertyName String of propertyName {@link TelemetryEventStrings}
     * @return true if the property belongs to PII/OII. False otherwise.
     */
    public boolean isPiiOrOii(final String propertyName) {
        return isPii(propertyName) || isOii(propertyName);
    }
}