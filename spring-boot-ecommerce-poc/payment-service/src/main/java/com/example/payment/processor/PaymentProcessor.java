package com.example.payment.processor;

import com.example.common.enums.PaymentStatus;
import com.example.common.request.PaymentRequest;

/**
 * Interface representing an external payment gateway.
 * Implementations can be swapped via configuration (mock for PoC, real gateway for production).
 */
public interface PaymentProcessor {

    /**
     * Charge the given payment request.
     * @return PaymentStatus.SUCCESS or PaymentStatus.FAILED
     */
    PaymentStatus charge(PaymentRequest request);

    /**
     * Refund a previously successful payment.
     * @param paymentId the ID of the payment to refund
     * @return PaymentStatus.REFUNDED on success
     */
    PaymentStatus refund(String paymentId);
}
