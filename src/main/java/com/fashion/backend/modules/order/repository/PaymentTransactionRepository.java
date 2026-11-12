package com.fashion.backend.modules.order.repository;

import com.fashion.backend.modules.order.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    boolean existsBySepayTransactionId(Long sepayTransactionId);

    List<PaymentTransaction> findTop100ByReviewRequiredTrueOrderByReceivedAtDesc();
}
