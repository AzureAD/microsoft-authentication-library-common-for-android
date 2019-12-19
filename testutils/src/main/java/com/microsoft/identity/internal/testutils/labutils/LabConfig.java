package com.microsoft.identity.internal.testutils.labutils;

import com.microsoft.identity.internal.test.labapi.model.ConfigInfo;

public class LabConfig {

    private static LabConfig sCurrentLabConfig;

    public static LabConfig getCurrentLabConfig() {
        return sCurrentLabConfig;
    }

    public static void setCurrentLabConfig(LabConfig currentLabConfig) {
        LabConfig.sCurrentLabConfig = currentLabConfig;
    }

    private ConfigInfo mConfigInfo;

    private String mLabUserPassword;

    public LabConfig(ConfigInfo configInfo, String labUserPassword) {
        this.mConfigInfo = configInfo;
        this.mLabUserPassword = labUserPassword;
    }

    public LabConfig(ConfigInfo configInfo) {
        this.mConfigInfo = configInfo;
        this.mLabUserPassword = null;
    }

    public ConfigInfo getConfigInfo() {
        return mConfigInfo;
    }

    public String getLabUserPassword() {
        return mLabUserPassword;
    }

    public void setLabUserPassword(String labUserPassword) {
        this.mLabUserPassword = labUserPassword;
    }

    public String getAuthority() {
        if (mConfigInfo == null) {
            return null;
        }

        return mConfigInfo.getLabInfo().getAuthority();
    }

    public String getAppId() {
        if (mConfigInfo == null) {
            return null;
        }

        return mConfigInfo.getAppInfo().getAppId();
    }
}
