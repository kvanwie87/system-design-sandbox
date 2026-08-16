package com.example.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CsvParserTest {

    private CsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new CsvParser();
    }

    @Test
    void parsesStandardCsv() throws IOException {
        String csv = """
                name,email,age
                Alice,alice@example.com,30
                Bob,bob@example.com,25
                Carol,carol@example.com,28
                Dave,dave@example.com,35
                Eve,eve@example.com,22
                """;

        CsvSummary summary = parser.parse(csv, "people.csv");

        assertEquals("people.csv", summary.getFileName());
        assertEquals(5, summary.getRowCount());
        assertEquals(List.of("name", "email", "age"), summary.getColumnNames());
        // Only first 3 rows are previewed
        assertEquals(3, summary.getFirstRows().size());
        assertEquals("Alice", summary.getFirstRows().get(0).get("name"));
        assertEquals("bob@example.com", summary.getFirstRows().get(1).get("email"));
        assertEquals("28", summary.getFirstRows().get(2).get("age"));
    }

    @Test
    void parsesFromInputStream() throws IOException {
        String csv = "col1,col2\nval1,val2\n";
        ByteArrayInputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        CsvSummary summary = parser.parse(stream, "input.csv");

        assertEquals("input.csv", summary.getFileName());
        assertEquals(1, summary.getRowCount());
        assertEquals(List.of("col1", "col2"), summary.getColumnNames());
        assertEquals(1, summary.getFirstRows().size());
        assertEquals(Map.of("col1", "val1", "col2", "val2"), summary.getFirstRows().get(0));
    }

    @Test
    void handlesEmptyFile() throws IOException {
        CsvSummary summary = parser.parse("", "empty.csv");

        assertEquals("empty.csv", summary.getFileName());
        assertEquals(0, summary.getRowCount());
        assertTrue(summary.getColumnNames().isEmpty());
        assertTrue(summary.getFirstRows().isEmpty());
    }

    @Test
    void handlesHeaderOnly() throws IOException {
        String csv = "name,email,age\n";

        CsvSummary summary = parser.parse(csv, "header-only.csv");

        assertEquals("header-only.csv", summary.getFileName());
        assertEquals(0, summary.getRowCount());
        assertEquals(List.of("name", "email", "age"), summary.getColumnNames());
        assertTrue(summary.getFirstRows().isEmpty());
    }

    @Test
    void handlesMissingValues() throws IOException {
        String csv = "a,b,c\n1,,3\n";

        CsvSummary summary = parser.parse(csv, "sparse.csv");

        assertEquals(1, summary.getRowCount());
        assertEquals("", summary.getFirstRows().get(0).get("b"));
        assertEquals("3", summary.getFirstRows().get(0).get("c"));
    }

    @Test
    void handlesFewerValuesThanColumns() throws IOException {
        String csv = "a,b,c\n1\n";

        CsvSummary summary = parser.parse(csv, "short.csv");

        assertEquals(1, summary.getRowCount());
        Map<String, String> row = summary.getFirstRows().get(0);
        assertEquals("1", row.get("a"));
        assertEquals("", row.get("b"));
        assertEquals("", row.get("c"));
    }

    @Test
    void skipsBlankLines() throws IOException {
        String csv = "x,y\n1,2\n\n3,4\n\n";

        CsvSummary summary = parser.parse(csv, "blanks.csv");

        assertEquals(2, summary.getRowCount());
    }
}
