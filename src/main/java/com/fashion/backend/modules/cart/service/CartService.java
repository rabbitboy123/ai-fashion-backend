package com.fashion.backend.modules.cart.service;

import com.fashion.backend.modules.cart.dto.request.AddToCartRequest;
import com.fashion.backend.modules.cart.dto.request.UpdateCartItemRequest;
import com.fashion.backend.modules.cart.dto.response.CartResponse;

public interface CartService {
    CartResponse getCartByUserId(Long userId);
    CartResponse addToCart(Long userId, AddToCartRequest request);
    CartResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request);
    CartResponse removeCartItem(Long userId, Long cartItemId);
    CartResponse clearCart(Long userId);
}
