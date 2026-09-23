package com.macreations.service.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.macreations.dto.CheckoutPreviewRequest;
import com.macreations.dto.CheckoutPreviewResponse;
import com.macreations.entity.Cart;
import com.macreations.entity.CartItem;
import com.macreations.entity.CartStatus;
import com.macreations.entity.Customer;
import com.macreations.entity.OrderStatus;
import com.macreations.entity.PaymentMethod;
import com.macreations.entity.PaymentStatus;
import com.macreations.entity.Product;
import com.macreations.exception.BadRequestException;
import com.macreations.repository.ProductRepository;
import com.macreations.service.CartService;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private CartService cartService;
    @Mock
    private ProductRepository productRepository;

    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        // V1 client rules: shipping ₹20 FIXED, COD ₹20 FIXED, tax ZERO (GST not charged)
        checkoutService = new CheckoutService(
                cartService,
                productRepository,
                new ConfigurableShippingChargeCalculator("FIXED", new BigDecimal("20.00")),
                new ConfigurableCodChargeCalculator("FIXED", new BigDecimal("20.00")),
                new ConfigurableTaxCalculator("ZERO", BigDecimal.ZERO),
                new ZeroDiscountCalculator(),
                "v1-test",
                "test-preview-secret-key-32chars!!");
    }

    @Test
    void emptyCartReturnsInvalid() {
        when(cartService.findActiveCartEntity(null, "tok")).thenReturn(Optional.empty());

        CheckoutPreviewRequest request = request(PaymentMethod.UPI);
        CheckoutPreviewResponse response = checkoutService.preview(null, "tok", request);

        assertThat(response.isValid()).isFalse();
        assertThat(response.isReadyToPlace()).isFalse();
        assertThat(response.getIssues()).extracting("code").contains("CART_EMPTY");
    }

    @Test
    void prepaidSubtotal500Shipping20Cod0Tax0GrandTotal520() {
        Product product = product(5L, "Bottle", "500.00", true);
        Cart cart = guestCart(1L, "guest-1", product, 1);
        when(cartService.findActiveCartEntity(null, "guest-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        CheckoutPreviewResponse response = checkoutService.preview(null, "guest-1", request(PaymentMethod.UPI));

        assertThat(response.isValid()).isTrue();
        assertThat(response.isReadyToPlace()).isTrue();
        assertThat(response.getItemsSubtotal()).isEqualByComparingTo("500.00");
        assertThat(response.getShippingCharge()).isEqualByComparingTo("20.00");
        assertThat(response.getCodCharge()).isEqualByComparingTo("0.00");
        assertThat(response.getTaxAmount()).isEqualByComparingTo("0.00");
        assertThat(response.getDiscountAmount()).isEqualByComparingTo("0.00");
        assertThat(response.getGrandTotal()).isEqualByComparingTo("520.00");
        assertThat(response.getPreviewHash()).isNotBlank();
    }

    @Test
    void codSubtotal500Shipping20Cod20Tax0GrandTotal540() {
        Product product = product(5L, "Bottle", "500.00", true);
        Cart cart = guestCart(1L, "guest-1", product, 1);
        when(cartService.findActiveCartEntity(null, "guest-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        CheckoutPreviewResponse response = checkoutService.preview(null, "guest-1", request(PaymentMethod.COD));

        assertThat(response.getItemsSubtotal()).isEqualByComparingTo("500.00");
        assertThat(response.getShippingCharge()).isEqualByComparingTo("20.00");
        assertThat(response.getCodCharge()).isEqualByComparingTo("20.00");
        assertThat(response.getTaxAmount()).isEqualByComparingTo("0.00");
        assertThat(response.getGrandTotal()).isEqualByComparingTo("540.00");
        assertThat(response.getResultingOrderStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(response.getResultingPaymentStatus()).isEqualTo(PaymentStatus.COD_PENDING);
        assertThat(response.isReadyToPlace()).isTrue();
    }

    @Test
    void upiHasZeroCodCharge() {
        assertCodZeroFor(PaymentMethod.UPI);
    }

    @Test
    void cardHasZeroCodCharge() {
        assertCodZeroFor(PaymentMethod.CARD);
    }

    @Test
    void netBankingHasZeroCodCharge() {
        assertCodZeroFor(PaymentMethod.NET_BANKING);
    }

    @Test
    void payLaterHasZeroCodChargeAndRemainsReady() {
        Product product = product(5L, "Bottle", "90.00", true);
        Cart cart = guestCart(1L, "guest-1", product, 1);
        when(cartService.findActiveCartEntity(null, "guest-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        CheckoutPreviewResponse response = checkoutService.preview(null, "guest-1", request(PaymentMethod.PAY_LATER));

        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.PAY_LATER);
        assertThat(response.getCodCharge()).isEqualByComparingTo("0.00");
        assertThat(response.getShippingCharge()).isEqualByComparingTo("20.00");
        assertThat(response.getTaxAmount()).isEqualByComparingTo("0.00");
        assertThat(response.getGrandTotal()).isEqualByComparingTo("110.00");
        assertThat(response.getResultingPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.isReadyToPlace()).isTrue();
    }

    @Test
    void gstIsNotChargedTaxIsZero() {
        Product product = product(5L, "Bottle", "200.00", true);
        Cart cart = guestCart(1L, "guest-1", product, 1);
        when(cartService.findActiveCartEntity(null, "guest-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        CheckoutPreviewResponse response = checkoutService.preview(null, "guest-1", request(PaymentMethod.CARD));

        assertThat(response.getTaxAmount()).isEqualByComparingTo("0.00");
        assertThat(response.getPendingRules()).doesNotContain("TAX_RULE_PENDING");
    }

    @Test
    void previewHashChangesWhenSwitchingFromPrepaidToCod() {
        Product product = product(5L, "Bottle", "500.00", true);
        Cart cart = guestCart(1L, "guest-1", product, 1);
        when(cartService.findActiveCartEntity(null, "guest-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        CheckoutPreviewResponse prepaid = checkoutService.preview(null, "guest-1", request(PaymentMethod.UPI));
        CheckoutPreviewResponse cod = checkoutService.preview(null, "guest-1", request(PaymentMethod.COD));

        assertThat(prepaid.getPreviewHash()).isNotBlank();
        assertThat(cod.getPreviewHash()).isNotBlank();
        assertThat(prepaid.getPreviewHash()).isNotEqualTo(cod.getPreviewHash());
        assertThat(prepaid.getGrandTotal()).isEqualByComparingTo("520.00");
        assertThat(cod.getGrandTotal()).isEqualByComparingTo("540.00");
    }

    @Test
    void validCustomerCartUsesCustomerOwnership() {
        Customer customer = new Customer();
        customer.setId(7L);
        Product product = product(5L, "Bottle", "100.00", true);
        Cart cart = customerCart(2L, customer, product, 1);
        when(cartService.findActiveCartEntity(eq(7L), any())).thenReturn(Optional.of(cart));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        CheckoutPreviewResponse response = checkoutService.preview(7L, "ignored", request(PaymentMethod.CARD));

        assertThat(response.isValid()).isTrue();
        assertThat(response.getItemsSubtotal()).isEqualByComparingTo("100.00");
        assertThat(response.getShippingCharge()).isEqualByComparingTo("20.00");
        assertThat(response.getGrandTotal()).isEqualByComparingTo("120.00");
    }

    @Test
    void hiddenProductIsNotPurchasable() {
        Product product = product(5L, "Bottle", "50.00", false);
        Cart cart = guestCart(1L, "guest-1", product, 1);
        when(cartService.findActiveCartEntity(null, "guest-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        CheckoutPreviewResponse response = checkoutService.preview(null, "guest-1", request(PaymentMethod.UPI));

        assertThat(response.isValid()).isFalse();
        assertThat(response.getIssues()).extracting("code").contains("PRODUCT_UNAVAILABLE");
        assertThat(response.getPreviewHash()).isNull();
    }

    @Test
    void missingProductIsReported() {
        Product stub = product(404L, "Gone", "10.00", true);
        Cart cart = guestCart(1L, "guest-1", stub, 1);
        when(cartService.findActiveCartEntity(null, "guest-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        CheckoutPreviewResponse response = checkoutService.preview(null, "guest-1", request(PaymentMethod.UPI));

        assertThat(response.isValid()).isFalse();
        assertThat(response.getIssues()).extracting("code").contains("PRODUCT_NOT_FOUND");
    }

    @Test
    void invalidQuantityIsReported() {
        Product product = product(5L, "Bottle", "50.00", true);
        Cart cart = guestCart(1L, "guest-1", product, 100);
        when(cartService.findActiveCartEntity(null, "guest-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        CheckoutPreviewResponse response = checkoutService.preview(null, "guest-1", request(PaymentMethod.UPI));

        assertThat(response.isValid()).isFalse();
        assertThat(response.getIssues()).extracting("code").contains("INVALID_QUANTITY");
    }

    @Test
    void priceChangedRequiresReviewAndBlocksReadyToPlace() {
        Product product = product(5L, "Bottle", "180.00", true);
        Cart cart = guestCart(1L, "guest-1", product, 1);
        when(cartService.findActiveCartEntity(null, "guest-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        CheckoutPreviewRequest request = request(PaymentMethod.UPI);
        request.setLastSeenPrices(Map.of(5L, new BigDecimal("150.00")));

        CheckoutPreviewResponse response = checkoutService.preview(null, "guest-1", request);

        assertThat(response.isValid()).isTrue();
        assertThat(response.isRequiresReview()).isTrue();
        assertThat(response.isReadyToPlace()).isFalse();
        assertThat(response.getIssues()).extracting("code").contains("PRICE_CHANGED");
        assertThat(response.getLines().get(0).getUnitSellingPrice()).isEqualByComparingTo("180.00");
        assertThat(response.getPreviewHash()).isNotBlank();
    }

    @Test
    void paymentMethodRequired() {
        assertThatThrownBy(() -> checkoutService.preview(null, "tok", new CheckoutPreviewRequest()))
                .isInstanceOf(BadRequestException.class)
                .extracting("code")
                .isEqualTo("PAYMENT_METHOD_REQUIRED");
    }

    @Test
    void moneyUsesTwoDecimalPrecision() {
        Product product = product(5L, "Bottle", "10.555", true);
        Cart cart = guestCart(1L, "guest-1", product, 3);
        when(cartService.findActiveCartEntity(null, "guest-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        CheckoutPreviewResponse response = checkoutService.preview(null, "guest-1", request(PaymentMethod.UPI));

        assertThat(response.getLines().get(0).getUnitSellingPrice()).isEqualByComparingTo("10.56");
        assertThat(response.getItemsSubtotal()).isEqualByComparingTo("31.68");
        assertThat(response.getShippingCharge()).isEqualByComparingTo("20.00");
        assertThat(response.getGrandTotal()).isEqualByComparingTo("51.68");
    }

    private void assertCodZeroFor(PaymentMethod method) {
        Product product = product(5L, "Bottle", "100.00", true);
        Cart cart = guestCart(1L, "guest-1", product, 1);
        when(cartService.findActiveCartEntity(null, "guest-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        CheckoutPreviewResponse response = checkoutService.preview(null, "guest-1", request(method));

        assertThat(response.getCodCharge()).isEqualByComparingTo("0.00");
        assertThat(response.getShippingCharge()).isEqualByComparingTo("20.00");
        assertThat(response.getTaxAmount()).isEqualByComparingTo("0.00");
        assertThat(response.getGrandTotal()).isEqualByComparingTo("120.00");
    }

    private static CheckoutPreviewRequest request(PaymentMethod method) {
        CheckoutPreviewRequest request = new CheckoutPreviewRequest();
        request.setPaymentMethod(method);
        return request;
    }

    private static Product product(Long id, String title, String price, boolean published) {
        Product product = new Product();
        product.setId(id);
        product.setTitle(title);
        product.setSlug(title.toLowerCase());
        product.setSellingPrice(new BigDecimal(price));
        product.setMrp(new BigDecimal(price).add(new BigDecimal("20")));
        product.setPublished(published);
        return product;
    }

    private static Cart guestCart(Long id, String token, Product product, int qty) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setGuestToken(token);
        cart.setStatus(CartStatus.ACTIVE);
        cart.setItems(new ArrayList<>());
        CartItem item = new CartItem();
        item.setId(10L);
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(qty);
        cart.getItems().add(item);
        return cart;
    }

    private static Cart customerCart(Long id, Customer customer, Product product, int qty) {
        Cart cart = guestCart(id, null, product, qty);
        cart.setGuestToken(null);
        cart.setCustomer(customer);
        return cart;
    }
}
