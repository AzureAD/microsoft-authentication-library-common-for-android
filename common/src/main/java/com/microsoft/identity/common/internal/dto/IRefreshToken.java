package com.microsoft.identity.common.internal.dto;

public interface IRefreshToken {

    String getUniqueUserId();

    String getEnvironment();

    String getClientId();

    String getSecret();

    String getTarget();

    String getExpiresOn();

    String getFamilyId();

}
