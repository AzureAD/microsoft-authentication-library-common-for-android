package com.microsoft.identity.common.internal.broker.ipc;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;

import javax.annotation.Nullable;

import lombok.NonNull;

public interface IOperation {

    String name();

    @NonNull
    AuthenticationConstants.BrokerContentProvider.API getContentApi();

    @Nullable
    String getAccountManagerOperation();
}
