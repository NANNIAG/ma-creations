package com.macreations.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.macreations.entity.CustomerOtpChallenge;

public interface CustomerOtpChallengeRepository extends JpaRepository<CustomerOtpChallenge, Long> {

    Optional<CustomerOtpChallenge> findFirstByMobileNumberAndConsumedAtIsNullOrderByCreatedAtDesc(
            String mobileNumber);
}
