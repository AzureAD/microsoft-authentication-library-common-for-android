// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.internal.testutils.kusto;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Utilities to interact with test result file during android tests.
 */
public class TestResultFileUtils {

    public static final String TEST_RESULT_FILE_NAME = "test-results.csv";

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
            final FileWriter outputFile = new FileWriter(testResultFile, true);

            // create CSVWriter object with FileWriter object as parameter
            final KustoTableDataCsvWriter writer = new KustoTableDataCsvWriter(outputFile);

            writer.writeNext(data);

            // closing writer connection
            writer.close();
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
    }
}
