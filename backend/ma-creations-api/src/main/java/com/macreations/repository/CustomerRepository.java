package com.macreations.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.macreations.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByMobileNumber(String mobileNumber);

    boolean existsByMobileNumber(String mobileNumber);
}
