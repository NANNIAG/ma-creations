package com.macreations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.macreations.dto.AddCartItemRequest;
import com.macreations.dto.CartResponse;
import com.macreations.dto.UpdateCartItemQuantityRequest;
import com.macreations.entity.Cart;
import com.macreations.entity.CartItem;
import com.macreations.entity.CartStatus;
import com.macreations.entity.Customer;
import com.macreations.entity.Product;
import com.macreations.entity.ProductImage;
import com.macreations.exception.BadRequestException;
import com.macreations.exception.NotFoundException;
import com.macreations.mapper.CartMapper;
import com.macreations.repository.CartItemRepository;
import com.macreations.repository.CartRepository;
import com.macreations.repository.CustomerRepository;
import com.macreations.repository.ProductRepository;
import com.macreations.service.storage.ProductImageStorage;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductImageStorage productImageStorage;

    private CartMapper cartMapper;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartMapper = new CartMapper(productImageStorage);
        cartService = new CartService(cartRepository, cartItemRepository, productRepository, customerRepository, cartMapper);
    }

    @Test
    void getCartWithoutTokenReturnsEmptyCart() {
        CartResponse response = cartService.getCart(null, null);

        assertThat(response.getId()).isNull();
        assertThat(response.getGuestToken()).isNull();
        assertThat(response.getItemCount()).isZero();
        assertThat(response.getSubtotal()).isEqualByComparingTo("0.00");
        assertThat(response.getCurrency()).isEqualTo("INR");
        assertThat(response.getItems()).isEmpty();
        verify(cartRepository, never()).findByGuestTokenAndStatus(any(), any());
    }

    @Test
    void getCartWithInvalidTokenReturnsEmptyCart() {
        when(cartRepository.findByGuestTokenAndStatus("bad-token", CartStatus.ACTIVE))
                .thenReturn(Optional.empty());

        CartResponse response = cartService.getCart(null, "bad-token");

        assertThat(response.getId()).isNull();
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void addItemCreatesGuestCartAndUsesDbPrice() {
        Product product = sampleProduct(5L, "Bottle", "150.00", "200.00");
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            cart.setId(1L);
            return cart;
        });
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem item = invocation.getArgument(0);
            item.setId(10L);
            return item;
        });
        when(productImageStorage.toPublicUrl("img.jpg")).thenReturn("/api/media/img.jpg");

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(5L);
        request.setQuantity(2);

        CartResponse response = cartService.addItem(null, null, request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getGuestToken()).isNotBlank();
        assertThat(response.getItemCount()).isEqualTo(2);
        assertThat(response.getSubtotal()).isEqualByComparingTo("300.00");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getUnitPrice()).isEqualByComparingTo("150.00");
        assertThat(response.getItems().get(0).getLineTotal()).isEqualByComparingTo("300.00");
        assertThat(response.getItems().get(0).getImageUrl()).isEqualTo("/api/media/img.jpg");

        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(cartCaptor.capture());
        assertThat(cartCaptor.getValue().getStatus()).isEqualTo(CartStatus.ACTIVE);
        assertThat(cartCaptor.getValue().getGuestToken()).hasSize(36);
    }

    @Test
    void addSameProductTwiceIncreasesQuantity() {
        Product product = sampleProduct(5L, "Bottle", "100.00", "120.00");
        Cart cart = activeCart(1L, "token-a");
        CartItem existing = cartItem(10L, cart, product, 1);
        cart.getItems().add(existing);

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(cartRepository.findByGuestTokenAndStatus("token-a", CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 5L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(existing)).thenReturn(existing);

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(5L);
        request.setQuantity(2);

        CartResponse response = cartService.addItem(null, "token-a", request);

        assertThat(existing.getQuantity()).isEqualTo(3);
        assertThat(response.getItemCount()).isEqualTo(3);
        assertThat(response.getSubtotal()).isEqualByComparingTo("300.00");
    }

    @Test
    void updateQuantityUpdatesExistingItem() {
        Product product = sampleProduct(5L, "Bottle", "50.00", "80.00");
        Cart cart = activeCart(1L, "token-a");
        CartItem item = cartItem(10L, cart, product, 1);
        cart.getItems().add(item);

        when(cartRepository.findByGuestTokenAndStatus("token-a", CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(10L, 1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(item)).thenReturn(item);

        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest();
        request.setQuantity(4);

        CartResponse response = cartService.updateQuantity(null, "token-a", 10L, request);

        assertThat(item.getQuantity()).isEqualTo(4);
        assertThat(response.getSubtotal()).isEqualByComparingTo("200.00");
    }

    @Test
    void removeItemDeletesFromCart() {
        Product product = sampleProduct(5L, "Bottle", "50.00", "80.00");
        Cart cart = activeCart(1L, "token-a");
        CartItem item = cartItem(10L, cart, product, 2);
        cart.getItems().add(item);

        when(cartRepository.findByGuestTokenAndStatus("token-a", CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(10L, 1L)).thenReturn(Optional.of(item));

        CartResponse response = cartService.removeItem(null, "token-a", 10L);

        assertThat(cart.getItems()).isEmpty();
        assertThat(response.getItemCount()).isZero();
        verify(cartItemRepository).delete(item);
    }

    @Test
    void clearCartRemovesAllItems() {
        Product product = sampleProduct(5L, "Bottle", "50.00", "80.00");
        Cart cart = activeCart(1L, "token-a");
        cart.getItems().add(cartItem(10L, cart, product, 2));

        when(cartRepository.findByGuestTokenAndStatus("token-a", CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));

        CartResponse response = cartService.clearCart(null, "token-a");

        assertThat(cart.getItems()).isEmpty();
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getSubtotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectsQuantityZero() {
        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(1L);
        request.setQuantity(0);

        assertThatThrownBy(() -> cartService.addItem(null, null, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 1");
    }

    @Test
    void rejectsNegativeQuantity() {
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest();
        request.setQuantity(-3);

        assertThatThrownBy(() -> cartService.updateQuantity(null, "token", 1L, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsQuantityOver99() {
        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(1L);
        request.setQuantity(100);

        assertThatThrownBy(() -> cartService.addItem(null, null, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("99");
    }

    @Test
    void rejectsUnknownProduct() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(404L);
        request.setQuantity(1);

        assertThatThrownBy(() -> cartService.addItem(null, null, request))
                .isInstanceOf(NotFoundException.class)
                .extracting("code")
                .isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    void rejectsCombinedQuantityOver99() {
        Product product = sampleProduct(5L, "Bottle", "10.00", "12.00");
        Cart cart = activeCart(1L, "token-a");
        CartItem existing = cartItem(10L, cart, product, 90);
        cart.getItems().add(existing);

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(cartRepository.findByGuestTokenAndStatus("token-a", CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 5L)).thenReturn(Optional.of(existing));

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(5L);
        request.setQuantity(10);

        assertThatThrownBy(() -> cartService.addItem(null, "token-a", request))
                .isInstanceOf(BadRequestException.class)
                .extracting("code")
                .isEqualTo("QUANTITY_LIMIT_EXCEEDED");
    }

    @Test
    void guestCannotAccessAnotherGuestsCartItem() {
        when(cartRepository.findByGuestTokenAndStatus("token-a", CartStatus.ACTIVE))
                .thenReturn(Optional.of(activeCart(1L, "token-a")));
        when(cartItemRepository.findByIdAndCartId(99L, 1L)).thenReturn(Optional.empty());

        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest();
        request.setQuantity(2);

        assertThatThrownBy(() -> cartService.updateQuantity(null, "token-a", 99L, request))
                .isInstanceOf(NotFoundException.class)
                .extracting("code")
                .isEqualTo("CART_ITEM_NOT_FOUND");
    }

    @Test
    void pricesComeFromProductNotRequest() {
        Product product = sampleProduct(5L, "Bottle", "75.50", "99.00");
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            cart.setId(1L);
            return cart;
        });
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem item = invocation.getArgument(0);
            item.setId(10L);
            return item;
        });

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(5L);
        request.setQuantity(1);

        CartResponse response = cartService.addItem(null, null, request);

        assertThat(response.getItems().get(0).getUnitPrice()).isEqualByComparingTo("75.50");
        assertThat(response.getSubtotal()).isEqualByComparingTo("75.50");
    }

    @Test
    void rejectsHiddenProductOnAdd() {
        Product product = sampleProduct(5L, "Bottle", "50.00", "80.00");
        product.setPublished(false);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(5L);
        request.setQuantity(1);

        assertThatThrownBy(() -> cartService.addItem(null, null, request))
                .isInstanceOf(NotFoundException.class)
                .extracting("code")
                .isEqualTo("PRODUCT_NOT_FOUND");
        verify(cartRepository, never()).save(any());
    }

    @Test
    void getCartFiltersHiddenProductsFromResponse() {
        Product published = sampleProduct(5L, "Visible", "50.00", "80.00");
        Product hidden = sampleProduct(6L, "Hidden", "30.00", "40.00");
        hidden.setPublished(false);

        Cart cart = activeCart(1L, "token-a");
        cart.getItems().add(cartItem(10L, cart, published, 2));
        cart.getItems().add(cartItem(11L, cart, hidden, 3));

        when(cartRepository.findByGuestTokenAndStatus("token-a", CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(productImageStorage.toPublicUrl("img.jpg")).thenReturn("/api/media/img.jpg");

        CartResponse response = cartService.getCart(null, "token-a");

        assertThat(cart.getItems()).hasSize(2);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo(5L);
        assertThat(response.getItemCount()).isEqualTo(2);
        assertThat(response.getSubtotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void rejectsQuantityUpdateForHiddenProduct() {
        Product hidden = sampleProduct(5L, "Bottle", "50.00", "80.00");
        hidden.setPublished(false);
        Cart cart = activeCart(1L, "token-a");
        CartItem item = cartItem(10L, cart, hidden, 1);
        cart.getItems().add(item);

        when(cartRepository.findByGuestTokenAndStatus("token-a", CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(10L, 1L)).thenReturn(Optional.of(item));

        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest();
        request.setQuantity(4);

        assertThatThrownBy(() -> cartService.updateQuantity(null, "token-a", 10L, request))
                .isInstanceOf(NotFoundException.class)
                .extracting("code")
                .isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    void canRemoveHiddenProductFromCart() {
        Product hidden = sampleProduct(5L, "Bottle", "50.00", "80.00");
        hidden.setPublished(false);
        Cart cart = activeCart(1L, "token-a");
        CartItem item = cartItem(10L, cart, hidden, 2);
        cart.getItems().add(item);

        when(cartRepository.findByGuestTokenAndStatus("token-a", CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(10L, 1L)).thenReturn(Optional.of(item));

        CartResponse response = cartService.removeItem(null, "token-a", 10L);

        assertThat(cart.getItems()).isEmpty();
        assertThat(response.getItemCount()).isZero();
        verify(cartItemRepository).delete(item);
    }

    @Test
    void customerCartCreateAndRetrieve() {
        Customer customer = customer(7L);
        Product product = sampleProduct(5L, "Bottle", "50.00", "80.00");
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(cartRepository.findByCustomerIdAndStatus(7L, CartStatus.ACTIVE)).thenReturn(Optional.empty());
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> {
            Cart c = inv.getArgument(0);
            c.setId(20L);
            return c;
        });
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> {
            CartItem i = inv.getArgument(0);
            i.setId(30L);
            return i;
        });

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(5L);
        request.setQuantity(1);

        CartResponse created = cartService.addItem(7L, "ignored-guest-token", request);
        assertThat(created.getId()).isEqualTo(20L);
        assertThat(created.getGuestToken()).isNull();
        assertThat(created.getItemCount()).isEqualTo(1);

        Cart customerCart = activeCustomerCart(20L, customer);
        customerCart.getItems().add(cartItem(30L, customerCart, product, 1));
        when(cartRepository.findByCustomerIdAndStatus(7L, CartStatus.ACTIVE))
                .thenReturn(Optional.of(customerCart));

        CartResponse fetched = cartService.getCart(7L, "other-guest");
        assertThat(fetched.getId()).isEqualTo(20L);
        assertThat(fetched.getGuestToken()).isNull();
        verify(cartRepository, never()).findByGuestTokenAndStatus(any(), any());
    }

    @Test
    void customerACannotAccessCustomerBCartItem() {
        Customer customerA = customer(1L);
        Cart cartA = activeCustomerCart(10L, customerA);
        when(cartRepository.findByCustomerIdAndStatus(1L, CartStatus.ACTIVE))
                .thenReturn(Optional.of(cartA));
        when(cartItemRepository.findByIdAndCartId(99L, 10L)).thenReturn(Optional.empty());

        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest();
        request.setQuantity(2);

        assertThatThrownBy(() -> cartService.updateQuantity(1L, null, 99L, request))
                .isInstanceOf(NotFoundException.class)
                .extracting("code")
                .isEqualTo("CART_ITEM_NOT_FOUND");
    }

    @Test
    void mergeGuestIntoEmptyCustomerCartAttachesGuest() {
        Customer customer = customer(7L);
        Product product = sampleProduct(5L, "Bottle", "50.00", "80.00");
        Cart guest = activeCart(3L, "guest-token");
        guest.getItems().add(cartItem(11L, guest, product, 2));

        when(cartRepository.findByGuestTokenAndStatus("guest-token", CartStatus.ACTIVE))
                .thenReturn(Optional.of(guest));
        when(cartRepository.findByCustomerIdAndStatus(7L, CartStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        when(cartRepository.save(guest)).thenReturn(guest);

        CartResponse response = cartService.mergeGuestCart(7L, "guest-token");

        assertThat(guest.getCustomer()).isEqualTo(customer);
        assertThat(guest.getGuestToken()).isNull();
        assertThat(guest.getStatus()).isEqualTo(CartStatus.ACTIVE);
        assertThat(response.getGuestToken()).isNull();
        assertThat(response.getItemCount()).isEqualTo(2);
    }

    @Test
    void mergeGuestIntoExistingCustomerCartSumsQuantitiesAndCapsAt99() {
        Customer customer = customer(7L);
        Product product = sampleProduct(5L, "Bottle", "10.00", "12.00");
        Product other = sampleProduct(6L, "Cup", "20.00", "25.00");
        Product hidden = sampleProduct(8L, "Hidden", "5.00", "6.00");
        hidden.setPublished(false);

        Cart guest = activeCart(3L, "guest-token");
        guest.getItems().add(cartItem(11L, guest, product, 20));
        guest.getItems().add(cartItem(12L, guest, other, 1));
        guest.getItems().add(cartItem(13L, guest, hidden, 5));

        Cart customerCart = activeCustomerCart(9L, customer);
        CartItem existing = cartItem(21L, customerCart, product, 90);
        customerCart.getItems().add(existing);

        when(cartRepository.findByGuestTokenAndStatus("guest-token", CartStatus.ACTIVE))
                .thenReturn(Optional.of(guest));
        when(cartRepository.findByCustomerIdAndStatus(7L, CartStatus.ACTIVE))
                .thenReturn(Optional.of(customerCart));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productRepository.findById(6L)).thenReturn(Optional.of(other));
        when(productRepository.findById(8L)).thenReturn(Optional.of(hidden));
        when(cartItemRepository.findByCartIdAndProductId(9L, 5L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.findByCartIdAndProductId(9L, 6L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(guest)).thenReturn(guest);

        CartResponse response = cartService.mergeGuestCart(7L, "guest-token");

        assertThat(existing.getQuantity()).isEqualTo(99);
        assertThat(guest.getStatus()).isEqualTo(CartStatus.MERGED);
        assertThat(response.getItems()).extracting("productId").containsExactlyInAnyOrder(5L, 6L);
        assertThat(response.getItems()).noneMatch(i -> Long.valueOf(8L).equals(i.getProductId()));
    }

    @Test
    void mergeSkipsMissingProduct() {
        Customer customer = customer(7L);
        Product missingRef = sampleProduct(404L, "Gone", "10.00", "12.00");
        Cart guest = activeCart(3L, "guest-token");
        guest.getItems().add(cartItem(11L, guest, missingRef, 1));
        Cart customerCart = activeCustomerCart(9L, customer);

        when(cartRepository.findByGuestTokenAndStatus("guest-token", CartStatus.ACTIVE))
                .thenReturn(Optional.of(guest));
        when(cartRepository.findByCustomerIdAndStatus(7L, CartStatus.ACTIVE))
                .thenReturn(Optional.of(customerCart));
        when(productRepository.findById(404L)).thenReturn(Optional.empty());
        when(cartRepository.save(guest)).thenReturn(guest);

        CartResponse response = cartService.mergeGuestCart(7L, "guest-token");

        assertThat(response.getItems()).isEmpty();
        assertThat(guest.getStatus()).isEqualTo(CartStatus.MERGED);
        verify(cartItemRepository, never()).save(any());
    }

    private static Customer customer(Long id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setMobileNumber("9876543210");
        customer.setEnabled(true);
        return customer;
    }

    private static Cart activeCustomerCart(Long id, Customer customer) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setGuestToken(null);
        cart.setCustomer(customer);
        cart.setStatus(CartStatus.ACTIVE);
        cart.setItems(new ArrayList<>());
        return cart;
    }

    private static Cart activeCart(Long id, String token) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setGuestToken(token);
        cart.setStatus(CartStatus.ACTIVE);
        cart.setItems(new ArrayList<>());
        return cart;
    }

    private static CartItem cartItem(Long id, Cart cart, Product product, int quantity) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(quantity);
        return item;
    }

    private static Product sampleProduct(Long id, String title, String selling, String mrp) {
        Product product = new Product();
        product.setId(id);
        product.setTitle(title);
        product.setSlug(title.toLowerCase().replace(' ', '-'));
        product.setSellingPrice(new BigDecimal(selling));
        product.setMrp(new BigDecimal(mrp));
        product.setPublished(true);
        ProductImage image = new ProductImage();
        image.setId(1L);
        image.setProduct(product);
        image.setStoragePath("img.jpg");
        image.setSortOrder(0);
        product.getImages().add(image);
        return product;
    }
}
