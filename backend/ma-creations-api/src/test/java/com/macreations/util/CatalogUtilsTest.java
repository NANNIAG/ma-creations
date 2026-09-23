package com.macreations.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CatalogUtilsTest {

    @Test
    void calculatesDiscountPercent() {
        assertThat(CatalogUtils.calculateDiscountPercent(new BigDecimal("124"), new BigDecimal("152")))
                .isEqualTo(18);
    }

    @Test
    void returnsZeroWhenNoDiscount() {
        assertThat(CatalogUtils.calculateDiscountPercent(new BigDecimal("100"), new BigDecimal("100")))
                .isEqualTo(0);
    }

    @Test
    void slugifiesTitle() {
        assertThat(CatalogUtils.slugify("Modern Water Bottles")).isEqualTo("modern-water-bottles");
    }

    @Test
    void escapesLikeWildcardsInContainsPattern() {
        assertThat(CatalogUtils.toContainsLikePattern("100%_off"))
                .isEqualTo("%100\\%\\_off%");
        assertThat(CatalogUtils.toContainsLikePattern("a\\b"))
                .isEqualTo("%a\\\\b%");
    }
}
