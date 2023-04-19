package com.microsoft.identity.common.java.providers.nativeauth

interface IValidApiResponse {
    fun validateRequiredFields()

    fun validateOptionalFields()
}
