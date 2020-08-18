package com.microsoft.identity.internal.testutils.kusto;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Utilities to interact with test result file during android tests.
 */
public class TestResultFileUtils {

    public final static String TEST_RESULT_FILE_NAME = "test-results.csv";

    /**
     * Get the file containing the android test results.
     *
     * @return a File object containing the android test results
     */
    public static File getTestResultFile() {
        final File parent = new File(System.getProperty("user.home"));
        return new File(parent, TEST_RESULT_FILE_NAME);
    }

    /**
     * Write android client test results to a csv file.
     *
     * @param data the test data that needs to be written to csv
     */
    public static void writeTestResultsToCsv(@NonNull final EstsKustoClientTestTableData data) {
        final File testResultFile = getTestResultFile();
        try {
            // create FileWriter object with file as parameter
            FileWriter outputFile = new FileWriter(testResultFile, true);

            // create CSVWriter object filewriter object as parameter
            KustoTableDataCsvWriter writer = new KustoTableDataCsvWriter(outputFile);

            writer.writeNext(data);

            // closing writer connection
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
