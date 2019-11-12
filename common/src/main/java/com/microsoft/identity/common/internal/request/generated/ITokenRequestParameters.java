package com.microsoft.identity.common.internal.request.generated;

import com.microsoft.identity.common.internal.authorities.Authority;

import java.util.Set;

public interface ITokenRequestParameters {
    String clientId();
    Set<String> scopes();
    String redirectUri();
    Authority authority();
    String claimsRequestJson();
}
