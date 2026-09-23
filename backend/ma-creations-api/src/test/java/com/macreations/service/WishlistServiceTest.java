package com.macreations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.macreations.dto.AddWishlistItemRequest;
import com.macreations.dto.WishlistResponse;
import com.macreations.entity.Customer;
import com.macreations.entity.Product;
import com.macreations.entity.ProductImage;
import com.macreations.entity.Wishlist;
import com.macreations.entity.WishlistItem;
import com.macreations.entity.WishlistStatus;
import com.macreations.exception.NotFoundException;
import com.macreations.mapper.WishlistMapper;
import com.macreations.repository.CustomerRepository;
import com.macreations.repository.ProductRepository;
import com.macreations.repository.WishlistItemRepository;
import com.macreations.repository.WishlistRepository;
import com.macreations.service.storage.ProductImageStorage;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;
    @Mock
    private WishlistItemRepository wishlistItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductImageStorage productImageStorage;

    private WishlistMapper wishlistMapper;
    private WishlistService wishlistService;

    @BeforeEach
    void setUp() {
        wishlistMapper = new WishlistMapper(productImageStorage);
        wishlistService = new WishlistService(
                wishlistRepository, wishlistItemRepository, productRepository, customerRepository, wishlistMapper);
    }

    @Test
    void getWithoutTokenReturnsEmpty() {
        WishlistResponse response = wishlistService.getWishlist(null, null);

        assertThat(response.getId()).isNull();
        assertThat(response.getItemCount()).isZero();
        assertThat(response.getItems()).isEmpty();
        verify(wishlistRepository, never()).findByGuestTokenAndStatus(any(), any());
    }

    @Test
    void getWithInvalidTokenReturnsEmpty() {
        when(wishlistRepository.findByGuestTokenAndStatus("bad", WishlistStatus.ACTIVE))
                .thenReturn(Optional.empty());

        WishlistResponse response = wishlistService.getWishlist(null, "bad");

        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void addWithoutTokenCreatesWishlistAndUsesDbPrice() {
        Product product = sampleProduct(5L, "Bottle", "150.00", "200.00");
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(invocation -> {
            Wishlist w = invocation.getArgument(0);
            w.setId(1L);
            return w;
        });
        when(wishlistItemRepository.save(any(WishlistItem.class))).thenAnswer(invocation -> {
            WishlistItem item = invocation.getArgument(0);
            item.setId(10L);
            return item;
        });
        when(productImageStorage.toPublicUrl("img.jpg")).thenReturn("/api/media/img.jpg");

        AddWishlistItemRequest request = new AddWishlistItemRequest();
        request.setProductId(5L);

        WishlistResponse response = wishlistService.addItem(null, null, request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getGuestToken()).isNotBlank();
        assertThat(response.getItemCount()).isEqualTo(1);
        assertThat(response.getItems().get(0).getUnitPrice()).isEqualByComparingTo("150.00");
        assertThat(response.getItems().get(0).getDiscountPercent()).isEqualTo(25);
        assertThat(response.getItems().get(0).getImageUrl()).isEqualTo("/api/media/img.jpg");

        ArgumentCaptor<Wishlist> captor = ArgumentCaptor.forClass(Wishlist.class);
        verify(wishlistRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(WishlistStatus.ACTIVE);
    }

    @Test
    void duplicateAddIsIdempotent() {
        Product product = sampleProduct(5L, "Bottle", "100.00", "120.00");
        Wishlist wishlist = activeWishlist(1L, "token-a");
        WishlistItem existing = wishlistItem(10L, wishlist, product);
        wishlist.getItems().add(existing);

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(wishlistRepository.findByGuestTokenAndStatus("token-a", WishlistStatus.ACTIVE))
                .thenReturn(Optional.of(wishlist));
        when(wishlistItemRepository.findByWishlistIdAndProductId(1L, 5L)).thenReturn(Optional.of(existing));

        AddWishlistItemRequest request = new AddWishlistItemRequest();
        request.setProductId(5L);

        WishlistResponse response = wishlistService.addItem(null, "token-a", request);

        assertThat(response.getItemCount()).isEqualTo(1);
        verify(wishlistItemRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownProduct() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        AddWishlistItemRequest request = new AddWishlistItemRequest();
        request.setProductId(404L);

        assertThatThrownBy(() -> wishlistService.addItem(null, null, request))
                .isInstanceOf(NotFoundException.class)
                .extracting("code")
                .isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    void removeProductFromWishlist() {
        Product product = sampleProduct(5L, "Bottle", "50.00", "80.00");
        Wishlist wishlist = activeWishlist(1L, "token-a");
        WishlistItem item = wishlistItem(10L, wishlist, product);
        wishlist.getItems().add(item);

        when(wishlistRepository.findByGuestTokenAndStatus("token-a", WishlistStatus.ACTIVE))
                .thenReturn(Optional.of(wishlist));
        when(wishlistItemRepository.findByWishlistIdAndProductId(1L, 5L)).thenReturn(Optional.of(item));

        WishlistResponse response = wishlistService.removeItem(null, "token-a", 5L);

        assertThat(wishlist.getItems()).isEmpty();
        assertThat(response.getItemCount()).isZero();
        verify(wishlistItemRepository).delete(item);
    }

    @Test
    void removeNonExistingItemIsNoOp() {
        Wishlist wishlist = activeWishlist(1L, "token-a");
        when(wishlistRepository.findByGuestTokenAndStatus("token-a", WishlistStatus.ACTIVE))
                .thenReturn(Optional.of(wishlist));
        when(wishlistItemRepository.findByWishlistIdAndProductId(1L, 99L)).thenReturn(Optional.empty());

        WishlistResponse response = wishlistService.removeItem(null, "token-a", 99L);

        assertThat(response.getItemCount()).isZero();
        verify(wishlistItemRepository, never()).delete(any());
    }

    @Test
    void clearWishlistRemovesAllItems() {
        Product product = sampleProduct(5L, "Bottle", "50.00", "80.00");
        Wishlist wishlist = activeWishlist(1L, "token-a");
        wishlist.getItems().add(wishlistItem(10L, wishlist, product));

        when(wishlistRepository.findByGuestTokenAndStatus("token-a", WishlistStatus.ACTIVE))
                .thenReturn(Optional.of(wishlist));

        WishlistResponse response = wishlistService.clearWishlist(null, "token-a");

        assertThat(wishlist.getItems()).isEmpty();
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void removeWithoutTokenReturnsEmpty() {
        WishlistResponse response = wishlistService.removeItem(null, null, 5L);

        assertThat(response.getItems()).isEmpty();
        verify(wishlistRepository, never()).findByGuestTokenAndStatus(any(), any());
    }

    @Test
    void guestCannotSeeAnotherGuestsWishlist() {
        when(wishlistRepository.findByGuestTokenAndStatus("token-a", WishlistStatus.ACTIVE))
                .thenReturn(Optional.empty());

        WishlistResponse response = wishlistService.getWishlist(null, "token-a");

        assertThat(response.getItems()).isEmpty();
        verify(wishlistRepository).findByGuestTokenAndStatus(eq("token-a"), eq(WishlistStatus.ACTIVE));
    }

    @Test
    void rejectsHiddenProductOnAdd() {
        Product product = sampleProduct(5L, "Bottle", "50.00", "80.00");
        product.setPublished(false);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        AddWishlistItemRequest request = new AddWishlistItemRequest();
        request.setProductId(5L);

        assertThatThrownBy(() -> wishlistService.addItem(null, null, request))
                .isInstanceOf(NotFoundException.class)
                .extracting("code")
                .isEqualTo("PRODUCT_NOT_FOUND");
        verify(wishlistRepository, never()).save(any());
    }

    @Test
    void getWishlistFiltersHiddenProductsFromResponse() {
        Product published = sampleProduct(5L, "Visible", "50.00", "80.00");
        Product hidden = sampleProduct(6L, "Hidden", "30.00", "40.00");
        hidden.setPublished(false);

        Wishlist wishlist = activeWishlist(1L, "token-a");
        wishlist.getItems().add(wishlistItem(10L, wishlist, published));
        wishlist.getItems().add(wishlistItem(11L, wishlist, hidden));

        when(wishlistRepository.findByGuestTokenAndStatus("token-a", WishlistStatus.ACTIVE))
                .thenReturn(Optional.of(wishlist));
        when(productImageStorage.toPublicUrl("img.jpg")).thenReturn("/api/media/img.jpg");

        WishlistResponse response = wishlistService.getWishlist(null, "token-a");

        assertThat(wishlist.getItems()).hasSize(2);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo(5L);
        assertThat(response.getItemCount()).isEqualTo(1);
    }

    @Test
    void canRemoveHiddenProductFromWishlist() {
        Product hidden = sampleProduct(5L, "Bottle", "50.00", "80.00");
        hidden.setPublished(false);
        Wishlist wishlist = activeWishlist(1L, "token-a");
        WishlistItem item = wishlistItem(10L, wishlist, hidden);
        wishlist.getItems().add(item);

        when(wishlistRepository.findByGuestTokenAndStatus("token-a", WishlistStatus.ACTIVE))
                .thenReturn(Optional.of(wishlist));
        when(wishlistItemRepository.findByWishlistIdAndProductId(1L, 5L)).thenReturn(Optional.of(item));

        WishlistResponse response = wishlistService.removeItem(null, "token-a", 5L);

        assertThat(wishlist.getItems()).isEmpty();
        assertThat(response.getItemCount()).isZero();
        verify(wishlistItemRepository).delete(item);
    }

    @Test
    void customerWishlistCreateAndRetrieve() {
        Customer customer = customer(7L);
        Product product = sampleProduct(5L, "Bottle", "50.00", "80.00");
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(wishlistRepository.findByCustomerIdAndStatus(7L, WishlistStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(inv -> {
            Wishlist w = inv.getArgument(0);
            w.setId(20L);
            return w;
        });
        when(wishlistItemRepository.save(any(WishlistItem.class))).thenAnswer(inv -> {
            WishlistItem i = inv.getArgument(0);
            i.setId(30L);
            return i;
        });

        AddWishlistItemRequest request = new AddWishlistItemRequest();
        request.setProductId(5L);

        WishlistResponse created = wishlistService.addItem(7L, "ignored", request);
        assertThat(created.getGuestToken()).isNull();
        assertThat(created.getItemCount()).isEqualTo(1);

        Wishlist customerWishlist = activeCustomerWishlist(20L, customer);
        customerWishlist.getItems().add(wishlistItem(30L, customerWishlist, product));
        when(wishlistRepository.findByCustomerIdAndStatus(7L, WishlistStatus.ACTIVE))
                .thenReturn(Optional.of(customerWishlist));

        WishlistResponse fetched = wishlistService.getWishlist(7L, "other");
        assertThat(fetched.getId()).isEqualTo(20L);
        verify(wishlistRepository, never()).findByGuestTokenAndStatus(any(), any());
    }

    @Test
    void mergeGuestWishlistUnionsProductsAndMarksMerged() {
        Customer customer = customer(7L);
        Product a = sampleProduct(5L, "A", "10.00", "12.00");
        Product b = sampleProduct(6L, "B", "20.00", "25.00");
        Product hidden = sampleProduct(8L, "H", "5.00", "6.00");
        hidden.setPublished(false);

        Wishlist guest = activeWishlist(3L, "guest-token");
        guest.getItems().add(wishlistItem(11L, guest, a));
        guest.getItems().add(wishlistItem(12L, guest, b));
        guest.getItems().add(wishlistItem(13L, guest, hidden));

        Wishlist customerWishlist = activeCustomerWishlist(9L, customer);
        customerWishlist.getItems().add(wishlistItem(21L, customerWishlist, a));

        when(wishlistRepository.findByGuestTokenAndStatus("guest-token", WishlistStatus.ACTIVE))
                .thenReturn(Optional.of(guest));
        when(wishlistRepository.findByCustomerIdAndStatus(7L, WishlistStatus.ACTIVE))
                .thenReturn(Optional.of(customerWishlist));
        when(productRepository.findById(5L)).thenReturn(Optional.of(a));
        when(productRepository.findById(6L)).thenReturn(Optional.of(b));
        when(productRepository.findById(8L)).thenReturn(Optional.of(hidden));
        when(wishlistItemRepository.findByWishlistIdAndProductId(9L, 5L))
                .thenReturn(Optional.of(customerWishlist.getItems().get(0)));
        when(wishlistItemRepository.findByWishlistIdAndProductId(9L, 6L)).thenReturn(Optional.empty());
        when(wishlistItemRepository.save(any(WishlistItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(wishlistRepository.save(guest)).thenReturn(guest);

        WishlistResponse response = wishlistService.mergeGuestWishlist(7L, "guest-token");

        assertThat(guest.getStatus()).isEqualTo(WishlistStatus.MERGED);
        assertThat(response.getItems()).extracting("productId").containsExactlyInAnyOrder(5L, 6L);
        assertThat(response.getItemCount()).isEqualTo(2);
    }

    @Test
    void mergeGuestWishlistIntoEmptyCustomerAttaches() {
        Customer customer = customer(7L);
        Product product = sampleProduct(5L, "Bottle", "50.00", "80.00");
        Wishlist guest = activeWishlist(3L, "guest-token");
        guest.getItems().add(wishlistItem(11L, guest, product));

        when(wishlistRepository.findByGuestTokenAndStatus("guest-token", WishlistStatus.ACTIVE))
                .thenReturn(Optional.of(guest));
        when(wishlistRepository.findByCustomerIdAndStatus(7L, WishlistStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        when(wishlistRepository.save(guest)).thenReturn(guest);

        WishlistResponse response = wishlistService.mergeGuestWishlist(7L, "guest-token");

        assertThat(guest.getCustomer()).isEqualTo(customer);
        assertThat(guest.getGuestToken()).isNull();
        assertThat(response.getGuestToken()).isNull();
        assertThat(response.getItemCount()).isEqualTo(1);
    }

    private static Customer customer(Long id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setMobileNumber("9876543210");
        customer.setEnabled(true);
        return customer;
    }

    private static Wishlist activeCustomerWishlist(Long id, Customer customer) {
        Wishlist wishlist = new Wishlist();
        wishlist.setId(id);
        wishlist.setGuestToken(null);
        wishlist.setCustomer(customer);
        wishlist.setStatus(WishlistStatus.ACTIVE);
        wishlist.setItems(new ArrayList<>());
        return wishlist;
    }

    private static Wishlist activeWishlist(Long id, String token) {
        Wishlist wishlist = new Wishlist();
        wishlist.setId(id);
        wishlist.setGuestToken(token);
        wishlist.setStatus(WishlistStatus.ACTIVE);
        wishlist.setItems(new ArrayList<>());
        return wishlist;
    }

    private static WishlistItem wishlistItem(Long id, Wishlist wishlist, Product product) {
        WishlistItem item = new WishlistItem();
        item.setId(id);
        item.setWishlist(wishlist);
        item.setProduct(product);
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
