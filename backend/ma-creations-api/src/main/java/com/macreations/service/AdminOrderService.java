package com.macreations.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.macreations.dto.AdminOrderDetailResponse;
import com.macreations.dto.AdminOrderListItemResponse;
import com.macreations.dto.PageResponse;
import com.macreations.dto.UpdateOrderStatusRequest;
import com.macreations.dto.UpdateOrderTrackingRequest;
import com.macreations.entity.Order;
import com.macreations.entity.OrderStatus;
import com.macreations.entity.PaymentStatus;
import com.macreations.entity.PaymentTransaction;
import com.macreations.exception.BadRequestException;
import com.macreations.exception.NotFoundException;
import com.macreations.repository.OrderRepository;
import com.macreations.repository.PaymentTransactionRepository;

@Service
public class AdminOrderService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderDtoMapper orderDtoMapper;
    private final OrderStatusTransitionValidator transitionValidator;

    public AdminOrderService(
            OrderRepository orderRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            OrderDtoMapper orderDtoMapper,
            OrderStatusTransitionValidator transitionValidator) {
        this.orderRepository = orderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.orderDtoMapper = orderDtoMapper;
        this.transitionValidator = transitionValidator;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminOrderListItemResponse> listOrders(
            String q,
            OrderStatus status,
            PaymentStatus paymentStatus,
            Integer page,
            Integer size) {
        int pageIndex = page == null || page < 0 ? 0 : page;
        int pageSize = normalizeSize(size);
        String query = StringUtils.hasText(q) ? q.trim() : null;
        Page<Order> result = orderRepository.searchForAdmin(
                query,
                status,
                paymentStatus,
                PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "placedAt")));
        result.getContent().forEach(o -> {
            if (o.getItems() != null) {
                o.getItems().size();
            }
            if (o.getCustomer() != null) {
                o.getCustomer().getMobileNumber();
            }
        });
        return PageResponse.of(
                result.getContent().stream().map(orderDtoMapper::toAdminListItem).toList(),
                pageIndex,
                pageSize,
                result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getOrder(String orderNumber) {
        Order order = requireOrder(orderNumber);
        hydrate(order);
        List<PaymentTransaction> txs = paymentTransactionRepository.findByOrderIdOrderByCreatedAtDesc(order.getId());
        return orderDtoMapper.toAdminDetail(order, txs);
    }

    @Transactional
    public AdminOrderDetailResponse updateStatus(String orderNumber, UpdateOrderStatusRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new BadRequestException("STATUS_REQUIRED", "status is required");
        }
        Order order = requireOrder(orderNumber);
        transitionValidator.validate(order.getStatus(), request.getStatus());
        if (order.getStatus() != request.getStatus()) {
            order.setStatus(request.getStatus());
            orderRepository.save(order);
        }
        hydrate(order);
        List<PaymentTransaction> txs = paymentTransactionRepository.findByOrderIdOrderByCreatedAtDesc(order.getId());
        return orderDtoMapper.toAdminDetail(order, txs);
    }

    @Transactional
    public AdminOrderDetailResponse updateTracking(String orderNumber, UpdateOrderTrackingRequest request) {
        if (request == null || !StringUtils.hasText(request.getTrackingNumber())) {
            throw new BadRequestException("TRACKING_NUMBER_REQUIRED", "trackingNumber is required");
        }
        String trackingNumber = request.getTrackingNumber().trim();
        if (trackingNumber.isEmpty()) {
            throw new BadRequestException("TRACKING_NUMBER_REQUIRED", "trackingNumber is required");
        }
        if (trackingNumber.length() > 64) {
            throw new BadRequestException("TRACKING_NUMBER_TOO_LONG", "trackingNumber must be at most 64 characters");
        }
        Order order = requireOrder(orderNumber);
        order.setTrackingNumber(trackingNumber);
        orderRepository.save(order);
        hydrate(order);
        List<PaymentTransaction> txs = paymentTransactionRepository.findByOrderIdOrderByCreatedAtDesc(order.getId());
        return orderDtoMapper.toAdminDetail(order, txs);
    }

    private Order requireOrder(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber.trim())
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
    }

    private void hydrate(Order order) {
        if (order.getItems() != null) {
            order.getItems().size();
        }
        if (order.getShippingAddress() != null) {
            order.getShippingAddress().getCity();
        }
        if (order.getCustomer() != null) {
            order.getCustomer().getMobileNumber();
        }
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size > MAX_PAGE_SIZE) {
            throw new BadRequestException("INVALID_PAGE_SIZE", "page size must be at most " + MAX_PAGE_SIZE);
        }
        return size;
    }
}
