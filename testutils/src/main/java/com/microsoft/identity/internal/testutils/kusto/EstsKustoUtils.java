package com.microsoft.identity.internal.testutils.kusto;

import android.util.Log;

import androidx.annotation.NonNull;

import com.microsoft.azure.kusto.data.ClientImpl;
import com.microsoft.azure.kusto.data.ConnectionStringBuilder;
import com.microsoft.azure.kusto.data.KustoOperationResult;
import com.microsoft.azure.kusto.data.KustoResultSetTable;
import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import com.microsoft.azure.kusto.ingest.IngestClient;
import com.microsoft.azure.kusto.ingest.IngestClientFactory;
import com.microsoft.azure.kusto.ingest.IngestionMapping;
import com.microsoft.azure.kusto.ingest.IngestionProperties;
import com.microsoft.azure.kusto.ingest.exceptions.IngestionClientException;
import com.microsoft.azure.kusto.ingest.exceptions.IngestionServiceException;
import com.microsoft.azure.kusto.ingest.result.IngestionResult;
import com.microsoft.azure.kusto.ingest.result.IngestionStatus;
import com.microsoft.azure.kusto.ingest.result.OperationStatus;
import com.microsoft.azure.kusto.ingest.source.FileSourceInfo;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.identity.internal.testutils.labutils.LabHelper;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Utilities to interact with the Kusto Data SDK to perform operations on the ESTS Kusto Cluster.
 */
public class EstsKustoUtils {

    private static final String TAG = EstsKustoUtils.class.getSimpleName();

    private final static String ESTS_DATABASE_NAME = "ESTS";
    private final static String ESTS_KUSTO_CLUSTER = "estswus2";
    private final static String ESTS_KUSTO_APP_TENANT_ID = "72f988bf-86f1-41af-91ab-2d7cd011db47";

    private final static String SECRET_NAME_IDLABS_KUSTO_CONNECTOR_APP_ID =
            "IDLABS-KUSTO-Connector-AppID";
    private final static String SECRET_NAME_IDLABS_KUSTO_CONNECTOR_APP_SECRET =
            "IDLABS-KUSTO-Connector-AppSecret";

    private final static String ESTS_KUSTO_CLIENT_TEST_TABLE_INGESTION_MAPPING_ANDROID =
            "AndroidMapping";

    private static ConnectionStringBuilder createKustoConnectionString(@NonNull final KustoOperation kustoOperation) {
        final String kustoAppId = getEstsKustoConnectorAppIdFromLab();
        final String kustoAppKey = getEstsKustoConnectorAppKeyFromLab();

        final String resourceUri = kustoOperation == KustoOperation.Ingest
                ? "https://ingest-" + ESTS_KUSTO_CLUSTER + ".kusto.windows.net"
                : "https://" + ESTS_KUSTO_CLUSTER + ".kusto.windows.net";

        return ConnectionStringBuilder.createWithAadApplicationCredentials(
                resourceUri,
                kustoAppId,
                kustoAppKey,
                ESTS_KUSTO_APP_TENANT_ID);
    }

    /**
     * Perform the supplied query on ESTS Kusto Cluster.
     *
     * @param query the query that needs to be performed
     * @return
     * @throws URISyntaxException
     * @throws DataClientException
     * @throws DataServiceException
     */
    public static KustoResultSetTable query(@NonNull final String query) throws URISyntaxException,
            DataClientException, DataServiceException {
        final ConnectionStringBuilder connectionStringBuilder = createKustoConnectionString(
                KustoOperation.Query
        );

        final ClientImpl client = new ClientImpl(connectionStringBuilder);

        final KustoOperationResult result = client.execute(ESTS_DATABASE_NAME, query);
        return result.getPrimaryResults();
    }

    /**
     * Ingest the android test results into the Ests Kusto Client Test Table.
     *
     * @param testResultFileName the file containing android test results
     */
    public static void ingestAndroidClientTestResults(@NonNull final String testResultFileName) {
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

    /**
     * Ingest data contained within the supplied file into the specified table in the ESTS Kusto
     * Cluster.
     *
     * @param tableName        the table into which to ingest
     * @param ingestionMapping the mapping to use while ingestion
     * @param fileToIngest     the file that contains the data to ingest
     */
    public static void ingest(@NonNull final String tableName,
                              @NonNull final IngestionMapping ingestionMapping,
                              @NonNull final String fileToIngest) {
        final ConnectionStringBuilder connectionStringBuilder = createKustoConnectionString(
                KustoOperation.Ingest
        );

        IngestClient client;

        // Creating the client:
        try {
            client = IngestClientFactory.createClient(connectionStringBuilder);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Creating the ingestion properties:
        IngestionProperties ingestionProperties = new IngestionProperties(
                ESTS_DATABASE_NAME,
                tableName
        );

        ingestionProperties.setReportMethod(IngestionProperties.IngestionReportMethod.Table);
        ingestionProperties.setDataFormat(IngestionProperties.DATA_FORMAT.csv);
        ingestionProperties.setReportLevel(IngestionProperties.IngestionReportLevel.FailuresAndSuccesses);
        ingestionProperties.setFlushImmediately(true);

        ingestionProperties.setIngestionMapping(ingestionMapping);

        try {
            FileSourceInfo fileSourceInfo = new FileSourceInfo(fileToIngest, 0);
            IngestionResult ingestionResult = client.ingestFromFile(fileSourceInfo, ingestionProperties);

            List<IngestionStatus> statuses = ingestionResult.getIngestionStatusCollection();

            int timeoutInSec = 60;

            while (statuses.get(0).status == OperationStatus.Pending && timeoutInSec > 0) {
                Thread.sleep(1000);
                timeoutInSec -= 1;
                statuses = ingestionResult.getIngestionStatusCollection();
                Log.i(TAG, "Ingestion status: " + statuses.get(0).status.toString());
            }

            for (IngestionStatus status : statuses) {
                Log.i(TAG, "Final ingestion status: " + status.status.toString());
            }

        } catch (IngestionClientException | IngestionServiceException | InterruptedException |
                StorageException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getEstsKustoConnectorAppIdFromLab() {
        return LabHelper.getSecret(SECRET_NAME_IDLABS_KUSTO_CONNECTOR_APP_ID);
    }

    private static String getEstsKustoConnectorAppKeyFromLab() {
        return LabHelper.getSecret(SECRET_NAME_IDLABS_KUSTO_CONNECTOR_APP_SECRET);
    }
}
