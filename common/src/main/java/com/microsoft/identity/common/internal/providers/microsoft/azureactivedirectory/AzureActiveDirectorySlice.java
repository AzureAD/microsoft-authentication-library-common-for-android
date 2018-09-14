package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import com.google.gson.annotations.SerializedName;

public class AzureActiveDirectorySlice {

    @SerializedName("slice")
    private String mSlice;
    @SerializedName("dc")
    private String mDataCenter;

    public String getSlice() {
        return mSlice;
    }

    public String getDC() {
        return mDataCenter;
    }

    public void setSlice(String slice) {
        mSlice = slice;
    }

    public void setDataCenter(String dc){
        mDataCenter = dc;
    }

}
