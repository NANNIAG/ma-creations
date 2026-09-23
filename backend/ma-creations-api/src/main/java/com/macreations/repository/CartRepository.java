package com.macreations.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.macreations.entity.Cart;
import com.macreations.entity.CartStatus;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByGuestTokenAndStatus(String guestToken, CartStatus status);

    Optional<Cart> findByCustomerIdAndStatus(Long customerId, CartStatus status);
}
