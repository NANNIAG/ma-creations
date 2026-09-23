package com.macreations.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class CatalogUtils {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s_]+");

    private CatalogUtils() {
    }

    public static Integer calculateDiscountPercent(BigDecimal sellingPrice, BigDecimal mrp) {
        if (sellingPrice == null || mrp == null) {
            return null;
        }
        if (mrp.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        if (sellingPrice.compareTo(mrp) >= 0) {
            return 0;
        }
        BigDecimal discount = mrp.subtract(sellingPrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(mrp, 0, RoundingMode.HALF_UP);
        return discount.intValue();
    }

    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "product";
        }
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = WHITESPACE.matcher(normalized.toLowerCase(Locale.ROOT)).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        slug = slug.replaceAll("-{2,}", "-");
        slug = slug.replaceAll("^-|-$", "");
        return slug.isBlank() ? "product" : slug;
    }

    /**
     * Escapes LIKE wildcards so user input is matched literally, then wraps with %.
     */
    public static String toContainsLikePattern(String raw) {
        String escaped = raw
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
