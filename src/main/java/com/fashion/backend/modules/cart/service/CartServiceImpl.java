package com.fashion.backend.modules.cart.service;

import com.fashion.backend.common.exception.BadRequestException;
import com.fashion.backend.common.exception.ResourceNotFoundException;
import com.fashion.backend.modules.cart.dto.request.AddToCartRequest;
import com.fashion.backend.modules.cart.dto.request.UpdateCartItemRequest;
import com.fashion.backend.modules.cart.dto.response.CartItemResponse;
import com.fashion.backend.modules.cart.dto.response.CartResponse;
import com.fashion.backend.modules.cart.entity.Cart;
import com.fashion.backend.modules.cart.entity.CartItem;
import com.fashion.backend.modules.cart.repository.CartItemRepository;
import com.fashion.backend.modules.cart.repository.CartRepository;
import com.fashion.backend.modules.product.entity.Product;
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
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCartByUserId(Long userId) {
        Cart cart = getOrCreateCartEntity(userId);
        return mapToCartResponse(cart);
    }

    @Override
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        Cart cart = getOrCreateCartEntity(userId);

        ProductVariant variant = productVariantRepository.findById(request.getProductVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể sản phẩm với ID: " + request.getProductVariantId()));

        int requestedQty = request.getQuantity() != null && request.getQuantity() > 0 ? request.getQuantity() : 1;

        if (variant.getStockQuantity() < requestedQty) {
            throw new BadRequestException("Số lượng tồn kho không đủ (chỉ còn: " + variant.getStockQuantity() + ")");
        }

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), variant.getId());

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + requestedQty;

            if (newQuantity > variant.getStockQuantity()) {
                throw new BadRequestException("Tổng số lượng trong giỏ hàng (" + newQuantity + ") vượt quá số lượng tồn kho (" + variant.getStockQuantity() + ")");
            }

            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productVariant(variant)
                    .quantity(requestedQty)
                    .build();
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        return mapToCartResponse(cart);
    }

    @Override
    public CartResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCartEntity(userId);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mục giỏ hàng với ID: " + cartItemId));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Mục giỏ hàng này không thuộc quyền sở hữu của bạn");
        }

        int newQuantity = request.getQuantity() != null ? request.getQuantity() : 0;

        if (newQuantity <= 0) {
            cart.getItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
        } else {
            ProductVariant variant = cartItem.getProductVariant();
            if (newQuantity > variant.getStockQuantity()) {
                throw new BadRequestException("Số lượng cập nhật (" + newQuantity + ") vượt quá số lượng tồn kho (" + variant.getStockQuantity() + ")");
            }
            cartItem.setQuantity(newQuantity);
            cartItemRepository.save(cartItem);
        }

        return mapToCartResponse(cart);
    }

    @Override
    public CartResponse removeCartItem(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCartEntity(userId);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mục giỏ hàng với ID: " + cartItemId));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Mục giỏ hàng này không thuộc quyền sở hữu của bạn");
        }

        cart.getItems().remove(cartItem);
        cartItemRepository.delete(cartItem);

        return mapToCartResponse(cart);
    }

    @Override
    public CartResponse clearCart(Long userId) {
        Cart cart = getOrCreateCartEntity(userId);
        cart.getItems().clear();
        cartItemRepository.deleteByCartId(cart.getId());
        return mapToCartResponse(cart);
    }

    private Cart getOrCreateCartEntity(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));
            Cart newCart = Cart.builder().user(user).build();
            return cartRepository.save(newCart);
        });
    }

    private CartResponse mapToCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            ProductVariant variant = item.getProductVariant();
            Product product = variant.getProduct();

            BigDecimal additionalPrice = variant.getAdditionalPrice() != null ? variant.getAdditionalPrice() : BigDecimal.ZERO;
            BigDecimal unitPrice = product.getPrice().add(additionalPrice);
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            String imageUrl = (variant.getImageUrl() != null && !variant.getImageUrl().isBlank())
                    ? variant.getImageUrl()
                    : product.getImageUrl();

            CartItemResponse itemResp = CartItemResponse.builder()
                    .id(item.getId())
                    .productVariantId(variant.getId())
                    .productId(product.getId())
                    .productName(product.getName())
                    .sizeName(variant.getSize().getName())
                    .colorName(variant.getColor().getName())
                    .colorHex(variant.getColor().getHexCode())
                    .unitPrice(unitPrice)
                    .quantity(item.getQuantity())
                    .subtotal(subtotal)
                    .imageUrl(imageUrl)
                    .stockQuantity(variant.getStockQuantity())
                    .build();

            itemResponses.add(itemResp);
            totalQuantity += item.getQuantity();
            totalAmount = totalAmount.add(subtotal);
        }

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .build();
    }
}
