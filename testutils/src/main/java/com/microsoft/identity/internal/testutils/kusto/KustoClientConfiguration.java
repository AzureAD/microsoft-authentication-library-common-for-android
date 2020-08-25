package com.microsoft.identity.internal.testutils.kusto;

import androidx.annotation.NonNull;

/**
 * A configuration to setup a {@link KustoClient}.
 */
public class KustoClientConfiguration {

    private String cluster;
    private String database;
    private KustoConnectorApp connectorApp;

    public KustoClientConfiguration(@NonNull final String cluster,
                                    @NonNull final String database,
                                    @NonNull final KustoConnectorApp connectorApp) {
        this.cluster = cluster;
        this.database = database;
        this.connectorApp = connectorApp;
    }

    public String getCluster() {
        return cluster;
    }

    public String getDatabase() {
        return database;
    }

    public KustoConnectorApp getConnectorApp() {
        return connectorApp;
    }
}
