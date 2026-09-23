package com.macreations.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
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

import com.macreations.dto.CheckoutPreviewResponse;
import com.macreations.entity.PaymentMethod;
import com.macreations.exception.GlobalExceptionHandler;
import com.macreations.security.AdminUserDetailsService;
import com.macreations.security.JsonAuthenticationEntryPoint;
import com.macreations.security.JwtService;
import com.macreations.service.checkout.CheckoutService;

@WebMvcTest(controllers = CheckoutController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckoutService checkoutService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AdminUserDetailsService adminUserDetailsService;

    @MockitoBean
    private JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;

    @Test
    void previewReturnsEnvelope() throws Exception {
        CheckoutPreviewResponse preview = new CheckoutPreviewResponse();
        preview.setValid(true);
        preview.setReadyToPlace(false);
        preview.setPaymentMethod(PaymentMethod.UPI);
        preview.setItemsSubtotal(new BigDecimal("100.00"));
        preview.setPendingRules(List.of("SHIPPING_RULE_PENDING"));

        when(checkoutService.preview(isNull(), any(), any())).thenReturn(preview);

        mockMvc.perform(post("/api/checkout/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"UPI\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.paymentMethod").value("UPI"))
                .andExpect(jsonPath("$.data.itemsSubtotal").value(100.00));
    }
}
