package com.macreations.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.macreations.dto.WishlistItemResponse;
import com.macreations.dto.WishlistResponse;
import com.macreations.exception.GlobalExceptionHandler;
import com.macreations.security.AdminUserDetailsService;
import com.macreations.security.JsonAuthenticationEntryPoint;
import com.macreations.security.JwtService;
import com.macreations.service.WishlistService;

@WebMvcTest(controllers = WishlistController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WishlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WishlistService wishlistService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AdminUserDetailsService adminUserDetailsService;

    @MockitoBean
    private JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;

    @Test
    void getWishlistReturnsEmptyEnvelope() throws Exception {
        when(wishlistService.getWishlist(isNull(), isNull())).thenReturn(emptyWishlist());

        mockMvc.perform(get("/api/wishlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void addItemReturnsWishlistWithProductInfo() throws Exception {
        when(wishlistService.addItem(isNull(), eq("token-1"), any())).thenReturn(sampleWishlist());

        mockMvc.perform(post("/api/wishlist/items")
                        .header(WishlistController.WISHLIST_TOKEN_HEADER, "token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.guestToken").value("token-1"))
                .andExpect(jsonPath("$.data.itemCount").value(1))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(150.00));
    }

    @Test
    void removeItemAndClearWishlist() throws Exception {
        when(wishlistService.removeItem(isNull(), eq("token-1"), eq(5L))).thenReturn(emptyWishlist());
        when(wishlistService.clearWishlist(isNull(), eq("token-1"))).thenReturn(emptyWishlist());

        mockMvc.perform(delete("/api/wishlist/items/5")
                        .header(WishlistController.WISHLIST_TOKEN_HEADER, "token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount").value(0));

        mockMvc.perform(delete("/api/wishlist")
                        .header(WishlistController.WISHLIST_TOKEN_HEADER, "token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    private static WishlistResponse emptyWishlist() {
        WishlistResponse response = new WishlistResponse();
        response.setItemCount(0);
        response.setItems(List.of());
        return response;
    }

    private static WishlistResponse sampleWishlist() {
        WishlistItemResponse item = new WishlistItemResponse();
        item.setId(10L);
        item.setProductId(5L);
        item.setTitle("Example Product");
        item.setSlug("example-product");
        item.setImageUrl("/api/media/example.jpg");
        item.setUnitPrice(new BigDecimal("150.00"));
        item.setMrp(new BigDecimal("200.00"));
        item.setDiscountPercent(25);

        WishlistResponse response = new WishlistResponse();
        response.setId(1L);
        response.setGuestToken("token-1");
        response.setItemCount(1);
        response.setItems(List.of(item));
        return response;
    }
}
