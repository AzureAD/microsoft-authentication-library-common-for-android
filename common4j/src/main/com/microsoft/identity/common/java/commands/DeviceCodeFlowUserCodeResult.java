package com.microsoft.identity.common.java.commands;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

@Builder
@Getter
@Accessors(prefix = "m")
public class DeviceCodeFlowUserCodeResult {
    @SerializedName("vUri")
     String vUri;
    @SerializedName("userCode")
     String userCode;
    @SerializedName("message")
    String message;
    @SerializedName("sessionExpirationDate")
     Date sessionExpirationDate;

    public DeviceCodeFlowUserCodeResult(String verificationUri, String userCode, String message, Date expiredDate) {
        vUri = verificationUri;
        this.userCode = userCode;
        this.message = message;
        sessionExpirationDate = expiredDate;
    }

    public String getUserCode() {
        return userCode;
    }

    public  String getMessage() {
        return message;
    }

    public String getvUri() {
        return vUri;
    }

    public Date getSessionExpirationDate() {
        return sessionExpirationDate;
    }
}
