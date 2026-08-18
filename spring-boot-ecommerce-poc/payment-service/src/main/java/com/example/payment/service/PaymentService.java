package com.example.payment.service;

import com.example.common.enums.PaymentStatus;
import com.example.common.request.PaymentRequest;
import com.example.common.response.PaymentResponse;
import com.example.common.util.IdGenerator;
import com.example.payment.entity.PaymentRecord;
import com.example.payment.processor.PaymentProcessor;
import com.example.payment.repository.PaymentRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentProcessor paymentProcessor;
    private final PaymentRecordRepository paymentRecordRepository;

    public PaymentService(PaymentProcessor paymentProcessor,
                          PaymentRecordRepository paymentRecordRepository) {
        this.paymentProcessor = paymentProcessor;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        // Idempotency check: if the same key was used before, return cached result
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Optional<PaymentRecord> existing = paymentRecordRepository
                    .findByIdempotencyKey(request.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Idempotent request detected for key: {}. Returning cached result.",
                        request.idempotencyKey());
                return toResponse(existing.get());
            }
        }

        // Process payment through the processor
        PaymentStatus status = paymentProcessor.charge(request);

        // Persist payment record
        PaymentRecord record = new PaymentRecord();
        record.setId(IdGenerator.generate("pay"));
        record.setOrderId(request.orderId());
        record.setAmount(request.amount());
        record.setCurrency(request.currency());
        record.setStatus(status);
        record.setCardLast4(request.cardLast4());
        record.setIdempotencyKey(request.idempotencyKey());

        paymentRecordRepository.save(record);

        log.info("Payment processed: id={}, orderId={}, status={}", record.getId(), record.getOrderId(), status);
        return toResponse(record);
    }

    @Transactional
    public PaymentResponse refundPayment(String paymentId) {
        PaymentRecord record = paymentRecordRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (record.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Cannot refund payment with status: " + record.getStatus());
        }

        PaymentStatus refundStatus = paymentProcessor.refund(paymentId);
        record.setStatus(refundStatus);
        paymentRecordRepository.save(record);

        log.info("Payment refunded: id={}, orderId={}", paymentId, record.getOrderId());
        return toResponse(record);
    }

    public Optional<PaymentResponse> getPayment(String paymentId) {
        return paymentRecordRepository.findById(paymentId).map(this::toResponse);
    }

    private PaymentResponse toResponse(PaymentRecord record) {
        String message = switch (record.getStatus()) {
            case SUCCESS -> "Payment processed successfully";
            case FAILED -> "Payment declined";
            case REFUNDED -> "Payment refunded";
            case PENDING -> "Payment pending";
        };

        return new PaymentResponse(
                record.getId(),
                record.getOrderId(),
                record.getAmount(),
                record.getCurrency(),
                record.getStatus(),
                record.getCardLast4(),
                message,
                record.getCreatedAt() != null ? record.getCreatedAt().toString() : null
        );
    }
}
