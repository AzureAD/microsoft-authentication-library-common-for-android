package com.microsoft.identity.common.internal.logging;

import java.util.Map;

public interface IRequestContext extends Map<String, String> {

    String toJsonString();

}
