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

package com.microsoft.identity.common.java.flighting;

import static com.microsoft.identity.common.java.commands.SilentTokenCommand.ACQUIRE_TOKEN_SILENT_DEFAULT_TIMEOUT_MILLISECONDS;

import lombok.NonNull;

/**
 * List of Active Common flights.
 */
public enum CommonFlight implements IFlightConfig {
    /**
     * Flight to control whether or not to use Network capability for performing network check.
     */
    USE_NETWORK_CAPABILITY_FOR_NETWORK_CHECK("UseNetworkCapabilityForNetworkCheck", false),
    /**
     * Flight to control whether to expose the CCS (CachedCredService) request ID in TokenResponse.
     * This flight is default-on 
     */
    EXPOSE_CCS_REQUEST_ID_IN_TOKENRESPONSE("ExposeCcsRequestIdInTokenResponse", true),
    /**
     * Flight to control whether to expose the CCS (CachedCredService) request sequence in TokenResponse.
     * This flight is default-on 
     */
    EXPOSE_CCS_REQUEST_SEQUENCE_IN_TOKENRESPONSE("ExposeCcsRequestSequenceInTokenResponse", true),

    /**
     * Flight to control the timeout duration for Acquire Token Silent Calls
     * The default value is set to {@link ACQUIRE_TOKEN_SILENT_DEFAULT_TIMEOUT_MILLISECONDS}
     */
    ACQUIRE_TOKEN_SILENT_TIMEOUT_MILLISECONDS("AcquireTokenSilentTimeoutMilliSeconds", ACQUIRE_TOKEN_SILENT_DEFAULT_TIMEOUT_MILLISECONDS),

    /**
     * Flight to be able to disable/rollback the passkey feature in broker if necessary.
     * This will be set to true by default, once this feature is confirmed for the next release.
     */
    ENABLE_PASSKEY_FEATURE("EnablePasskeyFeature", true);

    private String key;
    private Object defaultValue;
    CommonFlight(@NonNull String key, @NonNull Object defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public Object getDefaultValue() {
        return this.defaultValue;
    }
}
