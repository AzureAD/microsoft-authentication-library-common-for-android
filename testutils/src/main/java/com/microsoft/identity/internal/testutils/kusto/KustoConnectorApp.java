package com.microsoft.identity.internal.testutils.kusto;

import androidx.annotation.NonNull;

/**
 * A model representing an app to connect to Kusto as a confidential client.
 */
public class KustoConnectorApp {

    private String appId;
    private String appKey;
    private String appTenantId;

    public KustoConnectorApp(@NonNull final String appId,
                             @NonNull final String appKey,
                             @NonNull final String appTenantId) {
        this.appId = appId;
        this.appKey = appKey;
        this.appTenantId = appTenantId;
    }

    String getAppId() {
        return appId;
    }

    String getAppKey() {
        return appKey;
    }

    String getAppTenantId() {
        return appTenantId;
    }
}
