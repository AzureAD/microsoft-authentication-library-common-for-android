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
package com.microsoft.identity.common.internal.broker;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;

import java.util.Date;

/**
 * Encapsulates the broker token response
 */
public class BrokerTokenResponse extends MicrosoftStsTokenResponse implements Parcelable{

    private String mAuthority;
    private String mTenantId;

    @SerializedName("not_before")
    private Date mExpiresNotBefore;

    @SerializedName("cloud_instance_host_name")
    private String mCloudInstanceHostName;

    public BrokerTokenResponse(MicrosoftStsTokenResponse microsoftStsTokenResponse){
        setExpiresIn(microsoftStsTokenResponse.getExpiresIn());
        setAccessToken(microsoftStsTokenResponse.getAccessToken());
        setTokenType(microsoftStsTokenResponse.getTokenType());
        setRefreshToken(microsoftStsTokenResponse.getRefreshToken());
        setScope(microsoftStsTokenResponse.getScope());
        setState(microsoftStsTokenResponse.getState());
        setIdToken(microsoftStsTokenResponse.getIdToken());
        setResponseReceivedTime(microsoftStsTokenResponse.getResponseReceivedTime());
        setExtExpiresOn(microsoftStsTokenResponse.getExtExpiresOn());
        setClientInfo(microsoftStsTokenResponse.getClientInfo());
        setClientId(microsoftStsTokenResponse.getClientId());
        setExtExpiresIn(microsoftStsTokenResponse.getExtExpiresIn());
        setFamilyId(microsoftStsTokenResponse.getFamilyId());
    }

    protected BrokerTokenResponse(Parcel in) {
        if(in != null) {
            setExpiresIn(in.readLong());
            setAccessToken(in.readString());
            setTokenType(in.readString());
            setRefreshToken(in.readString());
            setScope(in.readString());
            setState(in.readString());
            setIdToken(in.readString());
            setResponseReceivedTime(in.readLong());
            setExtExpiresOn(new Date(in.readLong()));
            setClientInfo(in.readString());
            setClientId(in.readString());
            setExtExpiresIn(in.readLong());
            setFamilyId(in.readString());
            setAuthority(in.readString());
            setTenantId(in.readString());
            setExpiresNotBefore(new Date(in.readLong()));
            setCloudInstanceHostName(in.readString());
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (dest != null) {
            dest.writeLong(getExpiresIn() != null ? getExpiresIn() : 0);
            dest.writeString(getAccessToken());
            dest.writeString(getTokenType());
            dest.writeString(getRefreshToken());
            dest.writeString(getScope());
            dest.writeString(getState());
            dest.writeString(getIdToken());
            dest.writeLong(getResponseReceivedTime());
            dest.writeLong(getExtExpiresOn() != null ? getExtExpiresOn().getTime() : 0);
            dest.writeString(getClientInfo());
            dest.writeString(getClientId());
            dest.writeLong(getExtExpiresIn() != null ? getExtExpiresIn() : 0);
            dest.writeString(getFamilyId());
            dest.writeString(getAuthority());
            dest.writeString(getTenantId());
            dest.writeLong(getExpiresNotBefore() != null ? getExpiresNotBefore().getTime() : 0);
            dest.writeString(getCloudInstanceHostName());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BrokerTokenResponse> CREATOR = new Creator<BrokerTokenResponse>() {
        @Override
        public BrokerTokenResponse createFromParcel(final Parcel in) {
            return new BrokerTokenResponse(in);
        }

        @Override
        public BrokerTokenResponse[] newArray(int size) {
            return new BrokerTokenResponse[size];
        }
    };

    public String getAuthority() {
        return mAuthority;
    }

    public void setAuthority(final String authority) {
        mAuthority = authority;
    }

    public String getTenantId() {
        return mTenantId;
    }

    public void setTenantId(final String tenantId) {
        mTenantId = tenantId;
    }

    public Date getExpiresNotBefore() {
        return mExpiresNotBefore;
    }

    public void setExpiresNotBefore(final Date expiresNotBefore) {
        mExpiresNotBefore = expiresNotBefore;
    }

    public String getCloudInstanceHostName() {
        return mCloudInstanceHostName;
    }

    public void setCloudInstanceHostName(final String cloudInstanceHostName) {
        this.mCloudInstanceHostName = cloudInstanceHostName;
    }
}
