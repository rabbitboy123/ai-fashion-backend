package com.fashion.backend.modules.auth.service;

import com.fashion.backend.common.constant.RoleType;
import com.fashion.backend.common.exception.BadRequestException;
import com.fashion.backend.modules.auth.dto.response.AuthResponse;
import com.fashion.backend.modules.auth.dto.request.LoginRequest;
import com.fashion.backend.modules.auth.dto.request.RegisterRequest;
import com.fashion.backend.modules.cart.entity.Cart;
import com.fashion.backend.modules.cart.repository.CartRepository;
import com.fashion.backend.modules.user.entity.User;
import com.fashion.backend.modules.user.repository.UserRepository;
import com.fashion.backend.security.JwtTokenProvider;
import com.fashion.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("test.client@aifashion.com")
                .password("encoded_pass")
                .fullName("Test Client")
                .role(RoleType.ROLE_CUSTOMER)
                .build();
    }

    @Test
    @DisplayName("Đăng ký thành công: mã hóa mật khẩu, lưu user, khởi tạo cart và trả về JWT")
    void register_Success() {
        RegisterRequest request = new RegisterRequest("test.client@aifashion.com", "password123", "Test Client", null, null);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(cartRepository.save(any(Cart.class))).thenReturn(new Cart());
        when(tokenProvider.generateToken(any())).thenReturn("mocked.jwt.token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.accessToken());
        assertEquals("test.client@aifashion.com", response.email());
        assertEquals("ROLE_CUSTOMER", response.role());

        verify(cartRepository, times(1)).save(any(Cart.class));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Đăng ký thất bại: Email đã tồn tại phải ném ra BadRequestException")
    void register_DuplicateEmail_ThrowsException() {
        RegisterRequest request = new RegisterRequest("test.client@aifashion.com", "password123", "Test Client", null, null);

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.register(request);
        });

        assertTrue(exception.getMessage().contains("đã được sử dụng"));
        verify(userRepository, never()).save(any(User.class));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("Đăng ký thất bại: Email viết hoa hoặc có khoảng trắng trùng lặp phải được chuẩn hóa và ném ra BadRequestException")
    void register_DuplicateEmail_MixedCaseAndWhitespace_ThrowsException() {
        RegisterRequest request = new RegisterRequest("  Test.Client@AIFashion.COM  ", "password123", "Test Client", null, null);

        when(userRepository.existsByEmail("test.client@aifashion.com")).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.register(request);
        });

        assertTrue(exception.getMessage().contains("đã được sử dụng"));
        verify(userRepository, never()).save(any(User.class));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("Đăng nhập thành công: Xác thực credentials và sinh JWT token")
    void login_Success() {
        LoginRequest request = new LoginRequest("test.client@aifashion.com", "password123");

        UserPrincipal principal = UserPrincipal.create(sampleUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("mocked.login.jwt");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked.login.jwt", response.accessToken());
        assertEquals("test.client@aifashion.com", response.email());
    }

    @Test
    @DisplayName("Đăng nhập thất bại: Sai mật khẩu phải ném ra BadRequestException")
    void login_BadCredentials_ThrowsException() {
        LoginRequest request = new LoginRequest("test.client@aifashion.com", "wrongpassword");

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.login(request);
        });

        assertEquals("Email hoặc mật khẩu không chính xác.", exception.getMessage());
    }
}
