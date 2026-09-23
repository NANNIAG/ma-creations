package com.macreations.service.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.macreations.dto.CheckoutIssue;
import com.macreations.dto.CheckoutLinePreview;
import com.macreations.dto.CheckoutPreviewRequest;
import com.macreations.dto.CheckoutPreviewResponse;
import com.macreations.entity.Cart;
import com.macreations.entity.CartItem;
import com.macreations.entity.OrderStatus;
import com.macreations.entity.PaymentMethod;
import com.macreations.entity.PaymentStatus;
import com.macreations.entity.Product;
import com.macreations.exception.BadRequestException;
import com.macreations.repository.ProductRepository;
import com.macreations.service.CartService;

@Service
public class CheckoutService {

    public static final int MAX_LINE_QUANTITY = 99;
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUND = RoundingMode.HALF_UP;

    private final CartService cartService;
    private final ProductRepository productRepository;
    private final ShippingChargeCalculator shippingChargeCalculator;
    private final CodChargeCalculator codChargeCalculator;
    private final TaxCalculator taxCalculator;
    private final DiscountCalculator discountCalculator;
    private final String pricingVersion;
    private final String previewSecret;

    public CheckoutService(
            CartService cartService,
            ProductRepository productRepository,
            ShippingChargeCalculator shippingChargeCalculator,
            CodChargeCalculator codChargeCalculator,
            TaxCalculator taxCalculator,
            DiscountCalculator discountCalculator,
            @Value("${app.checkout.pricing-version:v1}") String pricingVersion,
            @Value("${app.checkout.preview-secret:dev-only-checkout-preview-secret-key}") String previewSecret) {
        this.cartService = cartService;
        this.productRepository = productRepository;
        this.shippingChargeCalculator = shippingChargeCalculator;
        this.codChargeCalculator = codChargeCalculator;
        this.taxCalculator = taxCalculator;
        this.discountCalculator = discountCalculator;
        this.pricingVersion = pricingVersion;
        this.previewSecret = previewSecret;
    }

    @Transactional(readOnly = true)
    public CheckoutPreviewResponse preview(Long customerId, String guestToken, CheckoutPreviewRequest request) {
        if (request == null || request.getPaymentMethod() == null) {
            throw new BadRequestException("PAYMENT_METHOD_REQUIRED", "paymentMethod is required");
        }

        PaymentMethod method = request.getPaymentMethod();
        Cart cart = cartService.findActiveCartEntity(customerId, guestToken).orElse(null);

        CheckoutPreviewResponse response = new CheckoutPreviewResponse();
        response.setPaymentMethod(method);
        response.setCurrency("INR");
        applyResultingStatuses(response, method);

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            response.setValid(false);
            response.setReadyToPlace(false);
            response.setRequiresReview(false);
            response.getIssues().add(CheckoutIssue.of("CART_EMPTY", "Cart is empty"));
            return response;
        }

