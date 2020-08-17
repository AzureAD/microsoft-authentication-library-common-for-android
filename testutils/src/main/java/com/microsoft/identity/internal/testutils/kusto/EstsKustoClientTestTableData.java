package com.microsoft.identity.internal.testutils.kusto;

import java.sql.Timestamp;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EstsKustoClientTestTableData implements IKustoTableData {

    private Timestamp timestamp;
    private String runnerInstance;
    private String runnerVersion;
    private String scaleUnit;
    private String result;
    private String testName;
    private String errorMessage;

    public final String[] getTableDataAsCsv() {
        return new String[]{
                timestamp.toString(),
                runnerInstance,
                scaleUnit,
                result,
                testName,
                errorMessage,
                runnerVersion
        };
    }
}
