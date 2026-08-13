package com.example.webhookserver.model;

/**
 * Stock ticker data embedded within a webhook event.
 *
 * @param symbol        the stock ticker symbol (e.g., "AAPL", "TSLA")
 * @param price         the current stock price
 * @param change        the absolute price change (positive or negative)
 * @param percentChange the percentage change relative to the price
 */
public record StockTickerData(
        String symbol,
        double price,
        double change,
        double percentChange
) {
}
