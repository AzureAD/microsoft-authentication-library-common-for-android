package com.microsoft.identity.common.java.broker;


import com.microsoft.identity.common.java.exception.ClientException;

import java.util.Map;

import lombok.NonNull;

public interface ICallValidator {
    void throwIfNotInvokedByAcceptableApp(@NonNull String methodName,
                                          int callingUid,
                                          @NonNull Map<String, Iterable<String>> allowedApplications)
            throws ClientException;
}
