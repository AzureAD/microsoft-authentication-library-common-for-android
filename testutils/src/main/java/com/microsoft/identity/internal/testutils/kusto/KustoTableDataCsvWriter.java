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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

/**
 * A CSV writer for the {@link IKustoTableData} model.
 */
public class KustoTableDataCsvWriter extends CSVWriter {

    public KustoTableDataCsvWriter(Writer writer) {
        super(writer);
    }

    /**
     * Write the contents of the table data to a csv file.
     *
     * @param clientTestTableData the table data that needs to be written
     */
    public void writeNext(@NonNull final IKustoTableData clientTestTableData) throws IOException {
        writeNext(clientTestTableData.getTableDataAsCsv());
    }

    /**
     * Write the contents of the list of supplied table data to a csv file.
     *
     * @param clientTestTableDataList the table data that needs to be written
     */
    public void writeAll(@NonNull final ArrayList<IKustoTableData> clientTestTableDataList)
            throws IOException {
        for (final IKustoTableData data : clientTestTableDataList) {
            writeNext(data);
        }
    }
}
