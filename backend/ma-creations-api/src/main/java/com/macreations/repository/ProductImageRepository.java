package com.macreations.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.macreations.entity.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderBySortOrderAsc(Long productId);
}
