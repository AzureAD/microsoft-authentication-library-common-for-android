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
package com.microsoft.identity.common.java.request;

import com.microsoft.identity.common.java.AuthenticationConstants;

/**
 * Enum to indicate if the request came from an app hosting Adal or Msal Sdk
 */
public enum SdkType {
    ADAL,
    MSAL,
    MSAL_CPP,
    UNKNOWN;

    /**
     * Method for mapping the SdkType to appropriate String
     * for the purpose of sending it to the telemetry.
     *
     * @return MSAL.Android for case of ADAL and MSAL,
     * MSAL.xplat.Android for case of MSAL_CPP,
     * empty string otherwise(UNKNOWN).
     */
    public String getProductName() {
        if ((SdkType.ADAL == this) || (SdkType.MSAL == this)) {
            return AuthenticationConstants.SdkPlatformFields.PRODUCT_NAME_MSAL;
        } else if ((SdkType.MSAL_CPP == this)) {
            return AuthenticationConstants.SdkPlatformFields.PRODUCT_NAME_MSAL_CPP;
        } else {
            // value(SdkType.UNKNOWN) is intended for test-cases, eg. CommandDispatcherTest.java.
            return "";
        }

    }

    /**
     * Determines if the Sdk supports Microsoft Personal Accounts.
     *
     * @return True if Sdk supports Microsoft Personal Accounts, false otherwise.
     */
    public boolean isCapableOfMSA() {
        return (this == SdkType.MSAL) || (this == SdkType.MSAL_CPP);
    }
}
