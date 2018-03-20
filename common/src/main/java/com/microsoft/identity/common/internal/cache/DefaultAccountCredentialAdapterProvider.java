package com.microsoft.identity.common.internal.cache;

public class DefaultAccountCredentialAdapterProvider implements IAccountCredentialAdapterProvider {

    @Override
    public IAccountAdapter getAccountAdapter() {
        return new DefaultAccountAdapter();
    }

    @Override
    public ICredentialAdapter getCredentialAdapter() {
        return new DefaultCredentialAdapter();
    }
}
