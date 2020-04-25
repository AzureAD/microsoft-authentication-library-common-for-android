package com.microsoft.identity.internal.testutils.labutils;

import androidx.annotation.NonNull;

import com.microsoft.identity.internal.test.labapi.model.ConfigInfo;
import com.microsoft.identity.internal.test.labapi.model.TempUser;

public class LabConfig {

    private static LabConfig sCurrentLabConfig;

    public static LabConfig getCurrentLabConfig() {
        return sCurrentLabConfig;
    }

    public static void setCurrentLabConfig(LabConfig currentLabConfig) {
        LabConfig.sCurrentLabConfig = currentLabConfig;
    }

    private ConfigInfo mConfigInfo;
    private TempUser mTempUser;
    private String mLabUserPassword;

    public LabConfig(@NonNull ConfigInfo configInfo, String labUserPassword) {
        this.mConfigInfo = configInfo;
        this.mLabUserPassword = labUserPassword;
    }

    public LabConfig(@NonNull ConfigInfo configInfo) {
        this(configInfo, null);
    }

    public LabConfig(@NonNull TempUser tempUser, String labUserPassword) {
        this.mTempUser = tempUser;
        this.mLabUserPassword = labUserPassword;
    }

    public LabConfig(@NonNull TempUser tempUser) {
        this(tempUser, null);
    }

    public ConfigInfo getConfigInfo() {
        return mConfigInfo;
    }

    public TempUser getTempUser() {
        return mTempUser;
    }

    public String getLabUserPassword() {
        return mLabUserPassword;
    }

    public void setLabUserPassword(String labUserPassword) {
        this.mLabUserPassword = labUserPassword;
    }

    public String getAuthority() {
        if (mConfigInfo != null) {
            return mConfigInfo.getLabInfo().getAuthority();
        } else if (mTempUser != null) {
            return mTempUser.getAuthority();
        } else {
            return null;
        }
    }

    public String getAppId() {
        if (mConfigInfo == null) {
            return null;
        }

        return mConfigInfo.getAppInfo().getAppId();
    }
}
