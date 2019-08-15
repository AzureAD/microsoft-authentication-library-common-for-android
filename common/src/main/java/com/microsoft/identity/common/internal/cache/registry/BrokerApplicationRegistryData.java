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
package com.microsoft.identity.common.internal.cache.registry;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.cache.AbstractApplicationMetadata;

/**
 * A basic registry (key/value) entry for tracking info about apps which bind to the
 * broker.
 * <p>
 * This class is fine to expand over time if more properties need to be added - make note however,
 * that if you add more properties, you need to regenerate the hashCode() and equals()
 * implementation.
 */
public class BrokerApplicationRegistryData extends AbstractApplicationMetadata {

    private static final class SerializedNames extends AbstractApplicationMetadata.SerializedNames {
        static final String ALLOW_WPJ_ACCESS = "wpj_account_access_allowed";
    }

    @SerializedName(SerializedNames.ALLOW_WPJ_ACCESS)
    private boolean mWpjAccountAccessAllowed;

    /**
     * Gets the WPJ Account access state flag.
     *
     * @return True if account access is allowed. False otherwise.
     */
    public boolean isWpjAccountAccessAllowed() {
        return mWpjAccountAccessAllowed;
    }

    /**
     * Sets the WPJ Account access state flag.
     *
     * @param allow True if access should be allowed to the binding app. False otherwise.
     */
    public void setWpjAccountAccessAllowed(final boolean allow) {
        mWpjAccountAccessAllowed = allow;
    }

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BrokerApplicationRegistryData that = (BrokerApplicationRegistryData) o;

        return mWpjAccountAccessAllowed == that.mWpjAccountAccessAllowed;
    }
    //CHECKSTYLE:ON

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (mWpjAccountAccessAllowed ? 1 : 0);
        return result;
    }
    //CHECKSTYLE:ON
}
