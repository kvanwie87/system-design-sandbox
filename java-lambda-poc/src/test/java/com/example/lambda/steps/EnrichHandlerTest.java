package com.example.lambda.steps;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.example.lambda.model.PipelineState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrichHandlerTest {

    @Mock
    private Context mockContext;

    @Mock
    private LambdaLogger mockLogger;

    private EnrichHandler handler;

    @BeforeEach
    void setUp() {
        when(mockContext.getLogger()).thenReturn(mockLogger);
        handler = new EnrichHandler();
    }

    @Test
    void handleRequest_addsTotalColumn() {
        PipelineState state = new PipelineState();
        state.setRows(new ArrayList<>(List.of(
                createRow("10", "5.99"),
                createRow("7", "8.75")
        )));

        PipelineState result = handler.handleRequest(state, mockContext);

        assertEquals("ENRICHED", result.getStatus());
        assertEquals("59.90", result.getRows().get(0).get("total"));
        assertEquals("61.25", result.getRows().get(1).get("total"));
    }

    @Test
    void handleRequest_nonNumericQuantity_skipsTotal() {
        PipelineState state = new PipelineState();
        Map<String, String> row = new LinkedHashMap<>();
        row.put("quantity", "abc");
        row.put("price", "5.00");
        state.setRows(new ArrayList<>(List.of(row)));

        PipelineState result = handler.handleRequest(state, mockContext);

        assertEquals("ENRICHED", result.getStatus());
        assertNull(result.getRows().get(0).get("total"));
    }

    @Test
    void handleRequest_missingPriceColumn_skipsTotal() {
        PipelineState state = new PipelineState();
        Map<String, String> row = new LinkedHashMap<>();
        row.put("quantity", "10");
        state.setRows(new ArrayList<>(List.of(row)));

        PipelineState result = handler.handleRequest(state, mockContext);

        assertNull(result.getRows().get(0).get("total"));
    }

    @Test
    void handleRequest_emptyRows_returnsEnriched() {
        PipelineState state = new PipelineState();
        state.setRows(Collections.emptyList());

        PipelineState result = handler.handleRequest(state, mockContext);

        assertEquals("ENRICHED", result.getStatus());
    }

    @Test
    void handleRequest_nullRows_returnsEnriched() {
        PipelineState state = new PipelineState();
        state.setRows(null);

        PipelineState result = handler.handleRequest(state, mockContext);

        assertEquals("ENRICHED", result.getStatus());
    }

    private Map<String, String> createRow(String quantity, String price) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("id", "1");
        row.put("product", "Widget");
        row.put("quantity", quantity);
        row.put("status", "active");
        row.put("price", price);
        return row;
    }
}
