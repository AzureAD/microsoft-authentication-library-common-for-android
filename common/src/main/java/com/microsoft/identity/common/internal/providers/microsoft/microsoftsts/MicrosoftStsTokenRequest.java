package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenRequest;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@Accessors(prefix = "m")
public class MicrosoftStsTokenRequest extends MicrosoftTokenRequest {
    public MicrosoftStsTokenRequest() {
        super();
    }
}
