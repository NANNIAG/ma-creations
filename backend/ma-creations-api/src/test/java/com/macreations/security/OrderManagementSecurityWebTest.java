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
import java.util.List;

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
import com.macreations.dto.AdminOrderDetailResponse;
import com.macreations.dto.AdminOrderListItemResponse;
import com.macreations.dto.CustomerOrderDetailResponse;
import com.macreations.dto.CustomerOrderListItemResponse;
import com.macreations.dto.PageResponse;
import com.macreations.dto.UpdateOrderStatusRequest;
import com.macreations.exception.GlobalExceptionHandler;
import com.macreations.repository.CustomerRepository;
import com.macreations.service.AdminOrderService;
import com.macreations.service.CustomerOrderService;

@WebMvcTest(controllers = {
        CustomerOrderController.class,
        AdminOrderController.class
})
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=test-secret-key-at-least-32-characters-long",
        "app.security.customer.jwt.secret=test-customer-jwt-secret-key-32chars-xx"
})
class OrderManagementSecurityWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerOrderService customerOrderService;

    @MockitoBean
    private AdminOrderService adminOrderService;

    @MockitoBean
    private AdminUserDetailsService adminUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomerJwtService customerJwtService;

    @MockitoBean
    private CustomerRepository customerRepository;

    @Test
    void unauthenticatedCustomerOrdersRejected() throws Exception {
        mockMvc.perform(get("/api/customer/orders"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/customer/orders/MAC-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedAdminOrdersRejected() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/orders/MAC-1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/admin/orders/MAC-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminTokenCannotAccessCustomerOrderApis() throws Exception {
        mockMvc.perform(get("/api/customer/orders"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/customer/orders/MAC-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerTokenCannotAccessAdminOrderApis() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/orders/MAC-1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/orders/MAC-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCanListAndOpenOwnOrders() throws Exception {
        CustomerOrderListItemResponse item = new CustomerOrderListItemResponse();
        item.setOrderNumber("MAC-1");
        item.setPlacedAt(Instant.parse("2026-09-22T10:00:00Z"));
        item.setGrandTotal(new BigDecimal("190.00"));
        item.setCurrency("INR");
        item.setPaymentMethod("COD");
        item.setPaymentStatus("COD_PENDING");
        item.setOrderStatus("PLACED");
        item.setItemCount(1);
        when(customerOrderService.listMyOrders(any(), any()))
                .thenReturn(PageResponse.of(List.of(item), 0, 20, 1));

        CustomerOrderDetailResponse detail = new CustomerOrderDetailResponse();
        detail.setOrderNumber("MAC-1");
        detail.setOrderStatus("PLACED");
        detail.setPaymentStatus("COD_PENDING");
        detail.setGrandTotal(new BigDecimal("190.00"));
        when(customerOrderService.getMyOrder("MAC-1")).thenReturn(detail);

        mockMvc.perform(get("/api/customer/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].orderNumber").value("MAC-1"));

        mockMvc.perform(get("/api/customer/orders/MAC-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value("MAC-1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListOpenAndUpdateOrders() throws Exception {
        AdminOrderListItemResponse item = new AdminOrderListItemResponse();
        item.setOrderNumber("MAC-1");
        item.setGrandTotal(new BigDecimal("190.00"));
        item.setOrderStatus("PLACED");
        item.setPaymentStatus("COD_PENDING");
        when(adminOrderService.listOrders(any(), any(), any(), any(), any()))
                .thenReturn(PageResponse.of(List.of(item), 0, 20, 1));

        AdminOrderDetailResponse detail = new AdminOrderDetailResponse();
        detail.setOrderNumber("MAC-1");
        detail.setOrderStatus("PROCESSING");
        detail.setPaymentStatus("COD_PENDING");
        when(adminOrderService.getOrder("MAC-1")).thenReturn(detail);
        when(adminOrderService.updateStatus(anyString(), any(UpdateOrderStatusRequest.class)))
                .thenReturn(detail);

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].orderNumber").value("MAC-1"));

        mockMvc.perform(get("/api/admin/orders/MAC-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value("MAC-1"));

        mockMvc.perform(patch("/api/admin/orders/MAC-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("PROCESSING"));
    }
}
