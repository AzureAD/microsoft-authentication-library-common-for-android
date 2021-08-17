package com.microsoft.identity.common.java.util;

import lombok.NonNull;

public class OAuthUtil {
    private static final String RESOURCE_DEFAULT_SCOPE = "/.default";

    /**
     * Given a v1 resource uri, append '/.default' to convert it to a v2 scope.
     *
     * @param resource The v1 resource uri.
     * @return The v1 resource uri as a scope.
     */
    @NonNull
    public static String getScopeFromResource(@NonNull final String resource) {
        return resource + RESOURCE_DEFAULT_SCOPE;
    }
}
