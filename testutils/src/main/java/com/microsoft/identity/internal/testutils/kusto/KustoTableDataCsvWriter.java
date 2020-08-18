package com.microsoft.identity.internal.testutils.kusto;

import androidx.annotation.NonNull;

import com.opencsv.CSVWriter;

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
    public void writeNext(@NonNull final IKustoTableData clientTestTableData) {
        writeNext(clientTestTableData.getTableDataAsCsv(), false);
    }

    /**
     * Write the contents of the list of supplied table data to a csv file.
     *
     * @param clientTestTableDataList the table data that needs to be written
     */
    public void writeAll(@NonNull final ArrayList<IKustoTableData> clientTestTableDataList) {
        for (final IKustoTableData data : clientTestTableDataList) {
            writeNext(data);
        }
    }
}
