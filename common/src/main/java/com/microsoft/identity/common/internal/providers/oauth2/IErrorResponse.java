package com.microsoft.identity.common.internal.providers.oauth2;

public interface IErrorResponse {

    String getError();
    String getErrorDescription();
}
