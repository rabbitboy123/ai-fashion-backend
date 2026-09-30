package com.fashion.backend.modules.order.service;

import com.fashion.backend.common.constant.OrderStatus;
import com.fashion.backend.common.constant.PaymentMethod;
import com.fashion.backend.common.exception.BadRequestException;
import com.fashion.backend.common.exception.ResourceNotFoundException;
import com.fashion.backend.modules.cart.entity.Cart;
import com.fashion.backend.modules.cart.entity.CartItem;
import com.fashion.backend.modules.cart.repository.CartItemRepository;
import com.fashion.backend.modules.cart.repository.CartRepository;
import com.fashion.backend.modules.order.dto.request.CreateOrderRequest;
import com.fashion.backend.modules.order.dto.response.OrderItemResponse;
import com.fashion.backend.modules.order.dto.response.OrderResponse;
import com.fashion.backend.modules.order.entity.Order;
import com.fashion.backend.modules.order.entity.OrderItem;
import com.fashion.backend.modules.order.repository.OrderItemRepository;
import com.fashion.backend.modules.order.repository.OrderRepository;
import com.fashion.backend.modules.product.entity.ProductVariant;
import com.fashion.backend.modules.product.repository.ProductVariantRepository;
import com.fashion.backend.modules.user.entity.User;
import com.fashion.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Override
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Giỏ hàng của bạn đang trống"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng của bạn đang trống. Vui lòng chọn sản phẩm trước khi thanh toán.");
        }

        // 1. Kiểm tra và trừ tồn kho từng sản phẩm biến thể (Atomic validation)
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<CartItem> itemsToProcess = new ArrayList<>(cart.getItems());

        for (CartItem item : itemsToProcess) {
            ProductVariant variant = item.getProductVariant();
            if (variant.getStockQuantity() < item.getQuantity()) {
                throw new BadRequestException("Tác phẩm '" + variant.getProduct().getName() +
                        "' (Size: " + variant.getSize().getName() + ", Màu: " + variant.getColor().getName() +
                        ") không đủ tồn kho (chỉ còn: " + variant.getStockQuantity() + ")");
            }

            // Trừ tồn kho nguyên tử
            variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
            productVariantRepository.save(variant);

            BigDecimal additionalPrice = variant.getAdditionalPrice() != null ? variant.getAdditionalPrice() : BigDecimal.ZERO;
            BigDecimal unitPrice = variant.getProduct().getPrice().add(additionalPrice);
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(subtotal);
        }

        // 2. Tạo Đơn hàng
        Order order = Order.builder()
                .user(user)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.COD)
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .shippingAddress(request.getShippingAddress())
                .build();

        Order savedOrder = orderRepository.save(order);

        // 3. Snapshot dữ liệu vào từng OrderItem
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem item : itemsToProcess) {
            ProductVariant variant = item.getProductVariant();
            BigDecimal additionalPrice = variant.getAdditionalPrice() != null ? variant.getAdditionalPrice() : BigDecimal.ZERO;
            BigDecimal unitPrice = variant.getProduct().getPrice().add(additionalPrice);
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .productVariant(variant)
                    .productName(variant.getProduct().getName())
                    .sizeName(variant.getSize().getName())
                    .colorName(variant.getColor().getName())
                    .unitPrice(unitPrice)
                    .quantity(item.getQuantity())
                    .subtotal(subtotal)
                    .build();

            orderItems.add(orderItem);
        }
        orderItemRepository.saveAll(orderItems);
        savedOrder.setItems(orderItems);

        // 4. Xóa sạch giỏ hàng sau khi đặt hàng thành công
        cart.getItems().clear();
        cartItemRepository.deleteByCartId(cart.getId());

        log.info("Đặt hàng thành công: Mã đơn #BESPOKE-{}, Người nhận: {}, Tổng tiền: {}",
                savedOrder.getId(), savedOrder.getReceiverName(), savedOrder.getTotalAmount());

        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long userId, Long orderId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        if (!isAdmin && !order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Đơn hàng này không thuộc quyền sở hữu của bạn");
        }

        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        order.setStatus(status);
        Order updated = orderRepository.save(order);
        log.info("Cập nhật trạng thái đơn hàng #{}: {}", orderId, status);
        return mapToOrderResponse(updated);
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem oi : order.getItems()) {
                String imageUrl = null;
                if (oi.getProductVariant() != null) {
                    imageUrl = oi.getProductVariant().getImageUrl() != null && !oi.getProductVariant().getImageUrl().isBlank()
                            ? oi.getProductVariant().getImageUrl()
                            : oi.getProductVariant().getProduct().getImageUrl();
                }

                itemResponses.add(OrderItemResponse.builder()
                        .id(oi.getId())
                        .productVariantId(oi.getProductVariant() != null ? oi.getProductVariant().getId() : null)
                        .productName(oi.getProductName())
                        .sizeName(oi.getSizeName())
                        .colorName(oi.getColorName())
                        .unitPrice(oi.getUnitPrice())
                        .quantity(oi.getQuantity())
                        .subtotal(oi.getSubtotal())
                        .imageUrl(imageUrl)
                        .build());
            }
        }

        String orderCode = String.format("#BESPOKE-%06d", order.getId());

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(orderCode)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}
