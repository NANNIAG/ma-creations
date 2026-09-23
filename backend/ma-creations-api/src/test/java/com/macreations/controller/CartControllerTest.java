package com.macreations.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.macreations.dto.CartItemResponse;
import com.macreations.dto.CartResponse;
import com.macreations.exception.GlobalExceptionHandler;
import com.macreations.security.AdminUserDetailsService;
import com.macreations.security.JsonAuthenticationEntryPoint;
import com.macreations.security.JwtService;
import com.macreations.service.CartService;

@WebMvcTest(controllers = CartController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AdminUserDetailsService adminUserDetailsService;

    @MockitoBean
    private JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;

    @Test
    void getCartReturnsEmptyEnvelope() throws Exception {
        when(cartService.getCart(isNull(), isNull())).thenReturn(emptyCart());

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount").value(0))
                .andExpect(jsonPath("$.data.currency").value("INR"))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void addItemAcceptsBodyWithoutClientPrice() throws Exception {
        CartResponse cart = sampleCart();
        when(cartService.addItem(isNull(), eq("token-1"), any())).thenReturn(cart);

        mockMvc.perform(post("/api/cart/items")
                        .header(CartController.CART_TOKEN_HEADER, "token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":5,\"quantity\":1,\"unitPrice\":1.00,\"lineTotal\":1.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.guestToken").value("token-1"))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(150.00));
    }

    @Test
    void addItemRejectsInvalidQuantityViaValidation() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":5,\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateQuantityUsesPathItemId() throws Exception {
        when(cartService.updateQuantity(isNull(), eq("token-1"), eq(10L), any())).thenReturn(sampleCart());

        mockMvc.perform(patch("/api/cart/items/10")
                        .header(CartController.CART_TOKEN_HEADER, "token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void removeItemAndClearCart() throws Exception {
        when(cartService.removeItem(isNull(), eq("token-1"), eq(10L))).thenReturn(emptyCart());
        when(cartService.clearCart(isNull(), eq("token-1"))).thenReturn(emptyCart());

        mockMvc.perform(delete("/api/cart/items/10")
                        .header(CartController.CART_TOKEN_HEADER, "token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount").value(0));

        mockMvc.perform(delete("/api/cart")
                        .header(CartController.CART_TOKEN_HEADER, "token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    private static CartResponse emptyCart() {
        CartResponse response = new CartResponse();
        response.setItemCount(0);
        response.setSubtotal(new BigDecimal("0.00"));
        response.setCurrency("INR");
        response.setItems(List.of());
        return response;
    }

    private static CartResponse sampleCart() {
        CartItemResponse item = new CartItemResponse();
        item.setId(10L);
        item.setProductId(5L);
        item.setTitle("Example Product");
        item.setSlug("example-product");
        item.setImageUrl("/api/media/example.jpg");
        item.setUnitPrice(new BigDecimal("150.00"));
        item.setMrp(new BigDecimal("200.00"));
        item.setQuantity(2);
        item.setLineTotal(new BigDecimal("300.00"));

        CartResponse response = new CartResponse();
        response.setId(1L);
        response.setGuestToken("token-1");
        response.setItemCount(2);
        response.setSubtotal(new BigDecimal("300.00"));
        response.setCurrency("INR");
        response.setItems(List.of(item));
        return response;
    }
}
