package com.example.lambda.steps;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.example.lambda.model.PipelineState;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Step 2: Filters rows to keep only those with status == "active".
 *
 * Input: PipelineState with rows populated
 * Output: PipelineState with rows filtered to active-only
 */
public class FilterHandler implements RequestHandler<PipelineState, PipelineState> {

    private static final String STATUS_COLUMN = "status";
    private static final String ACTIVE_STATUS = "active";

    @Override
    public PipelineState handleRequest(PipelineState state, Context context) {
        LambdaLogger logger = context.getLogger();

        if (state.getRows() == null || state.getRows().isEmpty()) {
            state.setStatus("FILTERED");
            logger.log("FilterHandler: No rows to filter\n");
            return state;
        }

        int beforeCount = state.getRows().size();

        List<Map<String, String>> filtered = state.getRows().stream()
                .filter(row -> {
                    String status = row.get(STATUS_COLUMN);
                    return status != null && ACTIVE_STATUS.equalsIgnoreCase(status.trim());
                })
                .collect(Collectors.toList());

        state.setRows(filtered);
        state.setStatus("FILTERED");
        logger.log(String.format("FilterHandler: %d -> %d rows (kept active only)%n",
                beforeCount, filtered.size()));

        return state;
    }
}
