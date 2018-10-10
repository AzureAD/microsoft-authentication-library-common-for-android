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
package com.microsoft.identity.common.internal.dto;

import com.google.gson.annotations.SerializedName;

import static com.microsoft.identity.common.internal.dto.RefreshTokenRecord.SerializedNames.FAMILY_ID;
import static com.microsoft.identity.common.internal.dto.RefreshTokenRecord.SerializedNames.TARGET;

public class RefreshTokenRecord extends Credential {

    public static class SerializedNames extends Credential.SerializedNames {
        /**
         * String of family id.
         */
        public static final String FAMILY_ID = "family_id";
        /**
         * String of target.
         */
        public static final String TARGET = "target";
    }

    /**
     * 1st Party Application Family ID.
     */
    @SerializedName(FAMILY_ID)
    private String mFamilyId;

    /**
     * Permissions that are included in the token. Formats for endpoints will be different. 
     * <p>
     * Mandatory, if credential is scoped down by some parameters or requirements (e.g. by
     * resource, scopes or permissions).
     */
    @SerializedName(TARGET)
    private String mTarget;

    /**
     * Gets the target.
     *
     * @return The target to get.
     */
    public String getTarget() {
        return mTarget;
    }

    /**
     * Sets the target.
     *
     * @param target The target to set.
     */
    public void setTarget(final String target) {
        mTarget = target;
    }

    /**
     * Gets the family_id.
     *
     * @return The family_id to get.
     */
    public String getFamilyId() {
        return mFamilyId;
    }

    /**
     * Sets the family_id.
     *
     * @param familyId The family_id to set.
     */
    public void setFamilyId(final String familyId) {
        mFamilyId = familyId;
    }

    @Override
    public boolean isExpired() {
        return false;
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

        RefreshTokenRecord that = (RefreshTokenRecord) o;

        if (mFamilyId != null ? !mFamilyId.equals(that.mFamilyId) : that.mFamilyId != null)
            return false;
        return mTarget != null ? mTarget.equals(that.mTarget) : that.mTarget == null;
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
        result = 31 * result + (mFamilyId != null ? mFamilyId.hashCode() : 0);
        result = 31 * result + (mTarget != null ? mTarget.hashCode() : 0);
        return result;
    }
    //CHECKSTYLE:ON

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public String toString() {
        return "RefreshToken{" +
                "mFamilyId='" + mFamilyId + '\'' +
                ", mTarget='" + mTarget + '\'' +
                "} " + super.toString();
    }
    //CHECKSTYLE:ON
}
