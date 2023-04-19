package com.microsoft.identity.common.java.providers.nativeauth.requests

import java.net.URL

abstract class NativeAuthRequest {
    abstract var requestUrl: URL
    abstract var headers: Map<String, String?>
    abstract val parameters: NativeAuthRequestParameters

    abstract class NativeAuthRequestParameters {
        abstract val clientId: String
    }
}
