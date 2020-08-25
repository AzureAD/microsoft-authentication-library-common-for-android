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

import java.net.URISyntaxException;
import java.util.List;

/**
 * A client to interact with the Kusto SDK.
 */
public class KustoClient {

    private static final String TAG = KustoClient.class.getSimpleName();

    private KustoClientConfiguration kustoClientConfiguration;

    public KustoClient(@NonNull final KustoClientConfiguration kustoClientConfiguration) {
        this.kustoClientConfiguration = kustoClientConfiguration;
    }

    private ConnectionStringBuilder createKustoConnectionString(@NonNull final KustoOperation kustoOperation) {
        final String kustoAppId = kustoClientConfiguration.getConnectorApp().getAppId();
        final String kustoAppKey = kustoClientConfiguration.getConnectorApp().getAppKey();

        final String resourceUri = kustoOperation == KustoOperation.Ingest
                ? "https://ingest-" + kustoClientConfiguration.getCluster() + ".kusto.windows.net"
                : "https://" + kustoClientConfiguration.getCluster() + ".kusto.windows.net";

        return ConnectionStringBuilder.createWithAadApplicationCredentials(
                resourceUri,
                kustoAppId,
                kustoAppKey,
                kustoClientConfiguration.getConnectorApp().getAppTenantId());
    }

    /**
     * Perform the supplied query on ESTS Kusto Cluster.
     *
     * @param query the query that needs to be performed
     * @return a {@link KustoResultSetTable} containing the results of the query
     * @throws URISyntaxException   if unable to create a Kusto client
     * @throws DataClientException  if the query fails
     * @throws DataServiceException if the query fails
     */
    public KustoResultSetTable query(@NonNull final String query) throws URISyntaxException,
            DataClientException, DataServiceException {
        final ConnectionStringBuilder connectionStringBuilder = createKustoConnectionString(
                KustoOperation.Query
        );

        final ClientImpl client = new ClientImpl(connectionStringBuilder);

        final KustoOperationResult result = client.execute(
                kustoClientConfiguration.getDatabase(), query
        );
        return result.getPrimaryResults();
    }

    /**
     * Ingest data contained within the supplied file into the specified table in the ESTS Kusto
     * Cluster.
     *
     * @param tableName        the table into which to ingest
     * @param ingestionMapping the mapping to use while ingestion
     * @param fileToIngest     the file that contains the data to ingest
     */
    public void ingest(@NonNull final String tableName,
                       @NonNull final IngestionMapping ingestionMapping,
                       @NonNull final String fileToIngest) {
        final ConnectionStringBuilder connectionStringBuilder = createKustoConnectionString(
                KustoOperation.Ingest
        );

        final IngestClient client;

        // Creating the client:
        try {
            client = IngestClientFactory.createClient(connectionStringBuilder);
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Creating the ingestion properties:
        final IngestionProperties ingestionProperties = new IngestionProperties(
                kustoClientConfiguration.getDatabase(),
                tableName
        );

        ingestionProperties.setReportMethod(IngestionProperties.IngestionReportMethod.Table);
        ingestionProperties.setDataFormat(IngestionProperties.DATA_FORMAT.csv);
        ingestionProperties.setReportLevel(IngestionProperties.IngestionReportLevel.FailuresAndSuccesses);
        ingestionProperties.setFlushImmediately(true);

        ingestionProperties.setIngestionMapping(ingestionMapping);

        try {
            final FileSourceInfo fileSourceInfo = new FileSourceInfo(fileToIngest, 0);
            final IngestionResult ingestionResult = client.ingestFromFile(fileSourceInfo, ingestionProperties);

            List<IngestionStatus> statuses = ingestionResult.getIngestionStatusCollection();

            int timeoutInSec = 60;

            while (statuses.get(0).status == OperationStatus.Pending && timeoutInSec > 0) {
                Thread.sleep(1000);
                timeoutInSec -= 1;
                statuses = ingestionResult.getIngestionStatusCollection();
                Log.i(TAG, "Ingestion status: " + statuses.get(0).status.toString());
            }

            for (final IngestionStatus status : statuses) {
                if (status.status != OperationStatus.Succeeded) {
                    throw new RuntimeException(
                            "Kusto Ingestion failed with status: " + status.status.toString()
                    );
                } else {
                    Log.i(TAG, "Kusto Ingestion Succeeded!");
                }
            }

        } catch (final IngestionClientException | IngestionServiceException | InterruptedException |
                StorageException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
