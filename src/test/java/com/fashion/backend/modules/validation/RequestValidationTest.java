package com.fashion.backend.modules.validation;

import com.fashion.backend.modules.auth.dto.request.RegisterRequest;
import com.fashion.backend.modules.cart.dto.request.UpdateCartItemRequest;
import com.fashion.backend.modules.order.dto.request.CreateOrderRequest;
import com.fashion.backend.modules.product.dto.request.ProductFilterRequest;
import com.fashion.backend.modules.product.dto.request.AdminCreateVariantRequest;
import com.fashion.backend.modules.cart.dto.request.AddToCartRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RequestValidationTest {

    private static Validator validator;
    private static AutoCloseable validatorFactory;

    @BeforeAll
    static void setUp() {
        var factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        validatorFactory = factory::close;
    }

    @AfterAll
    static void tearDown() throws Exception {
        validatorFactory.close();
    }

    @Test
    void registerRejectsMalformedEmailAndFutureBirthDate() {
        RegisterRequest request = new RegisterRequest("bad-email", "secret1", "User", null,
                LocalDate.now().plusDays(1));
        var violations = validator.validate(request);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("dateOfBirth")));
    }

    @Test
    void cartRejectsZeroQuantity() {
        assertFalse(validator.validate(new UpdateCartItemRequest(0)).isEmpty());
    }

    @Test
    void checkoutRejectsInvalidPhone() {
        CreateOrderRequest request = new CreateOrderRequest("User", "abc", "Address", null);
        assertTrue(validator.validate(request).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("receiverPhone")));
    }

    @Test
    void optionalDefaultsRemainOptional() {
        CreateOrderRequest order = new CreateOrderRequest("User", "0912345678", "Address", null);
        AddToCartRequest cart = new AddToCartRequest(1L, null);
        AdminCreateVariantRequest variant = new AdminCreateVariantRequest();

        assertTrue(validator.validate(order).isEmpty());
        assertTrue(validator.validate(cart).isEmpty());
        assertTrue(validator.validate(variant).isEmpty());
    }

    @Test
    void productFiltersRejectInvertedPriceRange() {
        ProductFilterRequest request = ProductFilterRequest.builder()
                .minPrice(new BigDecimal("200"))
                .maxPrice(new BigDecimal("100"))
                .page(0).size(12).sortBy("newest").build();
        assertTrue(validator.validate(request).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("priceRangeValid")));
    }
}
