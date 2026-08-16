package com.example.processor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple CSV parser that reads content and produces a {@link CsvSummary}.
 * Uses basic split-on-comma logic suitable for a POC (no quoting support).
 */
public class CsvParser {

    private static final int MAX_PREVIEW_ROWS = 3;

    /**
     * Parse CSV content from an InputStream.
     *
     * @param inputStream the CSV content stream
     * @param fileName    the original file name (included in the summary)
     * @return a summary of the CSV content
     */
    public CsvSummary parse(InputStream inputStream, String fileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return doParse(reader, fileName);
        }
    }

    /**
     * Parse CSV content from a String.
     *
     * @param content  the CSV content as a string
     * @param fileName the original file name (included in the summary)
     * @return a summary of the CSV content
     */
    public CsvSummary parse(String content, String fileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            return doParse(reader, fileName);
        }
    }

    private CsvSummary doParse(BufferedReader reader, String fileName) throws IOException {
        String headerLine = reader.readLine();

        if (headerLine == null || headerLine.isBlank()) {
            return new CsvSummary(fileName, 0, List.of(), List.of());
        }

        List<String> columnNames = splitLine(headerLine);
        List<Map<String, String>> firstRows = new ArrayList<>();
        int rowCount = 0;

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            rowCount++;
            if (firstRows.size() < MAX_PREVIEW_ROWS) {
                List<String> values = splitLine(line);
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < columnNames.size(); i++) {
                    String value = i < values.size() ? values.get(i) : "";
                    row.put(columnNames.get(i), value);
                }
                firstRows.add(row);
            }
        }

        return new CsvSummary(fileName, rowCount, columnNames, firstRows);
    }

    private List<String> splitLine(String line) {
        String[] parts = line.split(",", -1);
        List<String> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            result.add(part.trim());
        }
        return result;
    }
}
