package com.fashion.backend.modules.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentExpiryScheduler {

    private final BankPaymentService bankPaymentService;

    @Scheduled(fixedDelay = 60_000)
    public void expireUnpaidPayments() {
        bankPaymentService.expirePendingPayments();
    }
}
