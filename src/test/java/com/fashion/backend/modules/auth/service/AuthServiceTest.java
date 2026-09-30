package com.fashion.backend.modules.auth.service;

import com.fashion.backend.common.constant.RoleType;
import com.fashion.backend.common.exception.BadRequestException;
import com.fashion.backend.modules.auth.dto.AuthResponse;
import com.fashion.backend.modules.auth.dto.LoginRequest;
import com.fashion.backend.modules.auth.dto.RegisterRequest;
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
    private AuthService authService;

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
        RegisterRequest request = RegisterRequest.builder()
                .email("test.client@aifashion.com")
                .password("password123")
                .fullName("Test Client")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(cartRepository.save(any(Cart.class))).thenReturn(new Cart());
        when(tokenProvider.generateToken(any())).thenReturn("mocked.jwt.token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getAccessToken());
        assertEquals("test.client@aifashion.com", response.getEmail());
        assertEquals("ROLE_CUSTOMER", response.getRole());

        verify(cartRepository, times(1)).save(any(Cart.class));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Đăng ký thất bại: Email đã tồn tại phải ném ra BadRequestException")
    void register_DuplicateEmail_ThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test.client@aifashion.com")
                .password("password123")
                .fullName("Test Client")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

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
        RegisterRequest request = RegisterRequest.builder()
                .email("  Test.Client@AIFashion.COM  ")
                .password("password123")
                .fullName("Test Client")
                .build();

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
        LoginRequest request = LoginRequest.builder()
                .email("test.client@aifashion.com")
                .password("password123")
                .build();

        UserPrincipal principal = UserPrincipal.create(sampleUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("mocked.login.jwt");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked.login.jwt", response.getAccessToken());
        assertEquals("test.client@aifashion.com", response.getEmail());
    }

    @Test
    @DisplayName("Đăng nhập thất bại: Sai mật khẩu phải ném ra BadRequestException")
    void login_BadCredentials_ThrowsException() {
        LoginRequest request = LoginRequest.builder()
                .email("test.client@aifashion.com")
                .password("wrongpassword")
                .build();

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.login(request);
        });

        assertEquals("Email hoặc mật khẩu không chính xác.", exception.getMessage());
    }
}
