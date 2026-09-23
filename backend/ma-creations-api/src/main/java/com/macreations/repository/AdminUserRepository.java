package com.macreations.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.macreations.entity.AdminUser;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByLoginIdentifier(String loginIdentifier);

    boolean existsByLoginIdentifier(String loginIdentifier);
}
