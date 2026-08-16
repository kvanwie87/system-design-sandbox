package com.example.processor;

import java.util.List;
import java.util.Map;

/**
 * Summary of a parsed CSV file, intended for publishing to SNS.
 */
public class CsvSummary {

    private String fileName;
    private int rowCount;
    private List<String> columnNames;
    private List<Map<String, String>> firstRows;

    public CsvSummary() {
    }

    public CsvSummary(String fileName, int rowCount, List<String> columnNames, List<Map<String, String>> firstRows) {
        this.fileName = fileName;
        this.rowCount = rowCount;
        this.columnNames = columnNames;
        this.firstRows = firstRows;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public List<Map<String, String>> getFirstRows() {
        return firstRows;
    }

    public void setFirstRows(List<Map<String, String>> firstRows) {
        this.firstRows = firstRows;
    }
}
