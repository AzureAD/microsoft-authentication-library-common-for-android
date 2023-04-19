package com.microsoft.identity.common.java.providers.nativeauth

import com.microsoft.identity.common.java.providers.oauth2.IErrorResponse

interface IApiErrorResponse :
    IErrorResponse,
    IValidApiResponse
