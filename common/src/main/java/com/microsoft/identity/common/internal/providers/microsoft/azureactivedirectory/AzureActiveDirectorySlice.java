package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@EqualsAndHashCode
@Accessors(prefix = "m")
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AzureActiveDirectorySlice {

    public final static String SLICE_PARAMETER = "slice";
    public final static String DC_PARAMETER = "dc";

    @SerializedName(SLICE_PARAMETER)
    private String mSlice;
    @SerializedName(DC_PARAMETER)
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

    public void setDataCenter(String dc) {
        mDataCenter = dc;
    }

}