        Map<Long, BigDecimal> lastSeen = request.getLastSeenPrices();
        List<CheckoutLinePreview> lines = new ArrayList<>();
        List<CheckoutIssue> issues = new ArrayList<>();
        boolean requiresReview = false;
        BigDecimal itemsSubtotal = BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUND);

        for (CartItem item : cart.getItems()) {
            Long productId = item.getProduct() != null ? item.getProduct().getId() : null;
            Integer quantity = item.getQuantity();

            if (productId == null) {
                issues.add(CheckoutIssue.of("PRODUCT_MISSING", "Cart item has no product"));
                continue;
            }

            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) {
                issues.add(CheckoutIssue.forProduct(
                        "PRODUCT_NOT_FOUND", "Product no longer exists", productId));
                continue;
            }
            if (!product.isPublished()) {
                issues.add(CheckoutIssue.forProduct(
                        "PRODUCT_UNAVAILABLE", "Product is not available for purchase", productId));
                continue;
            }
            if (quantity == null || quantity < 1 || quantity > MAX_LINE_QUANTITY) {
                issues.add(CheckoutIssue.forProduct(
                        "INVALID_QUANTITY", "Quantity must be between 1 and " + MAX_LINE_QUANTITY, productId));
                continue;
            }
            if (product.getSellingPrice() == null) {
                issues.add(CheckoutIssue.forProduct(
                        "PRICE_UNAVAILABLE", "Product price is unavailable", productId));
                continue;
            }

            BigDecimal unitPrice = money(product.getSellingPrice());
            BigDecimal lineSubtotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(MONEY_SCALE, MONEY_ROUND);

            CheckoutLinePreview line = new CheckoutLinePreview();
            line.setProductId(productId);
            line.setTitle(product.getTitle());
            line.setSlug(product.getSlug());
            line.setQuantity(quantity);
            line.setUnitSellingPrice(unitPrice);
            line.setUnitMrp(product.getMrp() == null ? null : money(product.getMrp()));
            line.setLineSubtotal(lineSubtotal);

            BigDecimal seen = lastSeen != null ? lastSeen.get(productId) : null;
            if (seen != null) {
                BigDecimal seenMoney = money(seen);
                line.setLastSeenUnitPrice(seenMoney);
                if (seenMoney.compareTo(unitPrice) != 0) {
                    line.setPriceChanged(true);
                    requiresReview = true;
                    CheckoutIssue priceIssue = CheckoutIssue.forProduct(
                            "PRICE_CHANGED",
                            "Product price changed since it was shown in the cart",
                            productId);
                    priceIssue.setPreviousPrice(seenMoney);
                    priceIssue.setCurrentPrice(unitPrice);
                    issues.add(priceIssue);
                }
            }

            lines.add(line);
            itemsSubtotal = itemsSubtotal.add(lineSubtotal);
        }

        response.setLines(lines);
        response.setIssues(issues);
        response.setRequiresReview(requiresReview);
        response.setItemsSubtotal(itemsSubtotal);

        boolean blockingIssues = issues.stream().anyMatch(i -> !"PRICE_CHANGED".equals(i.getCode()));
        response.setValid(!blockingIssues && !lines.isEmpty());

        if (blockingIssues || lines.isEmpty()) {
            response.setReadyToPlace(false);
            return response;
        }

        ChargeCalculation shipping = shippingChargeCalculator.calculate(itemsSubtotal, method);
        ChargeCalculation cod = codChargeCalculator.calculate(itemsSubtotal, method);
        BigDecimal shippingAmount = shipping.configured() ? money(shipping.amount()) : null;
        BigDecimal codAmount = cod.configured() ? money(cod.amount()) : null;

        ChargeCalculation tax = taxCalculator.calculate(
                itemsSubtotal,
                shippingAmount != null ? shippingAmount : BigDecimal.ZERO);
        ChargeCalculation discount = discountCalculator.calculate(itemsSubtotal);

        List<String> pending = new ArrayList<>();
        if (!shipping.configured()) {
            pending.add(shipping.code());
            issues.add(CheckoutIssue.of(shipping.code(), shipping.message()));
        }
        if (!cod.configured()) {
            pending.add(cod.code());
            issues.add(CheckoutIssue.of(cod.code(), cod.message()));
        }
        if (!tax.configured()) {
            pending.add(tax.code());
            issues.add(CheckoutIssue.of(tax.code(), tax.message()));
        }
        if (!discount.configured()) {
            pending.add(discount.code());
            issues.add(CheckoutIssue.of(discount.code(), discount.message()));
        }

        response.setPendingRules(pending);
        response.setShippingCharge(shippingAmount);
        response.setCodCharge(codAmount);
        response.setTaxAmount(tax.configured() ? money(tax.amount()) : null);
        response.setDiscountAmount(discount.configured() ? money(discount.amount()) : null);

        boolean chargesReady = shipping.configured() && cod.configured()
                && tax.configured() && discount.configured();

        if (chargesReady) {
            BigDecimal grand = itemsSubtotal
                    .add(shippingAmount)
                    .add(codAmount)
                    .add(money(tax.amount()))
                    .subtract(money(discount.amount()))
                    .setScale(MONEY_SCALE, MONEY_ROUND);
            response.setGrandTotal(grand);
        }

        boolean ready = response.isValid() && chargesReady && !requiresReview;
        response.setReadyToPlace(ready);

        if (response.isValid() && chargesReady) {
            response.setPreviewHash(buildPreviewHash(
                    cart.getId(),
                    customerId,
                    method,
                    lines,
                    response.getItemsSubtotal(),
                    response.getShippingCharge(),
                    response.getCodCharge(),
                    response.getTaxAmount(),
                    response.getDiscountAmount(),
                    response.getGrandTotal()));
        }

        return response;
    }

    private void applyResultingStatuses(CheckoutPreviewResponse response, PaymentMethod method) {
        if (method == PaymentMethod.COD) {
            response.setResultingOrderStatus(OrderStatus.PLACED);
            response.setResultingPaymentStatus(PaymentStatus.COD_PENDING);
        } else {
            response.setResultingOrderStatus(OrderStatus.PENDING_PAYMENT);
            response.setResultingPaymentStatus(PaymentStatus.PENDING);
        }
    }

    private String buildPreviewHash(
            Long cartId,
            Long customerId,
            PaymentMethod method,
            List<CheckoutLinePreview> lines,
            BigDecimal itemsSubtotal,
            BigDecimal shipping,
            BigDecimal cod,
            BigDecimal tax,
            BigDecimal discount,
            BigDecimal grandTotal) {
        String linePart = lines.stream()
                .sorted(Comparator.comparing(CheckoutLinePreview::getProductId))
                .map(l -> l.getProductId() + ":" + l.getQuantity() + ":" + l.getUnitSellingPrice().toPlainString())
                .collect(Collectors.joining("|"));

        String payload = String.join(";",
                pricingVersion,
                String.valueOf(cartId),
                customerId == null ? "guest" : customerId.toString(),
                method.name(),
                linePart,
                itemsSubtotal.toPlainString(),
                shipping.toPlainString(),
                cod.toPlainString(),
                tax.toPlainString(),
                discount.toPlainString(),
                grandTotal.toPlainString());

        return hmacSha256(payload);
    }

    private String hmacSha256(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(previewSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate checkout preview hash", ex);
        }
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUND);
        }
        return value.setScale(MONEY_SCALE, MONEY_ROUND);
    }
}
