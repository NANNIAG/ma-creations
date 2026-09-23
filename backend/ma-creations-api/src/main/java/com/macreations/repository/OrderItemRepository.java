package com.macreations.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.macreations.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
