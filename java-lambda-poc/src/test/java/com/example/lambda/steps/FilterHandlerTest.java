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
class FilterHandlerTest {

    @Mock
    private Context mockContext;

    @Mock
    private LambdaLogger mockLogger;

    private FilterHandler handler;

    @BeforeEach
    void setUp() {
        when(mockContext.getLogger()).thenReturn(mockLogger);
        handler = new FilterHandler();
    }

    @Test
    void handleRequest_filtersActiveRowsOnly() {
        PipelineState state = new PipelineState();
        state.setRows(List.of(
                createRow("1", "Widget", "active"),
                createRow("2", "Gadget", "inactive"),
                createRow("3", "Thing", "active")
        ));

        PipelineState result = handler.handleRequest(state, mockContext);

        assertEquals("FILTERED", result.getStatus());
        assertEquals(2, result.getRows().size());
        assertEquals("1", result.getRows().get(0).get("id"));
        assertEquals("3", result.getRows().get(1).get("id"));
    }

    @Test
    void handleRequest_caseInsensitiveFilter() {
        PipelineState state = new PipelineState();
        state.setRows(List.of(
                createRow("1", "Widget", "ACTIVE"),
                createRow("2", "Gadget", "Active"),
                createRow("3", "Thing", "INACTIVE")
        ));

        PipelineState result = handler.handleRequest(state, mockContext);

        assertEquals(2, result.getRows().size());
    }

    @Test
    void handleRequest_noActiveRows_returnsEmpty() {
        PipelineState state = new PipelineState();
        state.setRows(List.of(
                createRow("1", "Widget", "inactive"),
                createRow("2", "Gadget", "cancelled")
        ));

        PipelineState result = handler.handleRequest(state, mockContext);

        assertEquals("FILTERED", result.getStatus());
        assertTrue(result.getRows().isEmpty());
    }

    @Test
    void handleRequest_nullRows_returnsFiltered() {
        PipelineState state = new PipelineState();
        state.setRows(null);

        PipelineState result = handler.handleRequest(state, mockContext);

        assertEquals("FILTERED", result.getStatus());
    }

    @Test
    void handleRequest_emptyRows_returnsFiltered() {
        PipelineState state = new PipelineState();
        state.setRows(Collections.emptyList());

        PipelineState result = handler.handleRequest(state, mockContext);

        assertEquals("FILTERED", result.getStatus());
    }

    private Map<String, String> createRow(String id, String product, String status) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("product", product);
        row.put("quantity", "10");
        row.put("status", status);
        row.put("price", "5.00");
        return row;
    }
}
