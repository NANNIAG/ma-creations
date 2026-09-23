package com.macreations.service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.macreations.dto.AddCartItemRequest;
import com.macreations.dto.CartResponse;
import com.macreations.dto.UpdateCartItemQuantityRequest;
import com.macreations.entity.Cart;
import com.macreations.entity.CartItem;
import com.macreations.entity.CartStatus;
import com.macreations.entity.Customer;
import com.macreations.entity.Product;
import com.macreations.exception.BadRequestException;
import com.macreations.exception.NotFoundException;
import com.macreations.mapper.CartMapper;
import com.macreations.repository.CartItemRepository;
import com.macreations.repository.CartRepository;
import com.macreations.repository.CustomerRepository;
import com.macreations.repository.ProductRepository;

@Service
public class CartService {

    public static final int MAX_LINE_QUANTITY = 99;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final CartMapper cartMapper;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            CartMapper cartMapper) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.cartMapper = cartMapper;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long customerId, String guestToken) {
        if (customerId != null) {
            return findActiveCustomerCart(customerId)
                    .map(this::hydrateAndMap)
                    .orElseGet(cartMapper::emptyCart);
        }
        if (!StringUtils.hasText(guestToken)) {
            return cartMapper.emptyCart();
        }
        return findActiveGuestCart(guestToken.trim())
                .map(this::hydrateAndMap)
                .orElseGet(cartMapper::emptyCart);
    }

    /**
     * Resolves the ACTIVE cart entity for checkout validation (guest token or customer JWT owner).
     */
    @Transactional(readOnly = true)
    public java.util.Optional<Cart> findActiveCartEntity(Long customerId, String guestToken) {
        if (customerId != null) {
            return findActiveCustomerCart(customerId);
        }
        if (!StringUtils.hasText(guestToken)) {
            return java.util.Optional.empty();
        }
        return findActiveGuestCart(guestToken.trim());
    }

    @Transactional
    public CartResponse addItem(Long customerId, String guestToken, AddCartItemRequest request) {
        validateQuantity(request.getQuantity());
        Product product = requirePublishedProduct(request.getProductId());

        Cart cart = resolveOrCreateCart(customerId, guestToken);
        CartItem existing = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        if (existing == null) {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(request.getQuantity());
            cart.getItems().add(item);
            cartItemRepository.save(item);
        } else {
            int combined = existing.getQuantity() + request.getQuantity();
            if (combined > MAX_LINE_QUANTITY) {
                throw new BadRequestException(
                        "QUANTITY_LIMIT_EXCEEDED",
                        "Quantity must not exceed " + MAX_LINE_QUANTITY);
            }
            existing.setQuantity(combined);
            cartItemRepository.save(existing);
        }

        return hydrateAndMap(cart);
    }

    @Transactional
    public CartResponse updateQuantity(
            Long customerId, String guestToken, Long itemId, UpdateCartItemQuantityRequest request) {
        validateQuantity(request.getQuantity());
        Cart cart = requireActiveCart(customerId, guestToken);
        CartItem item = requireCartItem(cart.getId(), itemId);
        if (item.getProduct() == null || !item.getProduct().isPublished()) {
            throw new NotFoundException("PRODUCT_NOT_FOUND", "Product not found");
        }
        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
        return hydrateAndMap(cart);
    }

    @Transactional
    public CartResponse removeItem(Long customerId, String guestToken, Long itemId) {
        Cart cart = requireActiveCart(customerId, guestToken);
        CartItem item = requireCartItem(cart.getId(), itemId);
        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        return hydrateAndMap(cart);
    }

    @Transactional
    public CartResponse clearCart(Long customerId, String guestToken) {
        Cart cart = requireActiveCart(customerId, guestToken);
        cart.getItems().clear();
        return hydrateAndMap(cart);
    }

    /**
     * Clears items for a known cart id after successful COD placement or prepaid capture.
     * Leaves the cart row ACTIVE so the shopper can keep shopping.
     */
    @Transactional
    public void clearCartById(Long cartId) {
        if (cartId == null) {
            return;
        }
        cartRepository.findById(cartId).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    /**
     * Merge guest cart (X-Cart-Token) into the authenticated customer's active cart.
     * Guest cart is marked MERGED; customer cart remains the sole ACTIVE cart.
     */
    @Transactional
    public CartResponse mergeGuestCart(Long customerId, String guestToken) {
        if (customerId == null) {
            throw new BadRequestException("CUSTOMER_REQUIRED", "Customer authentication is required");
        }

        Optional<Cart> guestOpt = StringUtils.hasText(guestToken)
                ? findActiveGuestCart(guestToken.trim())
                : Optional.empty();

        Optional<Cart> customerOpt = findActiveCustomerCart(customerId);

        if (guestOpt.isEmpty()) {
            return customerOpt.map(this::hydrateAndMap)
                    .orElseGet(() -> hydrateAndMap(createCustomerCart(customerId)));
        }

        Cart guestCart = guestOpt.get();

        if (customerOpt.isEmpty()) {
            attachGuestCartToCustomer(guestCart, customerId);
            return hydrateAndMap(guestCart);
        }

        Cart customerCart = customerOpt.get();
        if (customerCart.getId().equals(guestCart.getId())) {
            return hydrateAndMap(customerCart);
        }

        mergeGuestItemsIntoCustomerCart(guestCart, customerCart);
        guestCart.setStatus(CartStatus.MERGED);
        cartRepository.save(guestCart);
        return hydrateAndMap(customerCart);
    }

    private void attachGuestCartToCustomer(Cart guestCart, Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found"));
        guestCart.setCustomer(customer);
        guestCart.setGuestToken(null);
        guestCart.setStatus(CartStatus.ACTIVE);
        cartRepository.save(guestCart);
    }

    private void mergeGuestItemsIntoCustomerCart(Cart guestCart, Cart customerCart) {
        for (CartItem guestItem : new ArrayList<>(guestCart.getItems())) {
            Product product = guestItem.getProduct();
            if (product == null || product.getId() == null) {
                continue;
            }
            // Reload to detect missing / unpublished
            Optional<Product> productOpt = productRepository.findById(product.getId());
            if (productOpt.isEmpty() || !productOpt.get().isPublished()) {
                continue;
            }
            Product published = productOpt.get();
            CartItem existing = cartItemRepository
                    .findByCartIdAndProductId(customerCart.getId(), published.getId())
                    .orElse(null);
            if (existing == null) {
                CartItem item = new CartItem();
                item.setCart(customerCart);
                item.setProduct(published);
                item.setQuantity(Math.min(guestItem.getQuantity(), MAX_LINE_QUANTITY));
                customerCart.getItems().add(item);
                cartItemRepository.save(item);
            } else {
                int combined = Math.min(MAX_LINE_QUANTITY, existing.getQuantity() + guestItem.getQuantity());
                existing.setQuantity(combined);
                cartItemRepository.save(existing);
            }
        }
    }

    private Cart resolveOrCreateCart(Long customerId, String guestToken) {
        if (customerId != null) {
            return findActiveCustomerCart(customerId)
                    .orElseGet(() -> createCustomerCart(customerId));
        }
        if (StringUtils.hasText(guestToken)) {
            String token = guestToken.trim();
            return findActiveGuestCart(token)
                    .orElseGet(() -> createGuestCart(token));
        }
        return createGuestCart(UUID.randomUUID().toString());
    }

    private Cart createGuestCart(String guestToken) {
        Cart cart = new Cart();
        cart.setGuestToken(guestToken);
        cart.setCustomer(null);
        cart.setStatus(CartStatus.ACTIVE);
        return cartRepository.save(cart);
    }

    private Cart createCustomerCart(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found"));
        Cart cart = new Cart();
        cart.setGuestToken(null);
        cart.setCustomer(customer);
        cart.setStatus(CartStatus.ACTIVE);
        return cartRepository.save(cart);
    }

    private Optional<Cart> findActiveGuestCart(String guestToken) {
        return cartRepository.findByGuestTokenAndStatus(guestToken, CartStatus.ACTIVE);
    }

    private Optional<Cart> findActiveCustomerCart(Long customerId) {
        return cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE);
    }

    private Cart requireActiveCart(Long customerId, String guestToken) {
        if (customerId != null) {
            return findActiveCustomerCart(customerId)
                    .orElseThrow(() -> new NotFoundException("CART_NOT_FOUND", "Cart not found"));
        }
        if (!StringUtils.hasText(guestToken)) {
            throw new BadRequestException("CART_TOKEN_REQUIRED", "Cart token is required");
        }
        return findActiveGuestCart(guestToken.trim())
                .orElseThrow(() -> new NotFoundException("CART_NOT_FOUND", "Cart not found"));
    }

    private Product requirePublishedProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException(
                        "PRODUCT_NOT_FOUND",
                        "Product not found: " + productId));
        if (!product.isPublished()) {
            throw new NotFoundException(
                    "PRODUCT_NOT_FOUND",
                    "Product not found: " + productId);
        }
        return product;
    }

    private CartItem requireCartItem(Long cartId, Long itemId) {
        return cartItemRepository.findByIdAndCartId(itemId, cartId)
                .orElseThrow(() -> new NotFoundException("CART_ITEM_NOT_FOUND", "Cart item not found: " + itemId));
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new BadRequestException("INVALID_QUANTITY", "Quantity must be at least 1");
        }
        if (quantity > MAX_LINE_QUANTITY) {
            throw new BadRequestException(
                    "QUANTITY_LIMIT_EXCEEDED",
                    "Quantity must not exceed " + MAX_LINE_QUANTITY);
        }
    }

    private CartResponse hydrateAndMap(Cart cart) {
        cart.getItems().forEach(item -> {
            Product product = item.getProduct();
            if (product != null) {
                product.getTitle();
                product.getImages().size();
            }
        });
        return cartMapper.toResponse(cart);
    }
}
