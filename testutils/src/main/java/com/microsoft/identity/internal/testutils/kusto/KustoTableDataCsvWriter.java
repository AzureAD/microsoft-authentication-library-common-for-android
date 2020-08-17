package com.microsoft.identity.internal.testutils.kusto;

import androidx.annotation.NonNull;

import com.opencsv.CSVWriter;

import java.io.Writer;
import java.util.ArrayList;

public class KustoTableDataCsvWriter extends CSVWriter {

    public KustoTableDataCsvWriter(Writer writer) {
        super(writer);
    }

    public void writeNext(@NonNull final IKustoTableData clientTestTableData) {
        writeNext(clientTestTableData.getTableDataAsCsv(), false);
    }

    public void writeAll(@NonNull final ArrayList<IKustoTableData> clientTestTableDataList) {
        for (final IKustoTableData data : clientTestTableDataList) {
            writeNext(data);
        }
    }
}
