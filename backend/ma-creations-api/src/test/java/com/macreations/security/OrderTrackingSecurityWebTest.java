package com.macreations.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.macreations.controller.AdminOrderController;
import com.macreations.controller.CustomerOrderController;
import com.macreations.controller.OrderController;
import com.macreations.dto.AdminOrderDetailResponse;
import com.macreations.dto.GuestOrderTrackingResponse;
import com.macreations.dto.UpdateOrderTrackingRequest;
import com.macreations.exception.GlobalExceptionHandler;
import com.macreations.exception.NotFoundException;
import com.macreations.repository.CustomerRepository;
import com.macreations.service.AdminOrderService;
import com.macreations.service.CustomerOrderService;
import com.macreations.service.GuestOrderTrackingService;
import com.macreations.service.OrderPlacementService;

@WebMvcTest(controllers = {
        CustomerOrderController.class,
        AdminOrderController.class,
        OrderController.class
})
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=test-secret-key-at-least-32-characters-long",
        "app.security.customer.jwt.secret=test-customer-jwt-secret-key-32chars-xx"
})
class OrderTrackingSecurityWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerOrderService customerOrderService;

    @MockitoBean
    private AdminOrderService adminOrderService;

    @MockitoBean
    private OrderPlacementService orderPlacementService;

    @MockitoBean
    private GuestOrderTrackingService guestOrderTrackingService;

    @MockitoBean
    private AdminUserDetailsService adminUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomerJwtService customerJwtService;

    @MockitoBean
    private CustomerRepository customerRepository;

    @Test
    void guestTrackIsPublic() throws Exception {
        GuestOrderTrackingResponse response = new GuestOrderTrackingResponse();
        response.setOrderNumber("MAC-G1");
        response.setPlacedAt(Instant.parse("2026-09-22T10:00:00Z"));
        response.setGrandTotal(new BigDecimal("190.00"));
        when(guestOrderTrackingService.track("MAC-G1", "9876543210")).thenReturn(response);

        mockMvc.perform(get("/api/orders/track")
                        .param("orderNumber", "MAC-G1")
                        .param("mobileNumber", "9876543210"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value("MAC-G1"));
    }

    @Test
    void guestTrackWrongMobileReturnsNotFound() throws Exception {
        when(guestOrderTrackingService.track(anyString(), anyString()))
                .thenThrow(new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        mockMvc.perform(get("/api/orders/track")
                        .param("orderNumber", "MAC-G1")
                        .param("mobileNumber", "9123456780"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void unauthenticatedCannotPatchTracking() throws Exception {
        mockMvc.perform(patch("/api/admin/orders/MAC-1/tracking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trackingNumber\":\"AWB-1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotPatchAdminTracking() throws Exception {
        mockMvc.perform(patch("/api/admin/orders/MAC-1/tracking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trackingNumber\":\"AWB-1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanPatchTracking() throws Exception {
        AdminOrderDetailResponse detail = new AdminOrderDetailResponse();
        detail.setOrderNumber("MAC-1");
        detail.setTrackingNumber("AWB-1");
        when(adminOrderService.updateTracking(anyString(), any(UpdateOrderTrackingRequest.class)))
                .thenReturn(detail);

        mockMvc.perform(patch("/api/admin/orders/MAC-1/tracking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trackingNumber\":\"AWB-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trackingNumber").value("AWB-1"));
    }
}
