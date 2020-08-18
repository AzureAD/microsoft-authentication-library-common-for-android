package com.microsoft.identity.internal.testutils.kusto;

/**
 * A model for a data point in a Kusto Table.
 */
public interface IKustoTableData {

    String[] getTableDataAsCsv();

}
