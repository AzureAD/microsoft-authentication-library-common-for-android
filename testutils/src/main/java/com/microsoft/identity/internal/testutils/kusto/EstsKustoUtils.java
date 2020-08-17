package com.microsoft.identity.internal.testutils.kusto;

import android.util.Log;

import androidx.annotation.NonNull;

import com.microsoft.azure.kusto.data.ClientImpl;
import com.microsoft.azure.kusto.data.ConnectionStringBuilder;
import com.microsoft.azure.kusto.data.KustoOperationResult;
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

import org.junit.Assert;

import java.net.URISyntaxException;
import java.util.List;

public class EstsKustoUtils {

    private static final String TAG = EstsKustoUtils.class.getSimpleName();

    private final static String ESTS_DATABASE_NAME = "ESTS";
    private final static String ESTS_KUSTO_CLUSTER = "estswus2";
    private final static String ESTS_KUSTO_APP_TENANT_ID = "72f988bf-86f1-41af-91ab-2d7cd011db47";

    private final static String ESTS_KUSTO_CLIENT_RESULT_TABLE_NAME = "ClientTestTable";

    private final static String SECRET_NAME_IDLABS_KUSTO_CONNECTOR_APP_ID =
            "IDLABS-KUSTO-Connector-AppID";
    private final static String SECRET_NAME_IDLABS_KUSTO_CONNECTOR_APP_SECRET =
            "IDLABS-KUSTO-Connector-AppSecret";

    private final static String ESTS_KUSTO_CLIENT_TEST_TABLE_INGESTION_MAPPING_ANDROID =
            "AndroidMapping";

    private static ConnectionStringBuilder createKustoConnectionString() {
        final String kustoAppId = getEstsKustoConnectorAppIdFromLab();
        final String kustoAppKey = getEstsKustoConnectorAppKeyFromLab();

        final ConnectionStringBuilder connectionStringBuilder =
                ConnectionStringBuilder.createWithAadApplicationCredentials(
                        "https://ingest-" + ESTS_KUSTO_CLUSTER + ".kusto.windows.net",
                        kustoAppId,
                        kustoAppKey,
                        ESTS_KUSTO_APP_TENANT_ID);

        return connectionStringBuilder;
    }

    public static void queryEstsKusto(@NonNull final String query) throws URISyntaxException {
        final ConnectionStringBuilder connectionStringBuilder = createKustoConnectionString();

        final ClientImpl client = new ClientImpl(connectionStringBuilder);

        try {
            final KustoOperationResult result = client.execute(ESTS_DATABASE_NAME, query);
            result.getPrimaryResults().next();
            final String firstItem = result.getPrimaryResults().getString("Message");
            Log.w(TAG, firstItem);
        } catch (DataServiceException | DataClientException e) {
            Assert.fail(e.getMessage());
        }
    }

    public static void ingestKusto(@NonNull final String filePath) {
        final ConnectionStringBuilder connectionStringBuilder = createKustoConnectionString();

        IngestClient client = null;

        // Creating the client:
        try {
            client = IngestClientFactory.createClient(connectionStringBuilder);
        } catch (URISyntaxException e) {
            Assert.fail(e.getMessage());
        }

        // Creating the ingestion properties:
        IngestionProperties ingestionProperties = new IngestionProperties(
                ESTS_DATABASE_NAME,
                ESTS_KUSTO_CLIENT_RESULT_TABLE_NAME
        );

        ingestionProperties.setReportMethod(IngestionProperties.IngestionReportMethod.Table);
        ingestionProperties.setDataFormat(IngestionProperties.DATA_FORMAT.csv);
        ingestionProperties.setReportLevel(IngestionProperties.IngestionReportLevel.FailuresAndSuccesses);
        ingestionProperties.setFlushImmediately(true);

        ingestionProperties.setIngestionMapping(
                ESTS_KUSTO_CLIENT_TEST_TABLE_INGESTION_MAPPING_ANDROID,
                IngestionMapping.IngestionMappingKind.Csv
        );

        try {
            FileSourceInfo fileSourceInfo = new FileSourceInfo(filePath, 0);
            IngestionResult ingestionResult = client.ingestFromFile(fileSourceInfo, ingestionProperties);
            Log.i(TAG, ingestionResult.toString());

            List<IngestionStatus> statuses = ingestionResult.getIngestionStatusCollection();

            int timeoutInSec = 120;

            while (statuses.get(0).status == OperationStatus.Pending && timeoutInSec > 0) {
                Thread.sleep(1000);
                timeoutInSec -= 1;
                statuses = ingestionResult.getIngestionStatusCollection();
                Log.i(TAG, "status: " + statuses.get(0).status.toString());
                Log.i(TAG, "failure status: " + statuses.get(0).failureStatus.getValue());
                Log.i(TAG, "error code: " + statuses.get(0).errorCode.name());
            }

            for (IngestionStatus status : statuses) {
                Log.i(TAG, "Final status: " + status.status.toString());
            }

        } catch (IngestionClientException e) {
            e.printStackTrace();
        } catch (IngestionServiceException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (StorageException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    private static String getEstsKustoConnectorAppIdFromLab() {
        return LabHelper.getSecret(SECRET_NAME_IDLABS_KUSTO_CONNECTOR_APP_ID);
    }

    private static String getEstsKustoConnectorAppKeyFromLab() {
        return LabHelper.getSecret(SECRET_NAME_IDLABS_KUSTO_CONNECTOR_APP_SECRET);
    }
}
