package com.macreations.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.macreations.dto.AdminOrderDetailResponse;
import com.macreations.dto.AdminOrderListItemResponse;
import com.macreations.dto.CustomerOrderDetailResponse;
import com.macreations.dto.CustomerOrderListItemResponse;
import com.macreations.dto.GuestOrderTrackingResponse;
import com.macreations.entity.Order;
import com.macreations.entity.OrderAddress;
import com.macreations.entity.OrderItem;
import com.macreations.entity.PaymentTransaction;

@Component
public class OrderDtoMapper {

    public CustomerOrderListItemResponse toCustomerListItem(Order order) {
        CustomerOrderListItemResponse item = new CustomerOrderListItemResponse();
        item.setOrderNumber(order.getOrderNumber());
        item.setPlacedAt(order.getPlacedAt());
        item.setGrandTotal(order.getGrandTotal());
        item.setCurrency(order.getCurrency());
        item.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        item.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
        item.setOrderStatus(order.getStatus() != null ? order.getStatus().name() : null);
        item.setItemCount(order.getItems() == null ? 0 : order.getItems().size());
        return item;
    }

    public CustomerOrderDetailResponse toCustomerDetail(Order order) {
        CustomerOrderDetailResponse detail = new CustomerOrderDetailResponse();
        fillCommonDetail(order, detail);
        return detail;
    }

    public AdminOrderListItemResponse toAdminListItem(Order order) {
        AdminOrderListItemResponse item = new AdminOrderListItemResponse();
        item.setOrderNumber(order.getOrderNumber());
        item.setPlacedAt(order.getPlacedAt());
        if (order.getCustomer() != null) {
            item.setCustomerId(order.getCustomer().getId());
        }
        item.setContactName(order.getContactName());
        item.setContactMobile(order.getContactMobile());
        item.setGrandTotal(order.getGrandTotal());
        item.setCurrency(order.getCurrency());
        item.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        item.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
        item.setOrderStatus(order.getStatus() != null ? order.getStatus().name() : null);
        item.setItemCount(order.getItems() == null ? 0 : order.getItems().size());
        return item;
    }

    public AdminOrderDetailResponse toAdminDetail(Order order, List<PaymentTransaction> transactions) {
        AdminOrderDetailResponse detail = new AdminOrderDetailResponse();
        detail.setOrderNumber(order.getOrderNumber());
        detail.setPlacedAt(order.getPlacedAt());
        if (order.getCustomer() != null) {
            detail.setCustomerId(order.getCustomer().getId());
            detail.setCustomerMobile(order.getCustomer().getMobileNumber());
        }
        detail.setOrderStatus(order.getStatus() != null ? order.getStatus().name() : null);
        detail.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
        detail.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        detail.setCurrency(order.getCurrency());
        detail.setItemsSubtotal(order.getItemsSubtotal());
        detail.setShippingCharge(order.getShippingCharge());
        detail.setCodCharge(order.getCodCharge());
        detail.setTaxAmount(order.getTaxAmount());
        detail.setDiscountAmount(order.getDiscountAmount());
        detail.setGrandTotal(order.getGrandTotal());
        detail.setContactName(order.getContactName());
        detail.setContactMobile(order.getContactMobile());
        detail.setContactEmail(order.getContactEmail());
        detail.setTrackingNumber(order.getTrackingNumber());
        detail.setShippingAddress(mapAddress(order.getShippingAddress()));
        detail.setItems(mapLines(order));
        detail.setPaymentTransactions(mapPaymentTx(transactions));
        return detail;
    }

