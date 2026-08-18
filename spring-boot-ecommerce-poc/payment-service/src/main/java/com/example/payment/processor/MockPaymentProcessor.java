package com.example.payment.processor;

import com.example.common.enums.PaymentStatus;
import com.example.common.request.PaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * In-memory mock payment processor with configurable failure scenarios.
 * Simulates latency, random failures, and card-specific rejections.
 */
@Component
public class MockPaymentProcessor implements PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentProcessor.class);
    private final Random random = new Random();

    @Value("${payment.mock.failure-rate:0}")
    private int failureRatePercent;

    @Value("${payment.mock.latency-ms:100}")
    private int latencyMs;

    @Value("${payment.mock.fail-card-suffix:0000}")
    private String failCardSuffix;

    @Override
    public PaymentStatus charge(PaymentRequest request) {
        simulateLatency();

        // Check for known-bad card numbers
        if (request.cardLast4() != null && request.cardLast4().endsWith(failCardSuffix)) {
            log.info("Payment DECLINED for order {} — card ending in {} is flagged",
                    request.orderId(), request.cardLast4());
            return PaymentStatus.FAILED;
        }

        // Random failure based on configured rate
        if (failureRatePercent > 0 && random.nextInt(100) < failureRatePercent) {
            log.info("Payment DECLINED for order {} — random failure (rate: {}%)",
                    request.orderId(), failureRatePercent);
            return PaymentStatus.FAILED;
        }

        log.info("Payment APPROVED for order {} — amount: {} {}",
                request.orderId(), request.amount(), request.currency());
        return PaymentStatus.SUCCESS;
    }

    @Override
    public PaymentStatus refund(String paymentId) {
        simulateLatency();
        log.info("Payment REFUNDED for paymentId {}", paymentId);
        return PaymentStatus.REFUNDED;
    }

    private void simulateLatency() {
        if (latencyMs > 0) {
            try {
                Thread.sleep(latencyMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
