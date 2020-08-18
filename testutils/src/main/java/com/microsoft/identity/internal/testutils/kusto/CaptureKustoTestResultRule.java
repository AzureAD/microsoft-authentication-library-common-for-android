package com.microsoft.identity.internal.testutils.kusto;

import android.util.Log;

import com.microsoft.identity.common.BuildConfig;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.sql.Timestamp;

/**
 * A JUnit rule to capture test results and save to a CSV File.
 */
public class CaptureKustoTestResultRule implements TestRule {

    private final String TAG = CaptureKustoTestResultRule.class.getSimpleName();

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Log.i(TAG, "Starting to write test results to file.");
                final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                final String runnerInstance = "MSAL RobolectricTest";
                final String runnerVersion = "";
                final String scaleUnit = BuildConfig.DC;
                final String testName = description.getDisplayName();
                String result = "", errorMessage = "";
                try {
                    base.evaluate();
                    result = "PASS";
                } catch (final Throwable throwable) {
                    result = "FAIL";
                    errorMessage = throwable.getMessage();
                    throw throwable;
                } finally {
                    final EstsKustoClientTestTableData clientTestTableData =
                            EstsKustoClientTestTableData.builder()
                                    .testName(testName)
                                    .timestamp(timestamp)
                                    .runnerInstance(runnerInstance)
                                    .runnerVersion(runnerVersion)
                                    .result(result)
                                    .scaleUnit(scaleUnit)
                                    .errorMessage(errorMessage)
                                    .build();

                    TestResultFileUtils.writeTestResultsToCsv(
                            clientTestTableData
                    );
                }
            }
        };
    }
}
