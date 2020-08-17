package com.microsoft.identity.internal.testutils.kusto;

import android.os.Environment;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtils {

    public final static String TEST_RESULT_FILE_NAME = "test-results.csv";

    public static File getTestResultFile() {
        final File parent = Environment.getDataDirectory();
        return new File(parent, TEST_RESULT_FILE_NAME);
    }

    public static String getAbsoluteTestResultFilePath() {
        return getTestResultFile().getAbsolutePath();
    }

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
