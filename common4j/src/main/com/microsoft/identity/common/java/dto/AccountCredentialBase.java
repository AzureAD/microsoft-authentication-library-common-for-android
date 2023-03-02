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
package com.microsoft.identity.common.java.dto;

import com.google.gson.JsonElement;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import lombok.NonNull;

/**
 * Base class for Objects to support the [de]/serialization of extra fields.
 */
public abstract class AccountCredentialBase {

    private transient Map<String, JsonElement> mAdditionalFields = Collections.synchronizedMap(new HashMap<String, JsonElement>());

    /**
     * Getter of additional fields.
     *
     * @return additional fields in Map<String, JsonElement>
     */
    public Map<String, JsonElement> getAdditionalFields() {
        return mAdditionalFields;
    }

    /**
     * Setter of additional fields.
     *
     * @param additionalFields Map<String, JsonElement>
     */
    public void setAdditionalFields(@NonNull final Map<String, JsonElement> additionalFields) {
        mAdditionalFields = Collections.synchronizedMap(additionalFields);
    }

    /**
     * Adds the additionalFields of the input AccountCredentialBase to this instance.
     *
     * @param other The input AccountCredentialBase to merge with this instance.
     */
    public void mergeAdditionalFields(@NonNull final AccountCredentialBase other) {
        if (null == mAdditionalFields) {
            mAdditionalFields = Collections.synchronizedMap(new HashMap<String, JsonElement>());
        }

        if (null != other.getAdditionalFields()) {
            for (final Map.Entry<String, JsonElement> entry : other.getAdditionalFields().entrySet()) {
                // Only add elements not present in the existing collection, so that old data
                // does not overwrite new data...
                if (mAdditionalFields.containsKey(entry.getKey())) {
                    continue;
                }

                mAdditionalFields.put(entry.getKey(), entry.getValue());
            }
        }
    }

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @Override
    public String toString() {
        return "AccountCredentialBase{"
                + "mAdditionalFields="
                + mAdditionalFields
                + '}';
    }
    //CHECKSTYLE:ON
}