    public GuestOrderTrackingResponse toGuestTracking(Order order) {
        GuestOrderTrackingResponse detail = new GuestOrderTrackingResponse();
        detail.setOrderNumber(order.getOrderNumber());
        detail.setPlacedAt(order.getPlacedAt());
        detail.setOrderStatus(order.getStatus() != null ? order.getStatus().name() : null);
        detail.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
        detail.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        detail.setTrackingNumber(order.getTrackingNumber());
        detail.setCurrency(order.getCurrency());
        detail.setShippingCharge(order.getShippingCharge());
        detail.setCodCharge(order.getCodCharge());
        detail.setGrandTotal(order.getGrandTotal());
        if (order.getShippingAddress() != null) {
            detail.setDeliveryCity(order.getShippingAddress().getCity());
            detail.setDeliveryPostalCode(order.getShippingAddress().getPostalCode());
        }
        List<GuestOrderTrackingResponse.LineItem> lines = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                GuestOrderTrackingResponse.LineItem line = new GuestOrderTrackingResponse.LineItem();
                line.setTitle(item.getProductTitleSnapshot());
                line.setQuantity(item.getQuantity());
                line.setLineSubtotal(item.getLineSubtotal());
                lines.add(line);
            }
        }
        detail.setItems(lines);
        return detail;
    }

    private void fillCommonDetail(Order order, CustomerOrderDetailResponse detail) {
        detail.setOrderNumber(order.getOrderNumber());
        detail.setPlacedAt(order.getPlacedAt());
        detail.setOrderStatus(order.getStatus() != null ? order.getStatus().name() : null);
        detail.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
        detail.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        detail.setCurrency(order.getCurrency());
        detail.setItemsSubtotal(order.getItemsSubtotal());
        detail.setShippingCharge(order.getShippingCharge());
        detail.setCodCharge(order.getCodCharge());
        detail.setTaxAmount(order.getTaxAmount());
        detail.setDiscountAmount(order.getDiscountAmount());
        detail.setGrandTotal(order.getGrandTotal());
        detail.setContactName(order.getContactName());
        detail.setContactMobile(order.getContactMobile());
        detail.setContactEmail(order.getContactEmail());
        detail.setTrackingNumber(order.getTrackingNumber());
        detail.setShippingAddress(mapAddress(order.getShippingAddress()));
        detail.setItems(mapLines(order));
    }

    private List<CustomerOrderDetailResponse.LineItem> mapLines(Order order) {
        List<CustomerOrderDetailResponse.LineItem> lines = new ArrayList<>();
        if (order.getItems() == null) {
            return lines;
        }
        for (OrderItem item : order.getItems()) {
            CustomerOrderDetailResponse.LineItem line = new CustomerOrderDetailResponse.LineItem();
            line.setTitle(item.getProductTitleSnapshot());
            line.setQuantity(item.getQuantity());
            line.setUnitSellingPrice(item.getUnitSellingPrice());
            line.setLineSubtotal(item.getLineSubtotal());
            lines.add(line);
        }
        return lines;
    }

    private CustomerOrderDetailResponse.AddressSnapshot mapAddress(OrderAddress address) {
        if (address == null) {
            return null;
        }
        CustomerOrderDetailResponse.AddressSnapshot snap = new CustomerOrderDetailResponse.AddressSnapshot();
        snap.setFullName(address.getFullName());
        snap.setMobile(address.getMobile());
        snap.setEmail(address.getEmail());
        snap.setLine1(address.getLine1());
        snap.setLine2(address.getLine2());
        snap.setLandmark(address.getLandmark());
        snap.setCity(address.getCity());
        snap.setState(address.getState());
        snap.setPostalCode(address.getPostalCode());
        snap.setCountry(address.getCountry());
        return snap;
    }

    private List<AdminOrderDetailResponse.PaymentTxSummary> mapPaymentTx(List<PaymentTransaction> transactions) {
        List<AdminOrderDetailResponse.PaymentTxSummary> list = new ArrayList<>();
        if (transactions == null) {
            return list;
        }
        for (PaymentTransaction tx : transactions) {
            AdminOrderDetailResponse.PaymentTxSummary summary = new AdminOrderDetailResponse.PaymentTxSummary();
            summary.setId(tx.getId());
            summary.setProvider(tx.getProvider() != null ? tx.getProvider().name() : null);
            summary.setProviderOrderId(tx.getProviderOrderId());
            summary.setProviderPaymentId(tx.getProviderPaymentId());
            summary.setStatus(tx.getStatus() != null ? tx.getStatus().name() : null);
            summary.setAmount(tx.getAmount());
            summary.setCurrency(tx.getCurrency());
            summary.setPaymentMethod(tx.getPaymentMethod() != null ? tx.getPaymentMethod().name() : null);
            summary.setFailureCode(tx.getFailureCode());
            list.add(summary);
        }
        return list;
    }
}
