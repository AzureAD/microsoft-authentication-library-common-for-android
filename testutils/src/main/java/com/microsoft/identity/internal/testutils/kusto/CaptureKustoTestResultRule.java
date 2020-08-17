package com.microsoft.identity.internal.testutils.kusto;

import android.util.Log;

import com.microsoft.identity.common.BuildConfig;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.sql.Timestamp;

public class CaptureKustoTestResultRule implements TestRule {

    private final String TAG = CaptureKustoTestResultRule.class.getSimpleName();

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Log.i(TAG, "Evaluating file rule");
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

                    FileUtils.writeTestResultsToCsv(
                            clientTestTableData
                    );
                }
            }
        };
    }
}
