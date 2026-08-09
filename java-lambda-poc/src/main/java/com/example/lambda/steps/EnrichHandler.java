package com.example.lambda.steps;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.example.lambda.model.PipelineState;

import java.util.Map;

/**
 * Step 3: Enriches rows by adding a computed "total" column (quantity * price).
 *
 * Input: PipelineState with filtered rows
 * Output: PipelineState with rows enriched with "total" field
 */
public class EnrichHandler implements RequestHandler<PipelineState, PipelineState> {

    private static final String QUANTITY_COLUMN = "quantity";
    private static final String PRICE_COLUMN = "price";
    private static final String TOTAL_COLUMN = "total";

    @Override
    public PipelineState handleRequest(PipelineState state, Context context) {
        LambdaLogger logger = context.getLogger();

        if (state.getRows() == null || state.getRows().isEmpty()) {
            state.setStatus("ENRICHED");
            logger.log("EnrichHandler: No rows to enrich\n");
            return state;
        }

        int enrichedCount = 0;
        for (Map<String, String> row : state.getRows()) {
            Double total = computeTotal(row);
            if (total != null) {
                row.put(TOTAL_COLUMN, String.format("%.2f", total));
                enrichedCount++;
            }
        }

        state.setStatus("ENRICHED");
        logger.log(String.format("EnrichHandler: Enriched %d/%d rows with total%n",
                enrichedCount, state.getRows().size()));

        return state;
    }

    private Double computeTotal(Map<String, String> row) {
        String quantityStr = row.get(QUANTITY_COLUMN);
        String priceStr = row.get(PRICE_COLUMN);

        if (quantityStr == null || priceStr == null) {
            return null;
        }

        try {
            double quantity = Double.parseDouble(quantityStr.trim());
            double price = Double.parseDouble(priceStr.trim());
            return quantity * price;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
