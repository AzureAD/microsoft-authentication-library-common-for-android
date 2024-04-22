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
package com.microsoft.identity.common.java.cache;

import com.google.gson.annotations.SerializedName;

/**
 * A representation of a broker-enabled app; adding a property for FoCI id to supplement properties
 * defined on {@link AbstractApplicationMetadata}.
 * <p>
 * Please note that two applications are "the same" if their client_id, environment, and app UID
 * (user) are the same. An app may not simultaneously be both FoCI and non-FoCI.
 */
public class BrokerApplicationMetadata extends AbstractApplicationMetadata {

    private static final class SerializedNames extends AbstractApplicationMetadata.SerializedNames {
        static final String FAMILY_ID = "family_id";
    }

    @SerializedName(SerializedNames.FAMILY_ID)
    private String mFoci;

    public String getFoci() {
        return mFoci;
    }

    public void setFoci(final String mFoci) {
        this.mFoci = mFoci;
    }

    /**
     * Tests if two instances of {@link BrokerApplicationMetadata} are equivalent. Please note that
     * family id (FoCI status) is not considered when determine "app equality".
     *
     * @param o The object to compare for equality.
     * @return True if the apps are equal.
     */
    @Override
    public boolean equals(Object o) {
        // This class intentionally no longer includes foci state to determine equality.
        // This is because an app may transition to/from FoCI.
        return super.equals(o);
    }

    /**
     * Returns a hash code value for the object. Please note that family id (FoCI status) is not a
     * valued hashed by this function.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        // This class intentionally no longer includes foci state to determine equality.
        // This is because an app may transition to/from FoCI.
        return super.hashCode();
    }
}
