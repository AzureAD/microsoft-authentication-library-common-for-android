package com.microsoft.identity.common.internal.request.generated;

import java.util.List;

public interface IScopesAddable<T extends CommandParameters> {
    public T addDefaultScopes(List<String> scopes);
}
