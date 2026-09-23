package com.macreations.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.macreations.controller.AdminProductController;
import com.macreations.controller.CartController;
import com.macreations.controller.CheckoutController;
import com.macreations.controller.CustomerAuthController;
import com.macreations.controller.ProductController;
import com.macreations.controller.WishlistController;
import com.macreations.dto.AdminProductListItemResponse;
import com.macreations.dto.CartResponse;
import com.macreations.dto.CheckoutPreviewResponse;
import com.macreations.dto.CustomerResponse;
import com.macreations.dto.ProductDetailResponse;
import com.macreations.dto.RequestOtpResponse;
import com.macreations.dto.WishlistResponse;
import com.macreations.exception.GlobalExceptionHandler;
import com.macreations.repository.CustomerRepository;
import com.macreations.service.CartService;
import com.macreations.service.CustomerAuthService;
import com.macreations.service.ProductService;
import com.macreations.service.WishlistService;
import com.macreations.service.checkout.CheckoutService;

/**
 * Verifies Spring Security rules for admin vs public catalog/cart/wishlist endpoints.
 * Slice tests — no MySQL required.
 */
@WebMvcTest(controllers = {
        AdminProductController.class,
        ProductController.class,
        CartController.class,
        WishlistController.class,
        CustomerAuthController.class,
        CheckoutController.class
})
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=test-secret-key-at-least-32-characters-long",
        "app.security.customer.jwt.secret=test-customer-jwt-secret-key-32chars-xx"
})
class AdminSecurityWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private WishlistService wishlistService;

    @MockitoBean
    private CustomerAuthService customerAuthService;

    @MockitoBean
    private CheckoutService checkoutService;

    @MockitoBean
    private AdminUserDetailsService adminUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomerJwtService customerJwtService;

    @MockitoBean
    private CustomerRepository customerRepository;

    @Test
    void unauthenticatedCreateProductIsRejected() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "a.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] {1});

        mockMvc.perform(multipart("/api/admin/products")
                        .file(image)
                        .param("title", "Bottle")
                        .param("categoryId", "1")
                        .param("sellingPrice", "10")
                        .param("mrp", "20"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void unauthenticatedListIsRejected() throws Exception {
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void unauthenticatedStatusUpdateIsRejected() throws Exception {
        mockMvc.perform(patch("/api/admin/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"published\":false}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void nonAdminCannotUpdateProductStatus() throws Exception {
        mockMvc.perform(patch("/api/admin/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"published\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authenticatedAdminCanCreateProduct() throws Exception {
        ProductDetailResponse created = new ProductDetailResponse();
        created.setId(1L);
        created.setTitle("Bottle");
        when(productService.createProduct(any(), any())).thenReturn(created);

        MockMultipartFile image = new MockMultipartFile(
                "image", "a.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] {1});

        mockMvc.perform(multipart("/api/admin/products")
                        .file(image)
                        .param("title", "Bottle")
                        .param("categoryId", "1")
                        .param("sellingPrice", "10")
                        .param("mrp", "20"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Bottle"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authenticatedAdminCanListProducts() throws Exception {
        when(productService.listAdminProducts()).thenReturn(List.of(new AdminProductListItemResponse()));

        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authenticatedAdminCanUpdateProduct() throws Exception {
        ProductDetailResponse updated = new ProductDetailResponse();
        updated.setId(1L);
        updated.setTitle("Updated");
        when(productService.updateProduct(eq(1L), any(), any())).thenReturn(updated);

        MockMultipartHttpServletRequestBuilder request = multipart("/api/admin/products/1");
        request.with(r -> {
            r.setMethod("PUT");
            return r;
        });

        mockMvc.perform(request
                        .param("title", "Updated")
                        .param("categoryId", "1")
                        .param("sellingPrice", "10")
                        .param("mrp", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authenticatedAdminCanUpdateProductStatus() throws Exception {
        AdminProductListItemResponse item = new AdminProductListItemResponse();
        item.setId(1L);
        item.setPublished(false);
        when(productService.updateProductStatus(eq(1L), any())).thenReturn(item);

        mockMvc.perform(patch("/api/admin/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"published\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.published").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProductEndpointIsNoLongerExposed() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/admin/products/1"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // No DeleteMapping remains; request must not succeed as hard-delete (204).
                    org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(204);
                    org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(200);
                });
    }

    @Test
    void publicProductListRemainsAccessible() throws Exception {
        when(productService.listProducts(any(), any(), any())).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    @Test
    void cartApisRemainPubliclyAccessible() throws Exception {
        CartResponse empty = new CartResponse();
        empty.setItemCount(0);
        empty.setCurrency("INR");
        empty.setItems(java.util.List.of());
        when(cartService.getCart(any(), any())).thenReturn(empty);

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount").value(0));
    }

    @Test
    void wishlistApisRemainPubliclyAccessible() throws Exception {
        WishlistResponse empty = new WishlistResponse();
        empty.setItemCount(0);
        empty.setItems(java.util.List.of());
        when(wishlistService.getWishlist(any(), any())).thenReturn(empty);

        mockMvc.perform(get("/api/wishlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount").value(0));
    }

    @Test
    void adminApisRemainProtectedWhileStorefrontIsPublic() throws Exception {
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isUnauthorized());

        CartResponse emptyCart = new CartResponse();
        emptyCart.setItemCount(0);
        emptyCart.setCurrency("INR");
        emptyCart.setItems(java.util.List.of());
        when(cartService.getCart(any(), any())).thenReturn(emptyCart);

        WishlistResponse emptyWishlist = new WishlistResponse();
        emptyWishlist.setItemCount(0);
        emptyWishlist.setItems(java.util.List.of());
        when(wishlistService.getWishlist(any(), any())).thenReturn(emptyWishlist);

        mockMvc.perform(get("/api/cart")).andExpect(status().isOk());
        mockMvc.perform(get("/api/wishlist")).andExpect(status().isOk());
    }

    @Test
    void checkoutPreviewIsPublic() throws Exception {
        CheckoutPreviewResponse preview = new CheckoutPreviewResponse();
        preview.setValid(false);
        preview.setReadyToPlace(false);
        when(checkoutService.preview(any(), any(), any())).thenReturn(preview);

        mockMvc.perform(post("/api/checkout/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"UPI\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void customerRequestOtpIsPublic() throws Exception {
        when(customerAuthService.requestOtp(any()))
                .thenReturn(new RequestOtpResponse("ok", 300));

        mockMvc.perform(post("/api/customer/auth/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobileNumber\":\"9876543210\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void customerMeRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/customer/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotAccessAdminApis() throws Exception {
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotAccessCustomerMe() throws Exception {
        mockMvc.perform(get("/api/customer/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cartMergeRequiresCustomerAuth() throws Exception {
        mockMvc.perform(post("/api/cart/merge")
                        .header("X-Cart-Token", "guest-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wishlistMergeRequiresCustomerAuth() throws Exception {
        mockMvc.perform(post("/api/wishlist/merge")
                        .header("X-Wishlist-Token", "guest-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotCallCartMerge() throws Exception {
        mockMvc.perform(post("/api/cart/merge")
                        .header("X-Cart-Token", "guest-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCanCallCartMerge() throws Exception {
        CartResponse empty = new CartResponse();
        empty.setItemCount(0);
        empty.setItems(java.util.List.of());
        empty.setCurrency("INR");
        empty.setSubtotal(java.math.BigDecimal.ZERO);
        when(cartService.mergeGuestCart(any(), any())).thenReturn(empty);

        mockMvc.perform(post("/api/cart/merge")
                        .header("X-Cart-Token", "guest-token"))
                .andExpect(status().isOk());
    }
}
