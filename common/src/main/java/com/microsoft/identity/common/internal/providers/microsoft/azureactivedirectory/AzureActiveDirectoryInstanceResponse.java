package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class AzureActiveDirectoryInstanceResponse {

    @SerializedName("tenant_discovery_endpoint")
    private String mTestDiscoveryEndpoint;
    @SerializedName("api-version")
    private String mApiVersion;
    @SerializedName("metadata")
    private ArrayList<AzureActiveDirectoryCloud> mClouds;


    public String getTestDiscoveryEndpoint() {
        return mTestDiscoveryEndpoint;
    }

    public void setTestDiscoveryEndpoint(String testDiscoveryEndpoint) {
        this.mTestDiscoveryEndpoint = mTestDiscoveryEndpoint;
    }

    public String getApiVersion() {
        return mApiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.mApiVersion = mApiVersion;
    }

    public ArrayList<AzureActiveDirectoryCloud> getClouds() {
        return mClouds;
    }

    public void setClouds(ArrayList<AzureActiveDirectoryCloud> clouds) {
        this.mClouds = mClouds;
    }
}
