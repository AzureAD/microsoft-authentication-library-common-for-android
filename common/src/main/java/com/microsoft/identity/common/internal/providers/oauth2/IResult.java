package com.microsoft.identity.common.internal.providers.oauth2;

public interface IResult {

    boolean getSuccess();
    IErrorResponse getErrorResponse();
}
