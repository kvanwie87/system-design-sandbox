package com.example.lambda;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CsvProcessorServiceTest {

    private CsvProcessorService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        service = new CsvProcessorService();
        objectMapper = new ObjectMapper();
    }

    @Test
    void processCSV_filtersActiveRowsAndAddsTotal() throws Exception {
        String csv = """
                id,product,quantity,status,price
                1,Widget A,10,active,5.99
                2,Widget B,3,inactive,12.50
                3,Gadget C,7,active,8.75
                """;

        String json = service.processCSV(toInputStream(csv));
        List<Map<String, String>> results = parseJson(json);

        assertEquals(2, results.size());

        // First active row
        assertEquals("1", results.get(0).get("id"));
        assertEquals("Widget A", results.get(0).get("product"));
        assertEquals("59.90", results.get(0).get("total"));

        // Second active row
        assertEquals("3", results.get(1).get("id"));
        assertEquals("Gadget C", results.get(1).get("product"));
        assertEquals("61.25", results.get(1).get("total"));
    }

    @Test
    void processCSV_caseInsensitiveStatusFilter() throws Exception {
        String csv = """
                id,product,quantity,status,price
                1,Widget,5,ACTIVE,2.00
                2,Gadget,3,Active,4.00
                3,Thing,1,INACTIVE,10.00
                """;

        String json = service.processCSV(toInputStream(csv));
        List<Map<String, String>> results = parseJson(json);

        assertEquals(2, results.size());
        assertEquals("10.00", results.get(0).get("total"));
        assertEquals("12.00", results.get(1).get("total"));
    }

    @Test
    void processCSV_emptyFile_returnsEmptyArray() throws Exception {
        String csv = "";

        String json = service.processCSV(toInputStream(csv));

        assertEquals("[]", json.trim());
    }

    @Test
    void processCSV_headersOnly_returnsEmptyArray() throws Exception {
        String csv = "id,product,quantity,status,price\n";

        String json = service.processCSV(toInputStream(csv));
        List<Map<String, String>> results = parseJson(json);

        assertTrue(results.isEmpty());
    }

    @Test
    void processCSV_noActiveRows_returnsEmptyArray() throws Exception {
        String csv = """
                id,product,quantity,status,price
                1,Widget A,10,inactive,5.99
                2,Widget B,3,inactive,12.50
                """;

        String json = service.processCSV(toInputStream(csv));
        List<Map<String, String>> results = parseJson(json);

        assertTrue(results.isEmpty());
    }

    @Test
    void processCSV_malformedRow_skipsGracefully() throws Exception {
        String csv = """
                id,product,quantity,status,price
                1,Widget A,10,active,5.99
                2,Widget B
                3,Gadget C,7,active,8.75
                """;

        String json = service.processCSV(toInputStream(csv));
        List<Map<String, String>> results = parseJson(json);

        // Row 2 is too short and should be skipped
        assertEquals(2, results.size());
        assertEquals("1", results.get(0).get("id"));
        assertEquals("3", results.get(1).get("id"));
    }

    @Test
    void processCSV_nonNumericQuantity_skipsTotalEnrichment() throws Exception {
        String csv = """
                id,product,quantity,status,price
                1,Widget A,abc,active,5.99
                """;

        String json = service.processCSV(toInputStream(csv));
        List<Map<String, String>> results = parseJson(json);

        assertEquals(1, results.size());
        assertNull(results.get(0).get("total"));
    }

    @Test
    void processCSV_sampleOrdersFile() throws Exception {
        // Load the actual sample-data/orders.csv from resources
        InputStream input = getClass().getResourceAsStream("/orders.csv");
        assertNotNull(input, "sample orders.csv should be on test classpath");

        String json = service.processCSV(input);
        List<Map<String, String>> results = parseJson(json);

        // 6 active rows out of 10 total
        assertEquals(6, results.size());

        // Verify all rows are active
        for (Map<String, String> row : results) {
            assertEquals("active", row.get("status"));
            assertNotNull(row.get("total"));
        }

        // Spot-check first row
        assertEquals("1", results.get(0).get("id"));
        assertEquals("Widget A", results.get(0).get("product"));
        assertEquals("59.90", results.get(0).get("total"));

        // Spot-check last row
        assertEquals("10", results.get(5).get("id"));
        assertEquals("Thingamajig J", results.get(5).get("product"));
        assertEquals("36.00", results.get(5).get("total"));
    }

    private InputStream toInputStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private List<Map<String, String>> parseJson(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<>() {});
    }
}
