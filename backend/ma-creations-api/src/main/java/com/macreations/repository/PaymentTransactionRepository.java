package com.macreations.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.macreations.entity.PaymentProvider;
import com.macreations.entity.PaymentTransaction;
import com.macreations.entity.PaymentTransactionStatus;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentTransaction> findByProviderAndProviderOrderId(PaymentProvider provider, String providerOrderId);

    Optional<PaymentTransaction> findByProviderAndProviderPaymentId(PaymentProvider provider, String providerPaymentId);

    Optional<PaymentTransaction> findByLastWebhookEventId(String lastWebhookEventId);

    List<PaymentTransaction> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<PaymentTransaction> findByOrderIdAndStatus(Long orderId, PaymentTransactionStatus status);
}
