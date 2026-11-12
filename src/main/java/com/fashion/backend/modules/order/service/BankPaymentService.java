package com.fashion.backend.modules.order.service;

import com.fashion.backend.common.constant.OrderStatus;
import com.fashion.backend.common.constant.PaymentStatus;
import com.fashion.backend.common.exception.BadRequestException;
import com.fashion.backend.common.exception.ResourceNotFoundException;
import com.fashion.backend.modules.order.dto.response.PaymentReconciliationResponse;
import com.fashion.backend.modules.order.entity.Order;
import com.fashion.backend.modules.order.entity.OrderItem;
import com.fashion.backend.modules.order.entity.Payment;
import com.fashion.backend.modules.order.entity.PaymentTransaction;
import com.fashion.backend.modules.order.repository.PaymentRepository;
import com.fashion.backend.modules.order.repository.PaymentTransactionRepository;
import com.fashion.backend.modules.product.entity.ProductVariant;
import com.fashion.backend.modules.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankPaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final ProductVariantRepository productVariantRepository;

    @Value("${app.payment.expiry-minutes:15}")
    private long expiryMinutes;

    @Value("${app.payment.vietqr.bank-id:}")
    private String bankId;

    @Value("${app.payment.vietqr.account-number:}")
    private String accountNumber;

    @Value("${app.payment.vietqr.account-name:}")
    private String accountName;

    @Value("${app.payment.vietqr.template:compact2}")
    private String template;

    @Value("${app.payment.vietqr.account-number:}")
    private String configuredAccountNumber;

    @Value("${app.payment.sepay-webhook-secret:}")
    private String sepayWebhookSecret;

    @Transactional
    public Payment createPayment(Order order) {
        if (isBlank(bankId) || isBlank(accountNumber) || isBlank(accountName)
                || isBlank(configuredAccountNumber) || isBlank(sepayWebhookSecret)) {
            throw new BadRequestException("Thanh toán chuyển khoản hiện chưa được cấu hình.");
        }
        if (order.getTotalAmount().stripTrailingZeros().scale() > 0) {
            throw new BadRequestException("Tổng tiền chuyển khoản phải là số nguyên đồng.");
        }
        String reference = String.format(Locale.ROOT, "DH%06d", order.getId());
        Payment payment = paymentRepository.save(Payment.builder()
                .order(order)
                .paymentReference(reference)
                .amount(order.getTotalAmount())
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .status(PaymentStatus.PENDING)
                .build());
        return payment;
    }

    @Transactional(readOnly = true)
    public Payment findForOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId).orElse(null);
    }

    @Transactional
    public void processWebhook(SePayWebhookPayload payload) {
        if (payload.id() == null || payload.transferAmount() == null || payload.transferAmount().signum() <= 0) {
            throw new IllegalArgumentException("Thiếu mã giao dịch hoặc số tiền SePay không hợp lệ.");
        }
        if (transactionRepository.existsBySepayTransactionId(payload.id())) {
            return;
        }
        if (!"in".equalsIgnoreCase(payload.transferType())) {
            return;
        }

        Payment payment = isBlank(payload.code())
                ? null
                : paymentRepository.findByPaymentReferenceForUpdate(payload.code().trim()).orElse(null);
        boolean reviewRequired = true;
        LocalDateTime now = LocalDateTime.now();

        if (payment != null && configuredAccountNumber.equals(payload.accountNumber())) {
            if (payment.getStatus() == PaymentStatus.PENDING
                    && payment.getExpiresAt().isAfter(now)
                    && payment.getOrder().getStatus() == OrderStatus.PENDING
                    && payment.getAmount().compareTo(payload.transferAmount()) == 0) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaidAt(now);
                payment.getOrder().setStatus(OrderStatus.CONFIRMED);
                paymentRepository.save(payment);
                reviewRequired = false;
            } else if (payment.getStatus() == PaymentStatus.PENDING
                    && (!payment.getExpiresAt().isAfter(now)
                    || payment.getOrder().getStatus() == OrderStatus.CANCELLED)) {
                expireAndReleaseInventory(payment, now);
                payment.setStatus(PaymentStatus.REVIEW_REQUIRED);
                paymentRepository.save(payment);
            } else if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.REVIEW_REQUIRED);
                paymentRepository.save(payment);
            }
        }

        transactionRepository.save(PaymentTransaction.builder()
                .sepayTransactionId(payload.id())
                .payment(payment)
                .accountNumber(limit(payload.accountNumber(), 100))
                .transferType(limit(payload.transferType(), 16))
                .paymentCode(limit(payload.code(), 64))
                .content(limit(payload.content(), 1000))
                .transferAmount(payload.transferAmount())
                .transactionDate(limit(payload.transactionDate(), 40))
                .reviewRequired(reviewRequired)
                .receivedAt(now)
                .build());
    }

    @Transactional
    public int expirePendingPayments() {
        List<Payment> expired = paymentRepository.findExpiredForUpdate(
                PaymentStatus.PENDING, LocalDateTime.now(), PageRequest.of(0, 100));
        LocalDateTime now = LocalDateTime.now();
        for (Payment payment : expired) {
            expireAndReleaseInventory(payment, now);
            payment.setStatus(PaymentStatus.EXPIRED);
            payment.getOrder().setStatus(OrderStatus.CANCELLED);
        }
        return expired.size();
    }

    @Transactional
    public void cancelPendingPayment(Long orderId) {
        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId).orElse(null);
        if (payment == null || payment.getStatus() != PaymentStatus.PENDING) {
            if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
                throw new BadRequestException("Đơn đã thanh toán cần được hoàn tiền trước khi hủy.");
            }
            return;
        }
        expireAndReleaseInventory(payment, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<PaymentReconciliationResponse> getReconciliationItems() {
        return transactionRepository.findTop100ByReviewRequiredTrueOrderByReceivedAtDesc().stream()
                .map(transaction -> PaymentReconciliationResponse.builder()
                        .transactionId(transaction.getId())
                        .sepayTransactionId(transaction.getSepayTransactionId())
                        .orderId(transaction.getPayment() == null ? null : transaction.getPayment().getOrder().getId())
                        .paymentReference(transaction.getPayment() == null ? transaction.getPaymentCode()
                                : transaction.getPayment().getPaymentReference())
                        .accountNumber(transaction.getAccountNumber())
                        .transferType(transaction.getTransferType())
                        .paymentCode(transaction.getPaymentCode())
                        .content(transaction.getContent())
                        .transferAmount(transaction.getTransferAmount())
                        .transactionDate(transaction.getTransactionDate())
                        .receivedAt(transaction.getReceivedAt())
                        .build())
                .toList();
    }

    public String createQrUrl(Payment payment) {
        return UriComponentsBuilder.fromUriString("https://img.vietqr.io/image/" + bankId + "-" + accountNumber + "-" + template + ".svg")
                .queryParam("amount", payment.getAmount().toBigInteger())
                .queryParam("addInfo", payment.getPaymentReference())
                .queryParam("accountName", accountName)
                .build()
                .encode()
                .toUriString();
    }

    private void expireAndReleaseInventory(Payment payment, LocalDateTime now) {
        Order order = payment.getOrder();
        for (OrderItem item : order.getItems()) {
            Long variantId = item.getProductVariant().getId();
            ProductVariant variant = productVariantRepository.findByIdForUpdate(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể để hoàn kho: " + variantId));
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            productVariantRepository.save(variant);
        }
        order.setStatus(OrderStatus.CANCELLED);
        payment.setStatus(PaymentStatus.EXPIRED);
        paymentRepository.save(payment);
        log.info("Expired unpaid bank transfer for order {} at {}", order.getId(), now);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String limit(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
