package com.microsoft.identity.client.ui.automation.rules;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.test.core.app.ApplicationProvider;

import com.google.gson.JsonElement;
import com.microsoft.identity.client.ui.automation.annotations.NetworkTest;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.network.NetworkTestStateManager;
import com.microsoft.identity.client.ui.automation.reporting.Timeline;
import com.microsoft.identity.client.ui.automation.sdk.ResultFuture;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NetworkTestRule<T> implements TestRule {

    private static final String TAG = NetworkTestRule.class.getSimpleName();
    private static final long FIND_UI_ELEMENT_TIMEOUT = CommonUtils.FIND_UI_ELEMENT_TIMEOUT;
    private final static String LOG_FOLDER_NAME = "automation_timeline";
    private static final Timeline timeline = Timeline.getInstance();

    private ResultFuture<T, Exception> testResult = new ResultFuture<>();
    private NetworkTestStateManager currentStateManager = null;
    private final List<JsonElement> timelineRecords = new ArrayList<>();

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void evaluate() throws Throwable {
                Logger.i(TAG, "Applying rule...");
                NetworkTest networkTestAnnotation = description.getAnnotation(NetworkTest.class);

                if (networkTestAnnotation == null) {
                    Logger.i(TAG, "Method[" + description.getMethodName() + "] does not have @NetworkTest annotation...");

                    networkTestAnnotation = description.getClass().getAnnotation(NetworkTest.class);
                }

                if (networkTestAnnotation != null) {
                    runNetworkTest(networkTestAnnotation, description, base);
                } else {
                    Logger.e(TAG, "No tests to be run. Network states input file is not defined.");
                }
            }
        };
    }

    private void runNetworkTest(final NetworkTest networkTestAnnotation, final Description description, final Statement base) throws Throwable {
        final boolean recordTimeline = networkTestAnnotation.recordTimeline();
        final String outputFile = description.getMethodName();

        final File timelineOutputFile = createFile(outputFile + ".json");
        final File timelineVisualFile = createFile(outputFile + ".html");

        // Update the timeout for waiting of a UI element
        CommonUtils.FIND_UI_ELEMENT_TIMEOUT =
                networkTestAnnotation.testTimeout() <= 0 ? FIND_UI_ELEMENT_TIMEOUT : TimeUnit.SECONDS.toMillis(networkTestAnnotation.testTimeout());

        final List<NetworkTestStateManager> stateManagers = NetworkTestStateManager
                .readCSVFile(description.getTestClass(), networkTestAnnotation.inputFile());

        for (NetworkTestStateManager stateManager : stateManagers) {
            currentStateManager = stateManager;

            if (stateManager.isIgnored()) {
                Logger.i(TAG, "Skipping network test [" + stateManager.getId() + "] since it was marked as ignored.");
            } else {
                if (recordTimeline) {
                    timeline.startRecording();
                }
                base.evaluate();

                cleanUp();
                if (recordTimeline) {
                    timeline.stopRecording();

                    timelineRecords.add(timeline.toJson("Network test run [" + stateManager.getId() + "]"));
                }
            }
        }

        // Create the output files.
        CommonUtils.writeToFile(timelineOutputFile, timelineRecords.toString(), false);
        Timeline.createHTMLVisuals(timelineVisualFile, description.getTestClass().getSimpleName(), timelineRecords);

        // Copy the files to SD card
        CommonUtils.copyFileToFolderInSdCard(timelineOutputFile, LOG_FOLDER_NAME);
        CommonUtils.copyFileToFolderInSdCard(timelineVisualFile, LOG_FOLDER_NAME);
    }

    private void cleanUp() {
        testResult = new ResultFuture<>();
    }

    public void setResult(T result) {
        testResult.setResult(result);
    }

    public void setException(Exception exception) {
        testResult.setException(exception);
    }

    public NetworkTestStateManager getCurrentStateManager() {
        return currentStateManager;
    }

    public T getResult(long timeoutSeconds, TimeUnit timeUnit) throws Throwable {
        return testResult.get(timeoutSeconds, timeUnit);
    }

    public T getResult() throws Exception {
        return testResult.get();
    }

    private File createFile(@NonNull final String filename) throws IOException {
        final Context context = ApplicationProvider.getApplicationContext();
        final File directory = context.getFilesDir();
        final File logFile = new File(directory, filename);

        if (!logFile.exists()) {
            final boolean fileCreated = logFile.createNewFile();
            if (!fileCreated) {
                throw new IOException("Unable to create new log file :(");
            }
        }

        return logFile;
    }
}
