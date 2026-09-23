package com.macreations.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.macreations.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    Optional<Product> findByIdAndPublishedTrue(Long id);

    List<Product> findByPublishedTrue(Sort sort);

    List<Product> findByCategoryIdAndPublishedTrue(Long categoryId, Sort sort);

    List<Product> findByCategoryId(Long categoryId, Sort sort);

    List<Product> findByCategorySlug(String categorySlug, Sort sort);

    List<Product> findAllByOrderByCreatedAtDesc();

    @Query("SELECT p FROM Product p JOIN p.category c WHERE p.published = true "
            + "AND (LOWER(p.title) LIKE LOWER(:pattern) ESCAPE '\\' "
            + "OR LOWER(c.name) LIKE LOWER(:pattern) ESCAPE '\\' "
            + "OR LOWER(p.slug) LIKE LOWER(:pattern) ESCAPE '\\')")
    List<Product> search(@Param("pattern") String pattern, Sort sort);

    @Query("SELECT p FROM Product p JOIN p.category c WHERE p.published = true "
            + "AND p.category.id = :categoryId "
            + "AND (LOWER(p.title) LIKE LOWER(:pattern) ESCAPE '\\' "
            + "OR LOWER(c.name) LIKE LOWER(:pattern) ESCAPE '\\' "
            + "OR LOWER(p.slug) LIKE LOWER(:pattern) ESCAPE '\\')")
    List<Product> searchByCategoryId(
            @Param("categoryId") Long categoryId,
            @Param("pattern") String pattern,
            Sort sort);
}
