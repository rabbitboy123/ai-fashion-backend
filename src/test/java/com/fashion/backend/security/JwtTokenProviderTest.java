package com.fashion.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", "9a618248b64db62d15b300a07b00580bbfddf4644075103c330e797b5d1e4e58");
        ReflectionTestUtils.setField(tokenProvider, "jwtExpiration", 86400000L);
    }

    @Test
    @DisplayName("Sinh và giải mã token hợp lệ")
    void generateAndValidateToken_Success() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(99L)
                .email("vip.member@aifashion.com")
                .fullName("Bespoke Client")
                .password("secret")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        String token = tokenProvider.generateToken(auth);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        boolean isValid = tokenProvider.validateToken(token);
        assertTrue(isValid);

        String email = tokenProvider.getEmailFromToken(token);
        assertEquals("vip.member@aifashion.com", email);
    }

    @Test
    @DisplayName("Xác thực token giả mạo hoặc sai định dạng phải trả về false")
    void validateToken_MalformedToken_ReturnsFalse() {
        boolean isValid = tokenProvider.validateToken("invalid.bearer.token");
        assertFalse(isValid);
    }
}
