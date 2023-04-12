package com.microsoft.identity.common.internal.providers.oauth2.nativeauth

import com.microsoft.identity.common.java.providers.oauth2.IErrorResponse

interface IApiErrorResponse :
    IErrorResponse,
    IValidApiResponse
