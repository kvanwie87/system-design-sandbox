package com.example.payment.repository;

import com.example.payment.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, String> {

    Optional<PaymentRecord> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentRecord> findByOrderId(String orderId);
}
