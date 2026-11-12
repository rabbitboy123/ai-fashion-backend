package com.fashion.backend.modules.order.service;

import com.fashion.backend.common.constant.OrderStatus;
import com.fashion.backend.common.constant.PaymentStatus;
import com.fashion.backend.modules.order.entity.Order;
import com.fashion.backend.modules.order.entity.Payment;
import com.fashion.backend.modules.order.entity.PaymentTransaction;
import com.fashion.backend.modules.order.entity.OrderItem;
import com.fashion.backend.modules.product.entity.ProductVariant;
import com.fashion.backend.modules.order.repository.PaymentRepository;
import com.fashion.backend.modules.order.repository.PaymentTransactionRepository;
import com.fashion.backend.modules.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankPaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentTransactionRepository transactionRepository;
    @Mock private ProductVariantRepository productVariantRepository;

    private BankPaymentService service;
    private Payment payment;

    @BeforeEach
    void setUp() {
        service = new BankPaymentService(paymentRepository, transactionRepository, productVariantRepository);
        ReflectionTestUtils.setField(service, "configuredAccountNumber", "0123456789");
        Order order = Order.builder().id(21L).status(OrderStatus.PENDING).totalAmount(new BigDecimal("250000")).build();
        payment = Payment.builder().id(9L).order(order).paymentReference("DH000021")
                .amount(new BigDecimal("250000")).status(PaymentStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(10)).build();
    }

    @Test
    void exactIncomingTransferConfirmsOrderOnlyOnce() {
        when(transactionRepository.existsBySepayTransactionId(1001L)).thenReturn(false);
        when(paymentRepository.findByPaymentReferenceForUpdate("DH000021")).thenReturn(Optional.of(payment));
        SePayWebhookPayload payload = new SePayWebhookPayload(1001L, "0123456789", "in", "DH000021",
                "DH000021 payment", new BigDecimal("250000"), "2026-11-12 10:00:00");

        service.processWebhook(payload);

        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(OrderStatus.CONFIRMED, payment.getOrder().getStatus());
        ArgumentCaptor<PaymentTransaction> transaction = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(transactionRepository).save(transaction.capture());
        assertFalse(transaction.getValue().isReviewRequired());
    }

    @Test
    void duplicateTransferIsIgnored() {
        when(transactionRepository.existsBySepayTransactionId(1001L)).thenReturn(true);

        service.processWebhook(new SePayWebhookPayload(1001L, "0123456789", "in", "DH000021",
                "duplicate", new BigDecimal("250000"), null));

        verifyNoInteractions(paymentRepository);
        verify(transactionRepository, never()).save(any(PaymentTransaction.class));
    }

    @Test
    void wrongAmountRequiresReconciliation() {
        when(transactionRepository.existsBySepayTransactionId(1002L)).thenReturn(false);
        when(paymentRepository.findByPaymentReferenceForUpdate("DH000021")).thenReturn(Optional.of(payment));

        service.processWebhook(new SePayWebhookPayload(1002L, "0123456789", "in", "DH000021",
                "partial", new BigDecimal("100000"), null));

        assertEquals(PaymentStatus.REVIEW_REQUIRED, payment.getStatus());
        assertEquals(OrderStatus.PENDING, payment.getOrder().getStatus());
        verify(transactionRepository).save(argThat(PaymentTransaction::isReviewRequired));
    }

    @Test
    void expiredPaymentCancelsOrderAndRestoresReservedStock() {
        ProductVariant variant = ProductVariant.builder().id(55L).stockQuantity(2).build();
        OrderItem item = OrderItem.builder().order(payment.getOrder()).productVariant(variant).quantity(3).build();
        payment.getOrder().setItems(java.util.List.of(item));
        payment.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(paymentRepository.findExpiredForUpdate(eq(PaymentStatus.PENDING), any(), any()))
                .thenReturn(java.util.List.of(payment));
        when(productVariantRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(variant));

        assertEquals(1, service.expirePendingPayments());

        assertEquals(PaymentStatus.EXPIRED, payment.getStatus());
        assertEquals(OrderStatus.CANCELLED, payment.getOrder().getStatus());
        assertEquals(5, variant.getStockQuantity());
        verify(productVariantRepository).save(variant);
    }
}
