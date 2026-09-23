package com.macreations.service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.macreations.dto.AddWishlistItemRequest;
import com.macreations.dto.WishlistResponse;
import com.macreations.entity.Customer;
import com.macreations.entity.Product;
import com.macreations.entity.Wishlist;
import com.macreations.entity.WishlistItem;
import com.macreations.entity.WishlistStatus;
import com.macreations.exception.BadRequestException;
import com.macreations.exception.NotFoundException;
import com.macreations.mapper.WishlistMapper;
import com.macreations.repository.CustomerRepository;
import com.macreations.repository.ProductRepository;
import com.macreations.repository.WishlistItemRepository;
import com.macreations.repository.WishlistRepository;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final WishlistMapper wishlistMapper;

    public WishlistService(
            WishlistRepository wishlistRepository,
            WishlistItemRepository wishlistItemRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            WishlistMapper wishlistMapper) {
        this.wishlistRepository = wishlistRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.wishlistMapper = wishlistMapper;
    }

    @Transactional(readOnly = true)
    public WishlistResponse getWishlist(Long customerId, String guestToken) {
        if (customerId != null) {
            return findActiveCustomerWishlist(customerId)
                    .map(this::hydrateAndMap)
                    .orElseGet(wishlistMapper::emptyWishlist);
        }
        if (!StringUtils.hasText(guestToken)) {
            return wishlistMapper.emptyWishlist();
        }
        return findActiveGuestWishlist(guestToken.trim())
                .map(this::hydrateAndMap)
                .orElseGet(wishlistMapper::emptyWishlist);
    }

    @Transactional
    public WishlistResponse addItem(Long customerId, String guestToken, AddWishlistItemRequest request) {
        Product product = requirePublishedProduct(request.getProductId());

        Wishlist wishlist = resolveOrCreateWishlist(customerId, guestToken);
        Optional<WishlistItem> existing = wishlistItemRepository
                .findByWishlistIdAndProductId(wishlist.getId(), product.getId());

        if (existing.isEmpty()) {
            WishlistItem item = new WishlistItem();
            item.setWishlist(wishlist);
            item.setProduct(product);
            wishlist.getItems().add(item);
            wishlistItemRepository.save(item);
        }

        return hydrateAndMap(wishlist);
    }

    @Transactional
    public WishlistResponse removeItem(Long customerId, String guestToken, Long productId) {
        Optional<Wishlist> wishlistOpt = findActiveWishlist(customerId, guestToken);
        if (wishlistOpt.isEmpty()) {
            return wishlistMapper.emptyWishlist();
        }

        Wishlist wishlist = wishlistOpt.get();
        wishlistItemRepository.findByWishlistIdAndProductId(wishlist.getId(), productId)
                .ifPresent(item -> {
                    wishlist.getItems().remove(item);
                    wishlistItemRepository.delete(item);
                });

        return hydrateAndMap(wishlist);
    }

    @Transactional
    public WishlistResponse clearWishlist(Long customerId, String guestToken) {
        Optional<Wishlist> wishlistOpt = findActiveWishlist(customerId, guestToken);
        if (wishlistOpt.isEmpty()) {
            return wishlistMapper.emptyWishlist();
        }

        Wishlist wishlist = wishlistOpt.get();
        wishlist.getItems().clear();
        return hydrateAndMap(wishlist);
    }

    /**
     * Merge guest wishlist into authenticated customer's active wishlist (union by product).
     */
    @Transactional
    public WishlistResponse mergeGuestWishlist(Long customerId, String guestToken) {
        if (customerId == null) {
            throw new BadRequestException("CUSTOMER_REQUIRED", "Customer authentication is required");
        }

        Optional<Wishlist> guestOpt = StringUtils.hasText(guestToken)
                ? findActiveGuestWishlist(guestToken.trim())
                : Optional.empty();

        Optional<Wishlist> customerOpt = findActiveCustomerWishlist(customerId);

        if (guestOpt.isEmpty()) {
            return customerOpt.map(this::hydrateAndMap)
                    .orElseGet(() -> hydrateAndMap(createCustomerWishlist(customerId)));
        }

        Wishlist guestWishlist = guestOpt.get();

        if (customerOpt.isEmpty()) {
            attachGuestWishlistToCustomer(guestWishlist, customerId);
            return hydrateAndMap(guestWishlist);
        }

        Wishlist customerWishlist = customerOpt.get();
        if (customerWishlist.getId().equals(guestWishlist.getId())) {
            return hydrateAndMap(customerWishlist);
        }

        mergeGuestItemsIntoCustomerWishlist(guestWishlist, customerWishlist);
        guestWishlist.setStatus(WishlistStatus.MERGED);
        wishlistRepository.save(guestWishlist);
        return hydrateAndMap(customerWishlist);
    }

    private void attachGuestWishlistToCustomer(Wishlist guestWishlist, Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found"));
        guestWishlist.setCustomer(customer);
        guestWishlist.setGuestToken(null);
        guestWishlist.setStatus(WishlistStatus.ACTIVE);
        wishlistRepository.save(guestWishlist);
    }

    private void mergeGuestItemsIntoCustomerWishlist(Wishlist guestWishlist, Wishlist customerWishlist) {
        for (WishlistItem guestItem : new ArrayList<>(guestWishlist.getItems())) {
            Product product = guestItem.getProduct();
            if (product == null || product.getId() == null) {
                continue;
            }
            Optional<Product> productOpt = productRepository.findById(product.getId());
            if (productOpt.isEmpty() || !productOpt.get().isPublished()) {
                continue;
            }
            Product published = productOpt.get();
            Optional<WishlistItem> existing = wishlistItemRepository
                    .findByWishlistIdAndProductId(customerWishlist.getId(), published.getId());
            if (existing.isEmpty()) {
                WishlistItem item = new WishlistItem();
                item.setWishlist(customerWishlist);
                item.setProduct(published);
                customerWishlist.getItems().add(item);
                wishlistItemRepository.save(item);
            }
        }
    }

    private Wishlist resolveOrCreateWishlist(Long customerId, String guestToken) {
        if (customerId != null) {
            return findActiveCustomerWishlist(customerId)
                    .orElseGet(() -> createCustomerWishlist(customerId));
        }
        if (StringUtils.hasText(guestToken)) {
            String token = guestToken.trim();
            return findActiveGuestWishlist(token)
                    .orElseGet(() -> createGuestWishlist(token));
        }
        return createGuestWishlist(UUID.randomUUID().toString());
    }

    private Optional<Wishlist> findActiveWishlist(Long customerId, String guestToken) {
        if (customerId != null) {
            return findActiveCustomerWishlist(customerId);
        }
        if (!StringUtils.hasText(guestToken)) {
            return Optional.empty();
        }
        return findActiveGuestWishlist(guestToken.trim());
    }

    private Wishlist createGuestWishlist(String guestToken) {
        Wishlist wishlist = new Wishlist();
        wishlist.setGuestToken(guestToken);
        wishlist.setCustomer(null);
        wishlist.setStatus(WishlistStatus.ACTIVE);
        return wishlistRepository.save(wishlist);
    }

    private Wishlist createCustomerWishlist(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found"));
        Wishlist wishlist = new Wishlist();
        wishlist.setGuestToken(null);
        wishlist.setCustomer(customer);
        wishlist.setStatus(WishlistStatus.ACTIVE);
        return wishlistRepository.save(wishlist);
    }

    private Optional<Wishlist> findActiveGuestWishlist(String guestToken) {
        return wishlistRepository.findByGuestTokenAndStatus(guestToken, WishlistStatus.ACTIVE);
    }

    private Optional<Wishlist> findActiveCustomerWishlist(Long customerId) {
        return wishlistRepository.findByCustomerIdAndStatus(customerId, WishlistStatus.ACTIVE);
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

    private WishlistResponse hydrateAndMap(Wishlist wishlist) {
        wishlist.getItems().forEach(item -> {
            Product product = item.getProduct();
            if (product != null) {
                product.getTitle();
                product.getImages().size();
            }
        });
        return wishlistMapper.toResponse(wishlist);
    }
}
