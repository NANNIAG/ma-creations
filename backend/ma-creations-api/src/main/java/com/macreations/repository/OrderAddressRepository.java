package com.macreations.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.macreations.entity.OrderAddress;

public interface OrderAddressRepository extends JpaRepository<OrderAddress, Long> {
}
