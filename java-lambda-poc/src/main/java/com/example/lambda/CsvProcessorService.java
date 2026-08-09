package com.example.lambda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service that processes CSV data by filtering rows, enriching with computed columns,
 * and converting the result to JSON.
 *
 * Processing steps:
 * 1. Filter: Keep only rows where status == "active" (case-insensitive)
 * 2. Enrich: Add a "total" column computed as quantity * price
 * 3. Convert: Serialize the result as a JSON array
 */
public class CsvProcessorService {

    private static final String STATUS_COLUMN = "status";
    private static final String QUANTITY_COLUMN = "quantity";
    private static final String PRICE_COLUMN = "price";
    private static final String ACTIVE_STATUS = "active";

    private final ObjectMapper objectMapper;

    public CsvProcessorService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Processes a CSV input stream and returns a JSON string.
     *
     * @param csvInput the CSV data as an InputStream
     * @return JSON array string of filtered and enriched records
     */
    public String processCSV(InputStream csvInput) {
        List<Map<String, String>> results = new ArrayList<>();

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(csvInput, StandardCharsets.UTF_8)).build()) {

            String[] headers = reader.readNext();
            if (headers == null || headers.length == 0) {
                return "[]";
            }

            // Trim headers to handle whitespace
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim();
            }

            // Find column indices
            int statusIndex = findColumnIndex(headers, STATUS_COLUMN);
            int quantityIndex = findColumnIndex(headers, QUANTITY_COLUMN);
            int priceIndex = findColumnIndex(headers, PRICE_COLUMN);

            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length < headers.length) {
                    continue; // Skip malformed rows
                }

                // Filter: only keep active rows
                if (statusIndex >= 0 && !ACTIVE_STATUS.equalsIgnoreCase(line[statusIndex].trim())) {
                    continue;
                }

                // Build the record map
                Map<String, String> record = new LinkedHashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    record.put(headers[i], line[i].trim());
                }

                // Enrich: add total = quantity * price
                Double total = computeTotal(line, quantityIndex, priceIndex);
                if (total != null) {
                    record.put("total", String.format("%.2f", total));
                }

                results.add(record);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error processing CSV: " + e.getMessage(), e);
        }

        try {
            return objectMapper.writeValueAsString(results);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing to JSON: " + e.getMessage(), e);
        }
    }

    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }

    private Double computeTotal(String[] line, int quantityIndex, int priceIndex) {
        if (quantityIndex < 0 || priceIndex < 0) {
            return null;
        }
        try {
            double quantity = Double.parseDouble(line[quantityIndex].trim());
            double price = Double.parseDouble(line[priceIndex].trim());
            return quantity * price;
        } catch (NumberFormatException e) {
            // Skip enrichment for rows with non-numeric values
            return null;
        }
    }
}
