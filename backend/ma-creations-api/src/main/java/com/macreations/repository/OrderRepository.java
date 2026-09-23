package com.macreations.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.macreations.entity.Order;
import com.macreations.entity.OrderStatus;
import com.macreations.entity.PaymentStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    List<Order> findByCustomerIdOrderByPlacedAtDesc(Long customerId);

    Page<Order> findByCustomerIdOrderByPlacedAtDesc(Long customerId, Pageable pageable);

    List<Order> findByStatus(OrderStatus status);

    @Query("""
            SELECT o FROM Order o
            LEFT JOIN o.customer c
            WHERE (:status IS NULL OR o.status = :status)
              AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus)
              AND (
                   :q IS NULL OR :q = ''
                   OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR o.contactMobile LIKE CONCAT('%', :q, '%')
                   OR o.guestMobile LIKE CONCAT('%', :q, '%')
                   OR (c IS NOT NULL AND c.mobileNumber LIKE CONCAT('%', :q, '%'))
              )
            """)
    Page<Order> searchForAdmin(
            @Param("q") String q,
            @Param("status") OrderStatus status,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            Pageable pageable);
}
