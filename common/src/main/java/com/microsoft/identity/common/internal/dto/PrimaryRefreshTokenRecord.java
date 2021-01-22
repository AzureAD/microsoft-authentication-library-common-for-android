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

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.microsoft.identity.common.internal.dto.PrimaryRefreshTokenRecord.SerializedNames.*;

public class PrimaryRefreshTokenRecord extends Credential {

    public static class SerializedNames extends Credential.SerializedNames {
        /**
         * String of realm.
         */
        public static final String REALM = "realm";

        /**
         * String of session_key.
         */
        public static final String SESSION_KEY = "session_key";

        /**
         * String of session_key_rolling_date.
         */
        public static final String SESSION_KEY_ROLLING_DATE = "session_key_rolling_date";

        /**
         * String of prt_protocol_version.
         */
        public static final String PRT_PROTOCOL_VERSION = "prt_protocol_version";
    }

    /**
     * PRT expiry time. This value is returned from the server as refresh_token_expires_in that
     * should be added to local time.
     */
    @SerializedName(EXPIRES_ON)
    private String mExpiresOn;

    /**
     * Session key of PRT.
     */
    @SerializedName(SESSION_KEY)
    private String mSessionKey;

    /**
     * The version of the PRT Protocol being used, version 3 supports no device_id.
     */
    @SerializedName(PRT_PROTOCOL_VERSION)
    private String mPrtProtocolVersion;

    /**
     * Session key expiry time. This value is determined by the client and set to 60 days after the
     * session key was initally issued
     */
    @SerializedName(SESSION_KEY_ROLLING_DATE)
    private String mSessionKeyRollingDate;

    public boolean isExpired(final String expires) {
        // Init a Calendar for the current time/date
        final Calendar calendar = Calendar.getInstance();
        final Date validity = calendar.getTime();
        // Init a Date for the accessToken's expiry
        long epoch = Long.valueOf(expires);
        final Date expiresOn = new Date(
                TimeUnit.SECONDS.toMillis(epoch)
        );
        return expiresOn.before(validity);
    }

    @Override
    public boolean isExpired() {
        return isExpired(getExpiresOn());
    }

    /**
     * Gets the expires_on.
     * @return The expires_on to get.
     */
    public String getExpiresOn() {
        return mExpiresOn;
    }

    /**
     * Sets the expires_on.
     * @param expiresOn The expires_on to set.
     */
    public void setExpiresOn(String expiresOn) {
        mExpiresOn = expiresOn;
    }

    /**
     * Gets the session_key.
     * @return The session_key to get.
     */
    public String getSessionKey() {
        return mSessionKey;
    }

    /**
     * Sets the session_key.
     *
     * @param sessionKey The session_key to set.
     */
    public void setSessionKey(String sessionKey) {
        mSessionKey = sessionKey;
    }

    /**
     * Gets the prt_protocol_version.
     *
     * @return The prt_protocol_version to get.
     */
    public String getPrtProtocolVersion() {
        return mPrtProtocolVersion;
    }

    /**
     * Sets the prt_protocol_version.
     *
     * @param prtProtocolVersion The prt_protocol_version to set.
     */
    public void setPrtProtocolVersion(String prtProtocolVersion) {
        mPrtProtocolVersion = prtProtocolVersion;
    }

    /**
     * Gets the session_key_rolling_date.
     *
     * @return The session_key_rolling_date to get.
     */
    public String getSessionKeyRollingDate() {
        return mSessionKeyRollingDate;
    }

    /**
     * Sets the session_key_rolling_date.
     * @param sessionKeyRollingDate The session_key_rolling_date to set.
     */
    public void setSessionKeyRollingDate(String sessionKeyRollingDate) {
        mSessionKeyRollingDate = sessionKeyRollingDate;
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

        PrimaryRefreshTokenRecord that = (PrimaryRefreshTokenRecord) o;

        if (mExpiresOn != null ? !mExpiresOn.equals(that.mExpiresOn) : that.mExpiresOn != null)
            return false;
        if (mSessionKey != null ? !mSessionKey.equals(that.mSessionKey) : that.mSessionKey != null)
            return false;
        if (mPrtProtocolVersion != null ? !mPrtProtocolVersion.equals(that.mPrtProtocolVersion) : that.mPrtProtocolVersion != null)
            return false;
        return mSessionKeyRollingDate != null ? mSessionKeyRollingDate.equals(that.mSessionKeyRollingDate) : that.mSessionKeyRollingDate == null;
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
        result = 31 * result + (mExpiresOn != null ? mExpiresOn.hashCode() : 0);
        result = 31 * result + (mSessionKey != null ? mSessionKey.hashCode() : 0);
        result = 31 * result + (mPrtProtocolVersion != null ? mPrtProtocolVersion.hashCode() : 0);
        result = 31 * result + (mSessionKeyRollingDate != null ? mSessionKeyRollingDate.hashCode() : 0);
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
        return "PrimaryRefreshTokenRecord{" +
                "mExpiresOn='" + mExpiresOn + '\'' +
                ", mSessionKey='" + mSessionKey + '\'' +
                ", mPrtProtocolVersion='" + mPrtProtocolVersion + '\'' +
                ", mSessionKeyRollingDate='" + mSessionKeyRollingDate + '\'' +
                "} " + super.toString();
    }
    //CHECKSTYLE:ON
}
