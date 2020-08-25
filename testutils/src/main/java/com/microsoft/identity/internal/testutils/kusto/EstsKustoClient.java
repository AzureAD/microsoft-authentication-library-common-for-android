package com.microsoft.identity.internal.testutils.kusto;

import androidx.annotation.NonNull;

import com.microsoft.azure.kusto.ingest.IngestionMapping;
import com.microsoft.identity.internal.testutils.labutils.LabHelper;

/**
 * A Kusto Client to perform operations on ESTS Kusto Cluster.
 */
public class EstsKustoClient extends KustoClient {

    private final static String ESTS_DATABASE_NAME = "ESTS";
    private final static String ESTS_KUSTO_CLUSTER = "estswus2";
    private final static String ESTS_KUSTO_APP_TENANT_ID = "72f988bf-86f1-41af-91ab-2d7cd011db47";

    private final static String SECRET_NAME_IDLABS_KUSTO_CONNECTOR_APP_ID =
            "IDLABS-KUSTO-Connector-AppID";
    private final static String SECRET_NAME_IDLABS_KUSTO_CONNECTOR_APP_SECRET =
            "IDLABS-KUSTO-Connector-AppSecret";

    private final static String ESTS_KUSTO_CLIENT_TEST_TABLE_INGESTION_MAPPING_ANDROID =
            "AndroidMapping";

    public EstsKustoClient() {
        super(getEstsKustoClientConfig());
    }

    /**
     * Ingest the android test results into the Ests Kusto Client Test Table.
     *
     * @param testResultFileName the file containing android test results
     */
    public void ingestAndroidClientTestResults(@NonNull final String testResultFileName) {
        final IngestionMapping ingestionMapping = new IngestionMapping(
                ESTS_KUSTO_CLIENT_TEST_TABLE_INGESTION_MAPPING_ANDROID,
                IngestionMapping.IngestionMappingKind.Csv
        );

        ingest(
                EstsKustoClientTestTableData.ESTS_KUSTO_CLIENT_RESULT_TABLE_NAME,
                ingestionMapping,
                testResultFileName
        );
    }

    private static KustoClientConfiguration getEstsKustoClientConfig() {
        final String kustoAppId = getEstsKustoConnectorAppIdFromLab();
        final String kustoAppKey = getEstsKustoConnectorAppKeyFromLab();

        final KustoConnectorApp idlabsKustoConnector = new KustoConnectorApp(
                kustoAppId, kustoAppKey, ESTS_KUSTO_APP_TENANT_ID
        );

        return new KustoClientConfiguration(
                ESTS_KUSTO_CLUSTER, ESTS_DATABASE_NAME, idlabsKustoConnector
        );
    }

    private static String getEstsKustoConnectorAppIdFromLab() {
        return LabHelper.getSecret(SECRET_NAME_IDLABS_KUSTO_CONNECTOR_APP_ID);
    }

    private static String getEstsKustoConnectorAppKeyFromLab() {
        return LabHelper.getSecret(SECRET_NAME_IDLABS_KUSTO_CONNECTOR_APP_SECRET);
    }
}
