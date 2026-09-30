package com.fashion.backend.modules.order.service;

import com.fashion.backend.common.constant.OrderStatus;
import com.fashion.backend.modules.order.dto.request.CreateOrderRequest;
import com.fashion.backend.modules.order.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(Long userId, CreateOrderRequest request);
    List<OrderResponse> getUserOrders(Long userId);
    OrderResponse getOrderById(Long userId, Long orderId, boolean isAdmin);
    List<OrderResponse> getAllOrders();
    OrderResponse updateOrderStatus(Long orderId, OrderStatus status);
}
