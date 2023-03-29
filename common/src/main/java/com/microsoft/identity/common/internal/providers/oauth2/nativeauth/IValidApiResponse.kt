package com.microsoft.identity.common.internal.providers.oauth2.nativeauth

interface IValidApiResponse {
    fun validateRequiredFields()

    fun validateOptionalFields()
}
