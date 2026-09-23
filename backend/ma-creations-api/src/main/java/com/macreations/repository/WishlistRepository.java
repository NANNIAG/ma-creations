package com.macreations.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.macreations.entity.Wishlist;
import com.macreations.entity.WishlistStatus;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByGuestTokenAndStatus(String guestToken, WishlistStatus status);

    Optional<Wishlist> findByCustomerIdAndStatus(Long customerId, WishlistStatus status);
}
