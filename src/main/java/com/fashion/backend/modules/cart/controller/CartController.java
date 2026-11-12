package com.fashion.backend.modules.cart.controller;

import com.fashion.backend.common.api.ApiResponse;
import com.fashion.backend.modules.cart.dto.request.AddToCartRequest;
import com.fashion.backend.modules.cart.dto.request.UpdateCartItemRequest;
import com.fashion.backend.modules.cart.dto.response.CartResponse;
import com.fashion.backend.modules.cart.service.CartService;
import com.fashion.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Validated
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        CartResponse response = cartService.getCartByUserId(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy giỏ hàng thành công", response));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody AddToCartRequest request) {
        CartResponse response = cartService.addToCart(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Thêm sản phẩm vào giỏ hàng thành công", response));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateCartItemRequest request) {
        CartResponse response = cartService.updateCartItem(userPrincipal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật giỏ hàng thành công", response));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable @Positive Long id) {
        CartResponse response = cartService.removeCartItem(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm khỏi giỏ hàng thành công", response));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        CartResponse response = cartService.clearCart(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Làm trống giỏ hàng thành công", response));
    }
}
