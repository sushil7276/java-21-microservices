package com.ecommerce.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Payment> findByOrderId(Long orderId);

    Optional<Payment> findFirstByOrderIdAndStatus(Long orderId, String status);
}
