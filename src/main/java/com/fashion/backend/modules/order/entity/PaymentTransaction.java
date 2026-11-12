package com.fashion.backend.modules.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions", uniqueConstraints = @UniqueConstraint(
        name = "uk_payment_transaction_sepay_id", columnNames = "sepay_transaction_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sepay_transaction_id", nullable = false, unique = true)
    private Long sepayTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Column(name = "transfer_type", length = 16)
    private String transferType;

    @Column(name = "payment_code", length = 64)
    private String paymentCode;

    @Column(length = 1000)
    private String content;

    @Column(name = "transfer_amount", precision = 15, scale = 2)
    private BigDecimal transferAmount;

    @Column(name = "transaction_date", length = 40)
    private String transactionDate;

    @Column(name = "review_required", nullable = false)
    @Builder.Default
    private boolean reviewRequired = false;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
}
